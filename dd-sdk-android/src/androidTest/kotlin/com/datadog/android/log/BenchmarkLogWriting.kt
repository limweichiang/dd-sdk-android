/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2019 Datadog, Inc.
 */

package com.datadog.android.log

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.datadog.android.Datadog
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryException
import fr.xgouchet.elmyr.junit4.ForgeRule
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InvalidObjectException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BenchmarkLogWriting {
    @get:Rule
    val benchmark = BenchmarkRule()
    @get:Rule
    val forge = ForgeRule()

    lateinit var testedLogger: Logger

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        Datadog.initialize(context, "NO_TOKEN", "http://127.0.0.1")

        testedLogger = Logger.Builder()
            .setDatadogLogsEnabled(true)
            .setTimestampsEnabled(true)
            .setNetworkInfoEnabled(true)
            .build()
    }

    @After
    fun tearDown() {
        Datadog.stop()
    }

    @Test
    fun benchmark_writing_logs() {
        benchmark.measureRepeated {
            val message = runWithTimingDisabled { forge.anAlphabeticalString() }
            testedLogger.i(message)
        }
    }

    @Test
    fun benchmark_writing_logs_with_throwable() {
        benchmark.measureRepeated {
            val (message, throwable) = runWithTimingDisabled {
                forge.anAlphabeticalString() to forge.aThrowable()
            }
            testedLogger.e(message, throwable)
        }
    }

    @Test
    fun benchmark_writing_logs_with_attributes() {
        for (i in 1..16) {
            testedLogger.addAttribute(forge.anAlphabeticalString(), forge.anHexadecimalString())
        }
        benchmark.measureRepeated {
            val (message, throwable) = runWithTimingDisabled {
                forge.anAlphabeticalString() to forge.aThrowable()
            }
            testedLogger.e(message, throwable)
        }
    }

    @Test
    fun benchmark_writing_logs_with_tags() {
        for (i in 1..8) {
            testedLogger.addTag(forge.anAlphabeticalString(), forge.anHexadecimalString())
        }
        benchmark.measureRepeated {
            val (message, throwable) = runWithTimingDisabled {
                forge.anAlphabeticalString() to forge.aThrowable()
            }
            testedLogger.e(message, throwable)
        }
    }

    // region Internal

    private fun Forge.aThrowable(): Throwable {
        val errorMessage = anAlphabeticalString()
        return anElementFrom(
            IOException(errorMessage),
            IllegalStateException(errorMessage),
            UnknownError(errorMessage),
            ArrayIndexOutOfBoundsException(errorMessage),
            NullPointerException(errorMessage),
            ForgeryException(errorMessage),
            InvalidObjectException(errorMessage),
            UnsupportedOperationException(errorMessage),
            FileNotFoundException(errorMessage)
        )
    }

    // endregion
}