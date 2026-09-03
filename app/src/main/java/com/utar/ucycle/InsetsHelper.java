package com.utar.ucycle;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * From Android 15 (API 35) onwards the system forces edge-to-edge display, so
 * app content is drawn underneath the status bar and navigation bar unless we
 * handle the insets ourselves. Every screen calls this on its root view.
 *
 * The keyboard is handled here too. Because this listener consumes the insets,
 * windowSoftInputMode="adjustResize" alone can no longer lift the content, so
 * the bottom padding grows to match whichever is taller: the navigation bar or
 * the keyboard. That is what keeps the field you are typing in visible.
 */
public final class InsetsHelper {

    private InsetsHelper() { }

    /** Pads the view clear of the system bars, and of the keyboard when it opens. */
    public static void applyAll(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Same as applyAll but leaves the bottom alone. Used on screens where a
     * child view (bottom navigation, message input bar) should handle the
     * bottom inset itself so it stays flush with the screen edge.
     */
    public static void applyTopOnly(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
