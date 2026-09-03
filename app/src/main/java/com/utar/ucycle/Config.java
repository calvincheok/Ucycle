package com.utar.ucycle;

import android.util.Patterns;

/**
 * Central place for switches we want to flip between development and the
 * final demo, so nobody has to hunt through the code.
 */
public final class Config {

    private Config() { }

    /**
     * When true, only official UTAR addresses may register or log in.
     *
     * Set to FALSE during development so the group can test with any email
     * (Gmail, Outlook, etc.) without needing real UTAR accounts.
     * Set to TRUE before the final demo / submission so the app behaves the way
     * the proposal describes.
     */
    public static final boolean RESTRICT_TO_UTAR_EMAIL = false;

    /**
     * When true, the user must open the verification link sent to their inbox
     * before they can log in.
     *
     * Currently FALSE so the group can sign up with made-up addresses such as
     * abcdefg@1utar.my, which have no real inbox to receive the link.
     * Set back to TRUE for the final demo (use real email addresses then) -
     * this is the feature the proposal calls "OTP verification".
     */
    public static final boolean REQUIRE_EMAIL_VERIFICATION = false;

    /**
     * How many days past the due date before the owner may force-complete a
     * borrow the borrower never marked as returned.
     * Set to 0 when demonstrating, so the button appears immediately.
     */
    public static final int OVERDUE_GRACE_DAYS = 0;

    /** A trust score is hidden behind "New user" until this many ratings exist. */
    public static final int MIN_RATINGS_TO_SHOW_SCORE = 3;

    private static final String[] UTAR_DOMAINS = { "@1utar.my", "@utar.edu.my" };

    /** Validates the address according to whichever mode is switched on above. */
    public static boolean isEmailAllowed(String email) {
        if (email == null) return false;
        String e = email.trim().toLowerCase();
        if (e.isEmpty()) return false;

        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) return false;

        if (!RESTRICT_TO_UTAR_EMAIL) return true;

        for (String domain : UTAR_DOMAINS) {
            if (e.endsWith(domain)) return true;
        }
        return false;
    }

    /** Error text shown when the address is rejected. */
    public static String emailError() {
        return RESTRICT_TO_UTAR_EMAIL
                ? "Please use your official UTAR email (@1utar.my / @utar.edu.my)"
                : "Please enter a valid email address";
    }

    /** Hint / label used on the email fields. */
    public static String emailHint() {
        return RESTRICT_TO_UTAR_EMAIL ? "UTAR email" : "Email";
    }
}
