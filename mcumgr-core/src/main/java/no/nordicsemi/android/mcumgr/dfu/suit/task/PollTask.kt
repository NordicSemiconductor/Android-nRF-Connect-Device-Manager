package no.nordicsemi.android.mcumgr.dfu.suit.task

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nordicsemi.android.mcumgr.McuMgrTransport
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradeManager
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradeManager.OnResourceRequiredCallback
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradeManager.ResourceCallback
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradePerformer
import no.nordicsemi.android.mcumgr.exception.McuMgrException
import no.nordicsemi.android.mcumgr.exception.McuMgrTimeoutException
import no.nordicsemi.android.mcumgr.managers.SUITManager
import no.nordicsemi.android.mcumgr.task.TaskManager
import no.nordicsemi.android.mcumgr.util.ByteUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class PollTask(
    private val limit: Int = 5,
    private val interval: Long = 1000L
): SUITUpgradeTask() {
    private val LOG: Logger = LoggerFactory.getLogger(PollTask::class.java)

    private var isCancelled = false
    private var isComplete = false
    private var resourceCallback: OnResourceRequiredCallback? = null

    override fun getPriority(): Int = PRIORITY_PROCESS

    override fun getState(): SUITUpgradeManager.State = SUITUpgradeManager.State.PROCESSING

    @OptIn(DelicateCoroutinesApi::class)
    override fun start(
        performer: TaskManager<SUITUpgradePerformer.Settings, SUITUpgradeManager.State>
    ) {
        val manager = SUITManager(performer.transport)
        val task = this

        // Monitor connection state. A disconnection while polling means the process is complete.
        val observer = object : McuMgrTransport.ConnectionObserver {
            override fun onConnected() {}

            override fun onDisconnected() {
                LOG.info("Disconnected, upload complete")
                if (!isComplete) {
                    isComplete = true
                    manager.transporter.removeObserver(this)
                    performer.onTaskCompleted(task)
                }
            }
        }
        manager.transporter.addObserver(observer)

        // Let's count retries.
        var count = 0

        GlobalScope.launch {
            while (!isCancelled && !isComplete) {
                count += 1
                try {
                    // Wait initial interval
                    delay(interval)
                    if (isComplete || isCancelled) {
                        break
                    }
                    LOG.trace("Polling for resources ({}/{})...", count, limit)
                    val response = manager.poll()
                    if (response.isRequestingResource) {
                        manager.transporter.removeObserver(observer)

                        val uri = response.resourceUri ?: run {
                            performer.onTaskFailed(task, McuMgrException("Resource URI is invalid (0x): ${ByteUtil.byteArrayToHex(response.resourceId)}"))
                            return@launch
                        }
                        LOG.info("Resource requested: {}", uri)

                        val resourceCallback = performer.settings.resourceCallback
                        if (resourceCallback != null) {
                            this@PollTask.resourceCallback = resourceCallback
                            val callback: ResourceCallback = object : ResourceCallback {

                                override fun provide(data: ByteArray) {
                                    LOG.info("Resource of size {} bytes provided", data.size)
                                    this@PollTask.resourceCallback = null
                                    performer.enqueue(UploadResource(response.sessionId, data))
                                    performer.onTaskCompleted(task)
                                }

                                override fun error(e: Exception) = when (e) {
                                    is McuMgrException -> performer.onTaskFailed(task, e)
                                    else -> performer.onTaskFailed(task, McuMgrException(e))
                                }.also {
                                    LOG.error("Resource error", e)
                                    this@PollTask.resourceCallback = null
                                }

                            }
                            resourceCallback.onResourceRequired(uri, callback)
                        } else {
                            // Hint: Use setResourceCallback in SUITUpgradeManager to provide a callback.
                            LOG.error("Resource {} is required but no callback is provided", uri)
                            performer.onTaskFailed(task, McuMgrException("Resource $uri is required but no callback is provided"))
                        }
                        return@launch
                    } else if (count < limit) {
                        LOG.trace("Retrying in {} ms", interval)
                    } else {
                        LOG.warn("No resources requested after {} attempts, also no disconnection", limit)
                        performer.onTaskFailed(task, McuMgrTimeoutException())
                        break
                    }
                } catch (e: McuMgrTimeoutException) {
                    LOG.info("Request timed out, upload complete")
                    isComplete = true
                    performer.onTaskCompleted(task)
                    break
                } catch (e: McuMgrException) {
                    LOG.error("Error polling for SUIT manifest", e)
                    performer.onTaskFailed(task, e)
                    break
                } catch (e: Exception) {
                    LOG.error("Error polling for SUIT manifest", e)
                    performer.onTaskFailed(task, McuMgrException(e))
                    break
                }
            }
            manager.transporter.removeObserver(observer)
        }
    }

    override fun cancel() {
        super.cancel()
        isCancelled = true
        resourceCallback?.onUploadCancelled()
        resourceCallback = null
    }
}