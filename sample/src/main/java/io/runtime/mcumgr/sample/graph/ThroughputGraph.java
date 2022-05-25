package io.runtime.mcumgr.sample.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import io.runtime.mcumgr.sample.R;

public class ThroughputGraph extends View {
	private static final float DEFAULT_MAX_THROUGHPUT = 40.0f;

	private final Paint instantaneousThroughputLinePaint;
	private final Paint averageThroughputLinePaint;
	private final Paint metadataPaint;
	private float tenKbPerSHeight;
	/** View dimension. */
	private int width, height;

	private final float[] instantaneousThroughputData = new float[100];
	private final float[] averageThroughputData = new float[100];
	private final int[] progressData = new int[100];
	private float currentMaxThroughput, maxThroughput;
	private int currentPercent;
	private final Path instantaneousThroughputPath = new Path();
	private final float[] averageThroughputPoints = new float[4 * 100];

	public ThroughputGraph(final Context context, @Nullable final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ThroughputGraph(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public ThroughputGraph(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		currentMaxThroughput = maxThroughput = DEFAULT_MAX_THROUGHPUT; // kB/s

		metadataPaint = new Paint();
		metadataPaint.setStrokeWidth(2);
		instantaneousThroughputLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		averageThroughputLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		averageThroughputLinePaint.setStrokeWidth(5);
		averageThroughputLinePaint.setStrokeJoin(Paint.Join.ROUND);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			instantaneousThroughputLinePaint.setColor(getResources().getColor(R.color.colorInstantaneousThroughput, context.getTheme()));
			averageThroughputLinePaint.setColor(getResources().getColor(R.color.colorAverageThroughput, context.getTheme()));
			metadataPaint.setColor(getResources().getColor(R.color.colorGraphGrid, context.getTheme()));
		} else {
			instantaneousThroughputLinePaint.setColor(getResources().getColor(R.color.colorInstantaneousThroughput));
			averageThroughputLinePaint.setColor(getResources().getColor(R.color.colorAverageThroughput));
			metadataPaint.setColor(getResources().getColor(R.color.colorGraphGrid));
		}
	}

	// State restoration ---------------------------------------------------------------------------

	@Override
	protected void onRestoreInstanceState(final Parcelable state) {
		final SavedState ss = (SavedState) state;

		super.onRestoreInstanceState(ss.getSuperState());

		currentMaxThroughput = ss.maxThroughput;
		currentPercent = ss.currentPercent;
		System.arraycopy(ss.instantaneousThroughputData, 0, instantaneousThroughputData, 0, instantaneousThroughputData.length);
		System.arraycopy(ss.averageThroughputData, 0, averageThroughputData, 0, averageThroughputData.length);
		System.arraycopy(ss.progressData, 0, progressData, 0, progressData.length);
	}

	@Nullable
	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		final SavedState state = new SavedState(superState);
		state.maxThroughput = currentMaxThroughput;
		state.instantaneousThroughputData = instantaneousThroughputData;
		state.averageThroughputData = averageThroughputData;
		state.progressData = progressData;
		state.currentPercent = currentPercent;
		return state;
	}

	// Drawing -------------------------------------------------------------------------------------

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);

		if (currentPercent > 0) {
			canvas.drawPath(instantaneousThroughputPath, instantaneousThroughputLinePaint);
			canvas.drawLines(averageThroughputPoints, 0, currentPercent << 2, averageThroughputLinePaint);
			for (float h = height; h > 0; h -= tenKbPerSHeight) {
				canvas.drawLine(0, h, width, h, metadataPaint);
			}
		}
	}

	@Override
	protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		width = w;
		height = h;
		recalculate();
		recalculateMetadata();
	}

	// Public API ----------------------------------------------------------------------------------

	/**
	 * Sets the max throughput indicator at the given level.
	 * @param maxThroughput the value considered as high. A line will be drown there.
	 */
	public void setMaxThroughput(final float maxThroughput) {
		this.maxThroughput = Math.max(0, maxThroughput);
		if (maxThroughput > currentMaxThroughput)
			currentMaxThroughput = maxThroughput;
		recalculate();
		recalculateMetadata();
	}

	/**
	 * Adds a new throughput point to the graph.
	 *
	 * @param progress The current upload percentage for the measured throughout, from 0 to 100.
	 * @param instantaneousThroughput The instantaneous throughput in kB/s.
	 * @param averageThroughput The average throughput in kB/s.
	 */
	public void addProgress(final int progress, final float instantaneousThroughput, final float averageThroughput) {
		if (currentPercent < averageThroughputData.length && progressData[currentPercent] != progress) {
			instantaneousThroughputData[currentPercent] = instantaneousThroughput;
			averageThroughputData[currentPercent] = averageThroughput;
			progressData[currentPercent] = progress;
			currentPercent += 1;

			if (currentMaxThroughput < instantaneousThroughput) {
				currentMaxThroughput = instantaneousThroughput;
				recalculateMetadata();
			}
		}
		recalculate();
	}

	/**
	 * Clears the graph.
	 */
	public void clear() {
		currentPercent = 0;
		currentMaxThroughput = maxThroughput;
		instantaneousThroughputPath.reset();
		invalidate();
	}

	// Helper methods ------------------------------------------------------------------------------

	private void recalculate() {
		float previousX = 0, previousY = height;
		float lastY_i = height;

		instantaneousThroughputPath.rewind();
		for (int i = 0; i < currentPercent; ++i) {
			final float x = (float) width * progressData[i] / 100.0f;
			final float y_i = height - height * instantaneousThroughputData[i] / currentMaxThroughput;
			final float y_a = height - height * averageThroughputData[i] / currentMaxThroughput;
			if (i == 0) {
				final float initialX = x - (float) width / 100.0f;
				instantaneousThroughputPath.moveTo(initialX, height);

				averageThroughputPoints[0] = initialX;
				averageThroughputPoints[1] = height;
				averageThroughputPoints[2] = x;
				averageThroughputPoints[3] = y_a;
			} else {
				averageThroughputPoints[4 * i] = previousX;
				averageThroughputPoints[4 * i + 1] = previousY;
				averageThroughputPoints[4 * i + 2] = x;
				averageThroughputPoints[4 * i + 3] = y_a;
			}
			previousX = x;
			previousY = y_a;
			instantaneousThroughputPath.lineTo(x, y_i);
			lastY_i = y_i;
		}
		instantaneousThroughputPath.rLineTo(0, height - lastY_i);
		instantaneousThroughputPath.close();

		invalidate();
	}

	private void recalculateMetadata() {
		// Recalculate the indicator height.
		tenKbPerSHeight = height * 10.0f / currentMaxThroughput;
		invalidate();
	}

	// Saved State ---------------------------------------------------------------------------------

	static class SavedState extends BaseSavedState {
		private float maxThroughput;
		private float[] instantaneousThroughputData;
		private float[] averageThroughputData;
		private int[] progressData;
		private int currentPercent;

		/**
		 * Constructor called from {@link ThroughputGraph#onSaveInstanceState()}
		 */
		SavedState(Parcelable superState) {
			super(superState);
		}

		SavedState(Parcel in) {
			super(in);
			maxThroughput = in.readFloat();
			instantaneousThroughputData = in.createFloatArray();
			averageThroughputData = in.createFloatArray();
			progressData = in.createIntArray();
			currentPercent = in.readInt();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeFloat(maxThroughput);
			dest.writeFloatArray(instantaneousThroughputData);
			dest.writeFloatArray(averageThroughputData);
			dest.writeIntArray(progressData);
			dest.writeInt(currentPercent);
		}

		public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
