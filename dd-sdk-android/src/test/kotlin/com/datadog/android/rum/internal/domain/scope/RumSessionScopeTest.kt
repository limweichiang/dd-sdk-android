/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.internal.domain.scope

import com.datadog.android.core.internal.data.Writer
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.internal.domain.RumContext
import com.datadog.android.rum.internal.domain.event.RumEvent
import com.datadog.android.utils.forge.Configurator
import com.datadog.android.utils.forge.exhaustiveAttributes
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class RumSessionScopeTest {

    lateinit var testedScope: RumSessionScope

    @Mock
    lateinit var mockParentScope: RumScope

    @Mock
    lateinit var mockChildScope: RumScope

    @Mock
    lateinit var mockEvent: RumRawEvent

    @Mock
    lateinit var mockWriter: Writer<RumEvent>

    @Forgery
    lateinit var fakeParentContext: RumContext

    @BeforeEach
    fun `set up`() {
        whenever(mockParentScope.getRumContext()) doReturn fakeParentContext
        whenever(mockChildScope.handleEvent(any(), any())) doReturn mockChildScope
        testedScope = RumSessionScope(mockParentScope, TEST_INACTIVITY_NS, TEST_MAX_DURATION_NS)

        assertThat(GlobalRum.getRumContext()).isEqualTo(testedScope.getRumContext())
    }

    @AfterEach
    fun `tear down`() {
        // We never send events from this scope
        verifyZeroInteractions(mockWriter)
    }

    @Test
    fun `updates sessionId if first call`() {
        val context = testedScope.getRumContext()

        assertThat(context.sessionId).isNotEqualTo(UUID(0, 0))
        assertThat(context.applicationId).isEqualTo(fakeParentContext.applicationId)
        assertThat(context.viewId).isEqualTo(fakeParentContext.viewId)
    }

    @Test
    fun `updates sessionId if last interaction too old`() {
        val firstSessionId = testedScope.getRumContext().sessionId

        Thread.sleep(TEST_INACTIVITY_MS)
        val context = testedScope.getRumContext()

        assertThat(context.sessionId)
            .isNotEqualTo(UUID(0, 0))
            .isNotEqualTo(firstSessionId)
        assertThat(context.applicationId).isEqualTo(fakeParentContext.applicationId)
        assertThat(context.viewId).isEqualTo(fakeParentContext.viewId)
    }

    @Test
    fun `updates sessionId if duration is too long`() {
        val firstSessionId = testedScope.getRumContext().sessionId

        Thread.sleep(TEST_MAX_DURATION_MS)
        val context = testedScope.getRumContext()

        assertThat(context.sessionId)
            .isNotEqualTo(UUID(0, 0))
            .isNotEqualTo(firstSessionId)
        assertThat(context.applicationId).isEqualTo(fakeParentContext.applicationId)
        assertThat(context.viewId).isEqualTo(fakeParentContext.viewId)
    }

    @Test
    fun `updates sessionId if duration is too long with updates`() {
        val repeatCount = (TEST_MAX_DURATION_MS / TEST_SLEEP_MS) + 1
        val firstSessionId = testedScope.getRumContext().sessionId

        for (i in 0..repeatCount) {
            Thread.sleep(TEST_SLEEP_MS)
            testedScope.handleEvent(mockEvent, mockWriter)
        }
        Thread.sleep(TEST_SLEEP_MS)
        val context = testedScope.getRumContext()

        assertThat(context.sessionId)
            .isNotEqualTo(UUID(0, 0))
            .isNotEqualTo(firstSessionId)
        assertThat(context.applicationId).isEqualTo(fakeParentContext.applicationId)
        assertThat(context.viewId).isEqualTo(fakeParentContext.viewId)
    }

    @Test
    fun `keeps sessionId if last interaction is recent`() {
        val repeatCount = (TEST_INACTIVITY_MS / TEST_SLEEP_MS) + 1
        val firstSessionId = testedScope.getRumContext().sessionId

        for (i in 0..repeatCount) {
            Thread.sleep(TEST_SLEEP_MS)
            testedScope.handleEvent(mockEvent, mockWriter)
        }
        val context = testedScope.getRumContext()

        assertThat(context.sessionId).isEqualTo(firstSessionId)
        assertThat(context.applicationId).isEqualTo(fakeParentContext.applicationId)
        assertThat(context.viewId).isEqualTo(fakeParentContext.viewId)
    }

    @Test
    fun `does Nothing if childless and event not recognized`() {
        val result = testedScope.handleEvent(mockEvent, mockWriter)

        assertThat(testedScope.activeChildrenScopes).isEmpty()
        assertThat(result).isSameAs(testedScope)
    }

    @Test
    fun `delegates to child scopes if event not recognized`() {
        testedScope.activeChildrenScopes.add(mockChildScope)

        val result = testedScope.handleEvent(mockEvent, mockWriter)

        verify(mockChildScope).handleEvent(mockEvent, mockWriter)
        assertThat(testedScope.activeChildrenScopes).containsExactly(mockChildScope)
        assertThat(result).isSameAs(testedScope)
    }

    @Test
    fun `updates child scope on StartView event`(
        @StringForgery(StringForgeryType.ALPHABETICAL) key: String,
        @StringForgery(StringForgeryType.ALPHABETICAL) name: String,
        forge: Forge
    ) {
        val attributes = forge.exhaustiveAttributes()
        mockEvent = RumRawEvent.StartView(key, name, attributes)

        val result = testedScope.handleEvent(mockEvent, mockWriter)

        assertThat(testedScope.activeChildrenScopes).hasSize(1)
        val viewScope = testedScope.activeChildrenScopes.first() as RumViewScope
        assertThat(viewScope.keyRef.get()).isEqualTo(key)
        assertThat(viewScope.name).isEqualTo(name)
        assertThat(viewScope.attributes).isEqualTo(attributes)
        assertThat(result).isSameAs(testedScope)
    }

    @Test
    fun `updates child scope on StartView event with existing children`(
        @StringForgery(StringForgeryType.ALPHABETICAL) key: String,
        @StringForgery(StringForgeryType.ALPHABETICAL) name: String,
        forge: Forge
    ) {
        testedScope.activeChildrenScopes.add(mockChildScope)
        val attributes = forge.exhaustiveAttributes()
        mockEvent = RumRawEvent.StartView(key, name, attributes)

        val result = testedScope.handleEvent(mockEvent, mockWriter)

        verify(mockChildScope).handleEvent(mockEvent, mockWriter)
        assertThat(testedScope.activeChildrenScopes).hasSize(2)
        val viewScope = testedScope.activeChildrenScopes.last() as RumViewScope
        assertThat(viewScope.keyRef.get()).isEqualTo(key)
        assertThat(viewScope.name).isEqualTo(name)
        assertThat(viewScope.attributes).containsAllEntriesOf(attributes)
        assertThat(result).isSameAs(testedScope)
    }

    @Test
    fun `keeps children scope on child handled event`() {
        testedScope.activeChildrenScopes.add(mockChildScope)
        whenever(mockChildScope.handleEvent(mockEvent, mockWriter)) doReturn mockChildScope

        val result = testedScope.handleEvent(mockEvent, mockWriter)

        assertThat(testedScope.activeChildrenScopes).containsExactly(mockChildScope)
        assertThat(result).isSameAs(testedScope)
    }

    @Test
    fun `updates children scope on child handled event (null)`() {
        testedScope.activeChildrenScopes.add(mockChildScope)
        val newChildScope: RumScope = mock()
        whenever(mockChildScope.handleEvent(mockEvent, mockWriter)) doReturn null

        val result = testedScope.handleEvent(mockEvent, mockWriter)

        assertThat(testedScope.activeChildrenScopes).isEmpty()
        assertThat(result).isSameAs(testedScope)
    }

    companion object {

        private const val TEST_SLEEP_MS = 200L
        private const val TEST_INACTIVITY_MS = TEST_SLEEP_MS * 3
        private const val TEST_MAX_DURATION_MS = TEST_SLEEP_MS * 10

        private val TEST_INACTIVITY_NS = TimeUnit.MILLISECONDS.toNanos(TEST_INACTIVITY_MS)
        private val TEST_MAX_DURATION_NS = TimeUnit.MILLISECONDS.toNanos(TEST_MAX_DURATION_MS)
    }
}