package com.digia.cleverTap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.digia.digiaui.framework.DUIFactory
import com.digia.digiaui.framework.UIActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.util.ArrayList

object DigiaUIClevertapManager : DisplayUnitListener {

    data class InlineComponent(
        val componentId: String,
        val args: Map<String, Any?>
    )

    private val _slotComponents = MutableStateFlow<Map<String, InlineComponent>>(emptyMap())
    val slotComponents = _slotComponents.asStateFlow()

    fun init(context: android.content.Context) {
        val cleverTap = CleverTapAPI.getDefaultInstance(context)
        cleverTap?.setDisplayUnitListener(this)
    }

    fun dispose(context: android.content.Context) {
        val cleverTap = CleverTapAPI.getDefaultInstance(context)
        cleverTap?.setDisplayUnitListener(null)
    }

    override fun onDisplayUnitsLoaded(units: ArrayList<CleverTapDisplayUnit?>?) {
        if (units.isNullOrEmpty()) return

        for (unit in units) {
            if (unit == null) continue

            val customExtras = unit.customExtras ?: continue

            val command = customExtras["command"] as? String

            val args = when (val argsRaw = customExtras["args"]) {
                is String -> try { JSONObject(argsRaw) } catch (e: Exception) { JSONObject() }
                is JSONObject -> argsRaw
                else -> JSONObject()
            }
            val argsMap = args.keys().asSequence().associate { key -> key to args.get(key) }

            if (command != null) {
                // UI Action like Bottom Sheet or Dialog
                val viewId = customExtras["viewId"] as? String ?: continue
                
                val actionType = try {
                    UIActionType.valueOf(command.uppercase())
                } catch (e: Exception) {
                    // Log error or skip if the command doesn't match our enum names
                    continue
                }
                DUIFactory.getInstance().ShowUIAction(actionType, viewId, argsMap)
            } else {
                // Inline component mapping
                val newMappings = mutableMapOf<String, InlineComponent>()
                for (key in customExtras.keys) {
                    if (key == "args") continue // skip args
                    
                    val value = customExtras[key]
                    if (value is String) {
                        newMappings[key] = InlineComponent(value, argsMap)
                    }
                }
                
                if (newMappings.isNotEmpty()) {
                    _slotComponents.update { it + newMappings }
                }
            }
        }
    }

    @Composable
    fun showInLine(key: String, fallback: @Composable () -> Unit) {
        val components by slotComponents.collectAsState()
        val component = components[key]

        if (component != null) {
            DUIFactory.getInstance().CreateComponent(
                componentId = component.componentId,
                args = component.args.ifEmpty { null }
            )
        } else {
            fallback()
        }
    }
}
