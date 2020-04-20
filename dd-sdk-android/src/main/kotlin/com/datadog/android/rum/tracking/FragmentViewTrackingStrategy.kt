/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.tracking

import android.app.Activity
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.datadog.android.rum.internal.tracking.AndroidXFragmentLifecycleCallbacks
import com.datadog.android.rum.internal.tracking.FragmentLifecycleCallbacks
import com.datadog.android.rum.internal.tracking.NoOpLifecycleCallback
import com.datadog.android.rum.internal.tracking.OreoFragmentLifecycleCallbacks

/**
 * A [ViewTrackingStrategy] that will track [Fragment]s as RUM views.
 *
 * Each fragment's lifecycle will be monitored to start and stop RUM views when relevant.
 *
 * **Note**: This version of the [FragmentViewTrackingStrategy] is compatible with
 * the AndroidX Compat Library.
 * @param trackArguments whether we track Fragment arguments
 */
class FragmentViewTrackingStrategy(private val trackArguments: Boolean) :
    ActivityLifecycleTrackingStrategy(),
    ViewTrackingStrategy {

    private val androidXLifecycleCallbacks: FragmentLifecycleCallbacks<FragmentActivity>
        by lazy {
            AndroidXFragmentLifecycleCallbacks {
                if (trackArguments) convertToRumAttributes(it.arguments) else emptyMap()
            }
        }
    private val oreoLifecycleCallbacks: FragmentLifecycleCallbacks<Activity>
        by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                OreoFragmentLifecycleCallbacks {
                    if (trackArguments) convertToRumAttributes(it.arguments) else emptyMap()
                }
            } else {
                NoOpLifecycleCallback()
            }
        }

    // region ActivityLifecycleTrackingStrategy

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (FragmentActivity::class.java.isAssignableFrom(activity::class.java)) {
            androidXLifecycleCallbacks.register(activity as FragmentActivity)
        } else {
            // old deprecated way
            oreoLifecycleCallbacks.register(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        if (FragmentActivity::class.java.isAssignableFrom(activity::class.java)) {
            androidXLifecycleCallbacks.unregister(activity as FragmentActivity)
        } else {
            // old deprecated way
            oreoLifecycleCallbacks.unregister(activity)
        }
    }

    // endregion
}