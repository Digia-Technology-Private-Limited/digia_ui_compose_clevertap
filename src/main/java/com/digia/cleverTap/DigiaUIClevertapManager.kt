package com.digia.cleverTap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.inapp.customtemplates.TemplateContext
import com.clevertap.android.sdk.inapp.customtemplates.TemplatePresenter
import com.clevertap.android.sdk.inapp.customtemplates.TemplateProducer
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.digia.digiaui.framework.DUIFactory
import com.digia.digiaui.framework.UIActionType
import com.digia.digiaui.init.DigiaUIManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger

object DigiaUIClevertapManager : DisplayUnitListener {

    data class InlineComponent(
        val componentId: String,
        val args: Map<String, Any?>
    )

    private val _slotComponents = MutableStateFlow<Map<String, InlineComponent>>(emptyMap())
    val slotComponents = _slotComponents.asStateFlow()

    private var _isInitialized = false
    private val _activeNudgeViewCount = AtomicInteger(0)
    private val _nextNudgeViewId = AtomicInteger(0)

    fun init(context: android.content.Context) {
        if (_isInitialized) return
        _isInitialized = true

        val cleverTap = CleverTapAPI.getDefaultInstance(context)
        cleverTap?.setDisplayUnitListener(this)

        TemplatesManager.register(
            object : TemplateProducer {
                override fun definedTemplates(): Set<CustomTemplate> {
                    return setOf(
                        CustomTemplate.template(
                            "digia",
                            CustomTemplate.TemplateBuilder()
                                .presenter(object : TemplatePresenter {
                                    override fun onClose(templateContext: TemplateContext) {
                                        templateContext.setDismissed()
                                        DigiaUIManager.getInstance().bottomSheetManager?.dismiss()
                                        DigiaUIManager.getInstance().dialogManager?.dismiss()
                                    }

                                    override fun onPresent(templateContext: TemplateContext) {
                                        if (_activeNudgeViewCount.get() <= 0) {
                                            templateContext.setDismissed()
                                            return
                                        }

                                        val command = templateContext.getString("command")
                                        if (command.isNullOrEmpty()) {
                                            templateContext.setDismissed()
                                            return
                                        }

                                        val actionType = try {
                                            UIActionType.valueOf(command.uppercase())
                                        } catch (e: Exception) {
                                            templateContext.setDismissed()
                                            return
                                        }

                                        val viewId = templateContext.getString("viewId")
                                        if (viewId.isNullOrEmpty()) {
                                            templateContext.setDismissed()
                                            return
                                        }

                                        val argsStr = templateContext.getString("args") ?: "{}"
                                        val mapArgs = try {
                                            val typeToken = object : TypeToken<Map<String, Any?>>() {}.type
                                            Gson().fromJson<Map<String, Any?>>(argsStr, typeToken)
                                        } catch (e: Exception) {
                                            null
                                        }

                                        templateContext.setPresented()

                                        DUIFactory.getInstance().ShowUIAction(
                                            actionType = actionType,
                                            componentId = viewId,
                                            componentArgs = mapArgs,
                                            onDismiss = {
                                                templateContext.setDismissed()
                                            }
                                        )
                                    }
                                })
                                .build()
                        )
                    )
                }
            }
        )
    }

    fun dispose(context: android.content.Context) {
        val cleverTap = CleverTapAPI.getDefaultInstance(context)
        cleverTap?.setDisplayUnitListener(null)
    }

    fun registerActiveNudgeView(): Int {
        _activeNudgeViewCount.incrementAndGet()
        return _nextNudgeViewId.getAndIncrement()
    }

    fun unregisterActiveNudgeView(id: Int) {
        _activeNudgeViewCount.decrementAndGet()
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
                val viewId = customExtras["viewId"] as? String ?: continue
                val actionType = try {
                    UIActionType.valueOf(command.uppercase())
                } catch (e: Exception) {
                    continue
                }
                DUIFactory.getInstance().ShowUIAction(actionType, viewId, argsMap)
            } else {
                val newMappings = mutableMapOf<String, InlineComponent>()
                for (key in customExtras.keys) {
                    if (key == "args") continue
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
