/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.response.dflt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@SuppressWarnings({"PointlessBitwiseExpression", "unused"})
public class McuMgrTaskStatResponse extends McuMgrOsResponse {
    // For Zephyr see:
    // https://github.com/zephyrproject-rtos/zephyr/blob/master/kernel/include/kernel_structs.h
    /** Not a real thread */
    public static final int THREAD_DUMMY_MASK = 1 << 0;
    /** Thread is waiting on an object */
    public static final int THREAD_PENDING_MASK = 1 << 1;
    /** Thread has not yet started */
    public static final int THREAD_PRESTART_MASK = 1 << 2;
    /** Thread has terminated */
    public static final int THREAD_DEAD_MASK = 1 << 3;
    /** Thread is suspended */
    public static final int THREAD_SUSPENDED_MASK = 1 << 4;
    // State 1 << 5 is reserved for future use.
    /** Thread is present in the ready queue */
    public static final int THREAD_QUEUED_MASK = 1 << 6;

    // For Mynewt see:
    // https://github.com/apache/mynewt-core/blob/master/kernel/os/include/os/os_task.h
    /** Task is ready to run. */
    public static final int OS_TASK_READY = 1;
    /** Task is sleeping. */
    public static final int OS_TASK_SLEEP = 2;

    /**
     * Task map. The key is the task/thread name.
     */
    @JsonProperty("tasks")
    public Map<String, TaskStat> tasks;

    @JsonCreator
    public McuMgrTaskStatResponse() {}

    /**
     * Structure containing information about a running task.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskStat {
        /** Task Priority. */
        @JsonProperty("prio")
        public long prio;
        /** Task ID. */
        @JsonProperty("tid")
        public long tid;
        /** Task state. See THREAD_* or _OS_TASK_* constants. */
        @JsonProperty("state")
        public long state;
        /** Stack usage, in bytes. */
        @JsonProperty("stkuse")
        public long stkuse;
        /** Size of this task's stack. */
        @JsonProperty("stksiz")
        public long stksiz;
        /** Total number of times this task has been context switched during execution. */
        @JsonProperty("cswcnt")
        public long cswcnt;
        /** Total task run time. */
        @JsonProperty("runtime")
        public long runtime;
        /** Last checking time. */
        @JsonProperty("last_checkin")
        public long last_checkin;
        /** Next checking time. */
        @JsonProperty("next_checkin")
        public long next_checkin;

        @JsonCreator
        public TaskStat() {}
    }
}
