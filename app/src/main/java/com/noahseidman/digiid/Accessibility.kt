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
                PasswordViewService.showWebsite(this, node, url)
            } else if (node != null) {
                PasswordViewService.showApp(this, node, nodeInfo.packageName.toString())
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
                    // Find the first field with http in it. Should be the url bar
                    if (url.isEmpty() && !it.text.isNullOrEmpty() && it.text.contains("http", true)){
                        url = it.text.toString()
                    }
                    if ((it.isPassword || (hasHintText() && !it.hintText.isNullOrEmpty() && it.hintText.contains("password"))) && it.isFocused && (it.text.isNullOrEmpty() || (!it.text.isNullOrEmpty() && it.text.contains("password", true)))) {
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
    var hasHintText: Boolean? = null
    fun hasHintText(): Boolean {
        if (hasHintText == null) {
            try {
                AccessibilityNodeInfo::class.java.getMethod("getHintText", *(null as Array<Class<*>>?)!!)
                hasHintText = true
            } catch (e: Throwable) {
                hasHintText = false;
            }
        }
        return hasHintText!!
    }
}