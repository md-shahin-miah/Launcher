package uk.ordere.launcher

import android.app.Activity
import android.view.Window
import android.view.WindowManager

/**
 * An interface implemented by every [Activity], Fragment etc. in Launcher.
 * It handles themes and window flags - a useful abstraction as it is the same everywhere.
 */
interface UIObject {
    fun onStart() {
        if (this is Activity) setWindowFlags(window)

        applyTheme()
        setOnClicks()

        adjustLayout()
    }
    fun setWindowFlags(window: Window) {
        window.setFlags(0, 0) // clear flags


            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            )


            window.setFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
    }


    // Don't use actual themes, rather create them on the fly for faster theme-switching
    fun applyTheme() { }
    fun setOnClicks() { }
    fun adjustLayout() { }
}