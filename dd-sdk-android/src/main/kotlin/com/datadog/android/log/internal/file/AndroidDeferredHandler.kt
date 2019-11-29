/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2019 Datadog, Inc.
 */

package com.datadog.android.log.internal.file

import android.os.Handler

internal class AndroidDeferredHandler(
    private val handler: Handler
) : DeferredHandler {

    override fun handle(r: Runnable) {
        handler.post(r)
    }
}