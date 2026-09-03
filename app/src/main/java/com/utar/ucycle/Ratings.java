package com.utar.ucycle;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.utar.ucycle.model.Rating;
import com.utar.ucycle.model.UserProfile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Writing a rating and keeping the target's trust score in step.
 *
 * There is one combined score per person, so a rating left after a borrow and
 * one left after a sale both count toward the same average.
 */
public final class Ratings {

    private Ratings() { }

    /**
     * Stores the rating and folds it into the target's running average.
     * The average is recomputed rather than incremented so that deleting a
     * comment later can also correct it.
     */
    public static Task<Void> submit(Rating rating) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ratingRef = db.collection("ratings").document();

        return ratingRef.set(rating)
                .continueWithTask(task -> recalculate(rating.getTargetUserId()));
    }

    /** Re-reads every rating for a user and rewrites their average and count. */
    public static Task<Void> recalculate(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        return db.collection("ratings")
                .whereEqualTo("targetUserId", userId)
                .get()
                .continueWithTask(task -> {
                    int count = 0;
                    int total = 0;
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc
                                : task.getResult().getDocuments()) {
                            Long stars = doc.getLong("stars");
                            if (stars == null) continue;
                            total += stars.intValue();
                            count++;
                        }
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("rating", count == 0 ? 0d : (double) total / count);
                    update.put("ratingCount", count);

                    return db.collection("users").document(userId)
                            .set(update, SetOptions.merge());
                });
    }

    /**
     * A single rating says very little, so a score stays hidden as "New user"
     * until there are enough of them to mean something.
     */
    public static String describeScore(@Nullable UserProfile profile) {
        if (profile == null) return "New user";
        return describeScore(profile.getRating(), profile.getRatingCount());
    }

    public static String describeScore(double rating, int count) {
        if (count < Config.MIN_RATINGS_TO_SHOW_SCORE) return "New user";
        return String.format(Locale.getDefault(), "%.1f \u2605 (%d)", rating, count);
    }

    /**
     * Colour for a score, so quality reads at a glance before anyone reads the
     * number: red for poor, amber for middling, green for good.
     */
    public static int colorRes(double rating) {
        if (rating < 2.5) return R.color.danger;
        if (rating < 4.0) return R.color.amber;
        return R.color.ucycle_green_dark;
    }

    /** The matching pale shade, for chip and badge backgrounds. */
    public static int backgroundColorRes(double rating) {
        if (rating < 2.5) return R.color.danger_light;
        if (rating < 4.0) return R.color.amber_light;
        return R.color.ucycle_green_light;
    }

    /**
     * Same idea for a whole profile, except a score that is still hidden behind
     * "New user" is shown in grey rather than implying it is bad.
     */
    public static int profileColorRes(double rating, int count) {
        if (count < Config.MIN_RATINGS_TO_SHOW_SCORE) return R.color.text_secondary;
        return colorRes(rating);
    }

    public static int profileBackgroundColorRes(double rating, int count) {
        if (count < Config.MIN_RATINGS_TO_SHOW_SCORE) return R.color.ucycle_green_light;
        return backgroundColorRes(rating);
    }

    /** Short form for tight spaces such as a list row. */
    public static String describeScoreShort(double rating, int count) {
        if (count < Config.MIN_RATINGS_TO_SHOW_SCORE) return "New";
        return String.format(Locale.getDefault(), "%.1f \u2605", rating);
    }
}
