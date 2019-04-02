package com.noahseidman.digiid

import android.accessibilityservice.AccessibilityService
import android.annotation.TargetApi
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.Executors


class Accessibility: AccessibilityService() {

    val executor = Executors.newSingleThreadExecutor()

    companion object {
        var url: String = ""
        var node: AccessibilityNodeInfo? = null
        val name = "com.noahseidman.digiid.Accessibility";
    }

    override fun onInterrupt() {

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (PasswordViewService.isPasswordHelperEnabled(this)) {
                getChromeUrl(getRootInActiveWindow());
            }
        } catch(e: Exception) {

        }
    }

    fun getChromeUrl(nodeInfo: AccessibilityNodeInfo) {
        executor.execute {
            url = ""
            node = null
            try {
                nodeSearch(nodeInfo)
            } catch(e: RecursionBreak) { }
            if (node != null && !url.isEmpty()) {
                PasswordViewService.show(this, node, url)
            } else {
                PasswordViewService.hide(this)
            }
        }
    }

    class RecursionBreak: Exception()

    @TargetApi(Build.VERSION_CODES.O)
    fun nodeSearch(info: AccessibilityNodeInfo?) {
        info?.let {
            for (i in 0 until it.childCount) {
                val childNode = it.getChild(i)
                childNode?.let {
                    if (it.text?.toString()?.contains("http", true) == true &&
                        it.className?.toString()?.contains("edittext", true) == true){
                        url = it.text.toString()
                    }
                    if (it.isPassword && it.isFocused && it.text?.toString()?.isEmpty() == true) {
                        node = it
                    }
                    if (node != null && !url.isEmpty()) {
                        throw RecursionBreak()
                    } else if (childNode.childCount > 0) {
                        nodeSearch(childNode);
                    }
                }
            }
        }
    }
}