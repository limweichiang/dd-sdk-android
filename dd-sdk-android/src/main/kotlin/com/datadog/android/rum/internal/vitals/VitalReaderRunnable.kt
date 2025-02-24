/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.internal.vitals

import com.datadog.android.core.internal.utils.sdkLogger
import com.datadog.android.rum.internal.RumFeature
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal class VitalReaderRunnable(
    val reader: VitalReader,
    val observer: VitalObserver,
    val executor: ScheduledExecutorService,
    val periodMs: Long
) : Runnable {

    override fun run() {
        val data = reader.readVitalData()
        if (data != null) {
            observer.onNewSample(data)
        }

        try {
            @Suppress("UnsafeThirdPartyFunctionCall") // NPE cannot happen here
            executor.schedule(this, periodMs, TimeUnit.MILLISECONDS)
        } catch (e: RejectedExecutionException) {
            sdkLogger.e(RumFeature.ERROR_VITAL_TASK_REJECTED, e)
        }
    }
}
