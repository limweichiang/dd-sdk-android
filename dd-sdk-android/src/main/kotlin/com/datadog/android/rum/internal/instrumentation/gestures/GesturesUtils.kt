package com.datadog.android.rum.internal.instrumentation.gestures

import android.content.res.Resources
import com.datadog.android.core.internal.CoreFeature
import com.datadog.android.rum.tracking.InteractionPredicate

internal fun resolveTargetName(
    interactionPredicate: InteractionPredicate,
    target: Any
): String {
    val customTargetName = interactionPredicate.getTargetName(target)
    return if (!customTargetName.isNullOrEmpty()) {
        customTargetName
    } else {
        ""
    }
}

internal fun resourceIdName(id: Int): String {
    @Suppress("SwallowedException")
    return try {
        CoreFeature.contextRef.get()?.resources?.getResourceEntryName(id)
            ?: idAsStringHexa(id)
    } catch (e: Resources.NotFoundException) {
        idAsStringHexa(id)
    }
}

private fun idAsStringHexa(id: Int) = "0x${id.toString(16)}"
