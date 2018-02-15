/***************************************************************************
 * Copyright (c) Intellinium SAS, 2014-present
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Intellinium SAS and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Intellinium SAS
 * and its suppliers and may be covered by French and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Intellinium SAS.
 ***************************************************************************/
/* TODO: add runtime copyright */

package io.runtime.mcumgr.dfu;

import android.app.Activity;
import android.util.Log;

import java.util.Date;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrInitCallback;
import io.runtime.mcumgr.McuMgrMtuCallback;
import io.runtime.mcumgr.McuMgrMtuProvider;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.mgrs.DefaultManager;
import io.runtime.mcumgr.mgrs.ImageManager;
import io.runtime.mcumgr.resp.McuMgrImageStateResponse;
import io.runtime.mcumgr.resp.McuMgrSimpleResponse;

// TODO Add retries for each step

public class FirmwareUpgradeManager
		implements ImageManager.ImageUploadCallback, McuMgrInitCallback, McuMgrMtuCallback {

	private final static String TAG = "FirmwareUpgradeManager";
	private final McuMgrTransport mTransporter;

	private ImageManager mImageManager;
	private DefaultManager mDefaultManager;
	private FirmwareUpgradeCallback mCallback;
	private byte[] mImageData;
	private byte[] mHash;

	private State mState;
	private boolean mPaused = false;

	/**
	 * If set, we must call all the callbacks in the main thread
	 */
	private Activity mActivity;
	private McuMgrInitCallback mInitCb;

	public FirmwareUpgradeManager(McuMgrTransport transport, byte[] imageData,
								  FirmwareUpgradeCallback callback) throws McuMgrException {
		mImageData = imageData;
		mCallback = callback;
		mState = State.NONE;
		mTransporter = transport;
		mImageManager = new ImageManager(mTransporter, imageData);
		mDefaultManager = new DefaultManager(mTransporter);
		mHash = ImageManager.getHashFromImage(imageData);
	}

	/**
	 * Asks all the callback function to be called in the UI thread
	 *
	 * @param activity The activity running the UI thread
	 */
	public void setCallbackOnUiThread(Activity activity) {
		this.mActivity = activity;
	}

	public synchronized void init(McuMgrInitCallback cb) {
		mInitCb = cb;
		mTransporter.init(this);
	}

	@Override
	public void onInitSuccess() {
		/* We have to check if the transporter is an {@link McuMgrMtuProvider}. If so, ask for the Mtu, as
		 * it is part of the transporter initialization */
		if (mTransporter instanceof McuMgrMtuProvider) {
			((McuMgrMtuProvider) mTransporter).getMtu(this);
		} else {
			mInitCb.onInitSuccess();
		}
	}

	@Override
	public void onInitError() {
		mInitCb.onInitError();
	}

	@Override
	public void onMtuFetched(int mtu) {
		this.mImageManager.setUploadMtu(mtu);
		mInitCb.onInitSuccess();
	}

	@Override
	public void onMtuError() {
		mInitCb.onInitError();
	}

	public synchronized void start() {
		if (mState != State.NONE) {
			Log.i(TAG, "Firmware upgrade is already in progress");
			return;
		}
		// Begin the upload
		mState = State.UPLOAD;
		mImageManager.upload(mImageData, this);
	}

	public synchronized void cancel() {
		cancelPrivate();
		if (mActivity != null) {
			mActivity.runOnUiThread(() -> mCallback.onCancel(mState));
		} else {
			mCallback.onCancel(mState);
		}
	}

	private synchronized void cancelPrivate() {
		mState = State.NONE;
		mPaused = false;
		if (mResetPollThread != null) {
			mResetPollThread.interrupt();
		}
	}

	private synchronized void fail(McuMgrException error) {
		if (mActivity != null) {
			mActivity.runOnUiThread(() -> mCallback.onFail(mState, error));
		} else {
			mCallback.onFail(mState, error);
		}
		cancelPrivate();
	}

	public synchronized void pause() {
		if (mState.isInProgress()) {
			Log.d(TAG, "Pausing upgrade.");
			mPaused = true;
			if (mState == State.UPLOAD) {
				mImageManager.pauseUpload();
			}
		}
	}

	public synchronized void resume() {
		if (mPaused) {
			mPaused = false;
			currentState();
		}
	}

	public boolean isPaused() {
		return mPaused;
	}

	public State getState() {
		return mState;
	}

	public boolean isInProgress() {
		return mState.isInProgress();
	}

	private synchronized void currentState() {
		if (mPaused) {
			return;
		}
		switch (mState) {
			case NONE:
				return;
			case UPLOAD:
				mImageManager.continueUpload();
				break;
			case TEST:
				mImageManager.test(mHash, mTestCallback);
				break;
			case RESET:
				nextState();
				break;
			case CONFIRM:
				mImageManager.confirm(mHash, mConfirmCallback);
				break;
		}
	}

	private synchronized void nextState() {
		if (mPaused) {
			return;
		}

		State prevState = mState;
		switch (mState) {
			case NONE:
				return;
			case UPLOAD:
				mState = State.TEST;
				mImageManager.test(mHash, mTestCallback);
				break;
			case TEST:
				mState = State.RESET;
				mDefaultManager.reset(mResetCallback);
				break;
			case RESET:
				mState = State.CONFIRM;
				mImageManager.confirm(mHash, mConfirmCallback);
				break;
			case CONFIRM:
				mState = State.SUCCESS;
				break;
		}
		Log.d(TAG, "Moving from state " + prevState.name() + " to state " + mState.name());

		if (mActivity != null) {
			mActivity.runOnUiThread(() -> mCallback.onStateChanged(prevState, mState));
		} else {
			mCallback.onStateChanged(prevState, mState);
		}

		if (mState == State.SUCCESS) {
			if (mActivity != null) {
				mActivity.runOnUiThread(() -> mCallback.onSuccess());
			} else {
				mCallback.onSuccess();
			}
		}
	}

	//******************************************************************
	// CoAP Handlers
	//******************************************************************

	private McuMgrCallback<McuMgrImageStateResponse> mTestCallback = new McuMgrCallback<McuMgrImageStateResponse>() {
		@Override
		public void onResponse(McuMgrImageStateResponse response) {
			if (!response.isSuccess()) {
				fail(new McuMgrException("Test command failed!"));
				return;
			}

			Log.v(TAG, "Test response: " + response.toString());
			if (response.images.length != 2) {
				fail(new McuMgrException("Test response does not contain enough info"));
				return;
			}
			if (!response.images[1].pending) {
				Log.e(TAG, "Tested image is not in a pending state.");
				fail(new McuMgrException("Tested image is not in a pending state."));
				return;
			}
			// Test image success, begin device reset
			nextState();
		}

		@Override
		public void onError(McuMgrException e) {

			fail(e);
		}
	};

	private McuMgrCallback<McuMgrSimpleResponse> mResetCallback = new McuMgrCallback<McuMgrSimpleResponse>() {
		@Override
		public void onResponse(McuMgrSimpleResponse response) {
			if (!response.isSuccess()) {
				fail(new McuMgrException("Confirm command failed!"));
				return;
			}
			if (response.getRcCode() != McuMgrErrorCode.OK) {
				Log.e(TAG, "Reset failed due to Newt Manager error: " + response.getRcCode());
				fail(new McuMgrErrorException(response.getRcCode()));
				return;
			}
			// Begin polling the device to determine the reset
			mResetPollThread.start();
		}

		@Override
		public void onError(McuMgrException e) {
			fail(e);
		}
	};

	private McuMgrCallback<McuMgrImageStateResponse> mConfirmCallback =
			new McuMgrCallback<McuMgrImageStateResponse>() {
		@Override
		public void onResponse(McuMgrImageStateResponse response) {
			if (!response.isSuccess()) {
				fail(new McuMgrException("Confirm failed!"));
				return;
			}
			if (response.getRcCode() != McuMgrErrorCode.OK) {
				Log.e(TAG, "Confirm failed due to Newt Manager error: " + response.getRcCode());
				fail(new McuMgrErrorException(response.getRcCode()));
				return;
			}
			if (response.images.length == 0) {
				fail(new McuMgrException("Test response does not contain enough info"));
				return;
			}
			if (!response.images[0].confirmed) {
				Log.e(TAG, "Image is not in a confirmed state.");
				fail(new McuMgrException("Image is not in a confirmed state."));
				return;
			}
			// Confirm image success
			nextState();
		}

		@Override
		public void onError(McuMgrException e) {
			fail(e);
		}
	};

	//******************************************************************
	// Firmware Upgrade State
	//******************************************************************

	public enum State {
		NONE, UPLOAD, TEST, RESET, CONFIRM, SUCCESS;

		public boolean isInProgress() {
			return this == UPLOAD || this == TEST || this == RESET || this == CONFIRM;
		}
	}

	//******************************************************************
	// Poll Reset Runnable
	//******************************************************************

	private Thread mResetPollThread = new Thread(new Runnable() {
		@Override
		public void run() {
			int attempts = 0;
			try {
				// Wait for the device to reset before polling. The BLE disconnect callback will
				// take 20 seconds to trigger. TODO need to figure out a better way for UDP
				synchronized (this) {
					wait(21000);
				}
				while (true) {
					Log.d(TAG, "Calling image list...");
					mImageManager.list(new McuMgrCallback<McuMgrImageStateResponse>() {
						@Override
						public synchronized void onResponse(McuMgrImageStateResponse response) {
							if (mState == State.RESET) {
								// Device has reset, begin confirm
								nextState();
								// Interrupt the thread
								mResetPollThread.interrupt();
							}
						}

						@Override
						public void onError(McuMgrException e) {
							// Do nothing...
						}
					});
					if (attempts == 4) {
						fail(new McuMgrException("Reset poller has reached attempt limit."));
						return;
					}
					attempts++;
					synchronized (this) {
						wait(5000);
					}
				}
			} catch (InterruptedException e) {
				// Do nothing...
			}
		}
	});

	//******************************************************************
	// Image Upload Callbacks
	//******************************************************************

	@Override
	public void onProgressChange(int bytesSent, int imageSize, Date ts) {
		if (mActivity != null) {
			mActivity.runOnUiThread(() -> mCallback.onUploadProgressChanged(bytesSent, imageSize, ts));
		} else {
			mCallback.onUploadProgressChanged(bytesSent, imageSize, ts);
		}
	}

	@Override
	public void onUploadFail(McuMgrException error) {
		if (mActivity != null) {
			mActivity.runOnUiThread(() -> mCallback.onFail(mState, error));
		} else {
			mCallback.onFail(mState, error);
		}
	}

	@Override
	public void onUploadFinish() {
		// Upload finished, move to next state
		nextState();
	}
}
