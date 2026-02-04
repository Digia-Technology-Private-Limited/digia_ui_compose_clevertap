package com.digia.cleverTap

import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.digia.digiaui.framework.DUIFactory
import com.digia.digiaui.framework.UIActionType
import java.util.ArrayList
import kotlin.text.uppercase
import org.json.JSONObject

object DigiaUIClevertapManager : DisplayUnitListener {

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
            val viewId = customExtras["viewId"] as? String

            if (command == null || viewId == null) continue

            val actionType =
                    try {
                        UIActionType.valueOf(command.uppercase())
                    } catch (e: Exception) {
                        // Log error or skip if the command doesn't match our enum names
                        continue
                    }

            val args =
                    when (val argsRaw = customExtras["args"]) {
                        is String -> JSONObject(argsRaw)
                        is JSONObject -> argsRaw
                        else -> JSONObject()
                    }

            val argsMap = args.keys().asSequence().associate { key -> key to args.get(key) }

            // Assuming DUIFactory has a method 'executeCommand' to handle this.
            DUIFactory.getInstance().ShowUIAction(actionType, viewId, argsMap)
        }
    }
}
