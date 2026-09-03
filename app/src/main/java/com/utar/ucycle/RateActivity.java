package com.utar.ucycle;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.utar.ucycle.databinding.ActivityRateBinding;
import com.utar.ucycle.model.Rating;

/**
 * Rate the other person after a completed borrow or sale. Stars are required,
 * the comment is optional, and the whole thing can be skipped.
 */
public class RateActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_USER_ID = "target_user_id";
    public static final String EXTRA_CONTEXT = "context";           // BORROW / SALE
    public static final String EXTRA_RECORD_TYPE = "record_type";   // BORROW / SALE
    public static final String EXTRA_RECORD_ID = "record_id";

    private ActivityRateBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser me;

    private String targetUserId;
    private String context;
    private String recordType;
    private String recordId;
    private int stars = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        me = FirebaseAuth.getInstance().getCurrentUser();

        targetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        context = getIntent().getStringExtra(EXTRA_CONTEXT);
        recordType = getIntent().getStringExtra(EXTRA_RECORD_TYPE);
        recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);

        if (targetUserId == null || me == null) { finish(); return; }

        // Show who is being rated.
        db.collection("users").document(targetUserId).get().addOnSuccessListener(doc -> {
            String name = doc.getString("name");
            binding.tvWho.setText("How did it go with " + (name == null ? "them" : name) + "?");
        });

        binding.ratingBar.setOnRatingBarChangeListener((bar, value, fromUser) -> {
            stars = Math.max(1, Math.round(value));
            bar.setRating(stars);
            tintStars();
        });
        tintStars();

        binding.btnSubmit.setOnClickListener(v -> submit());
        binding.btnSkip.setOnClickListener(v -> finish());
    }

    /** Red, amber then green as the score climbs, matching the rest of the app. */
    private void tintStars() {
        int color = ContextCompat.getColor(this, Ratings.colorRes(stars));
        binding.ratingBar.setProgressTintList(ColorStateList.valueOf(color));
        binding.ratingBar.setSecondaryProgressTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chip_border)));

        binding.tvStarLabel.setText(describe(stars));
        binding.tvStarLabel.setTextColor(color);
    }

    private String describe(int stars) {
        switch (stars) {
            case 1:  return "Very poor";
            case 2:  return "Poor";
            case 3:  return "Okay";
            case 4:  return "Good";
            default: return "Excellent";
        }
    }

    private void submit() {
        binding.btnSubmit.setEnabled(false);
        binding.progress.setVisibility(View.VISIBLE);

        db.collection("users").document(me.getUid()).get().addOnSuccessListener(doc -> {
            String myName = doc.getString("name");
            if (myName == null) myName = "Student";

            Rating rating = new Rating();
            rating.setTargetUserId(targetUserId);
            rating.setRaterId(me.getUid());
            rating.setRaterName(myName);
            rating.setStars(stars);
            rating.setComment(binding.etComment.getText().toString().trim());
            rating.setContext(context == null ? "BORROW" : context);

            Ratings.submit(rating)
                    .addOnSuccessListener(v -> {
                        markRecordRated();
                        Toast.makeText(this, "Thanks for the rating", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.btnSubmit.setEnabled(true);
                        binding.progress.setVisibility(View.GONE);
                        Toast.makeText(this, "Could not save: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });
    }

    /** Flags my side as rated so the button does not come back. */
    private void markRecordRated() {
        if (recordId == null || recordType == null) return;

        if (TransactionDetailActivity.TYPE_SALE.equals(recordType)) {
            db.collection("sales").document(recordId).get().addOnSuccessListener(doc -> {
                String sellerId = doc.getString("sellerId");
                boolean iAmSeller = me.getUid().equals(sellerId);
                db.collection("sales").document(recordId)
                        .update(iAmSeller ? "sellerRated" : "buyerRated", true);
            });
        } else {
            db.collection("borrows").document(recordId).get().addOnSuccessListener(doc -> {
                String ownerId = doc.getString("ownerId");
                boolean iAmOwner = me.getUid().equals(ownerId);
                db.collection("borrows").document(recordId)
                        .update(iAmOwner ? "ownerRated" : "borrowerRated", true);
            });
        }
    }
}
