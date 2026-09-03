package com.utar.ucycle;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.utar.ucycle.adapter.RatingAdapter;
import com.utar.ucycle.databinding.ActivityUserProfileBinding;
import com.utar.ucycle.model.Rating;
import com.utar.ucycle.model.UserProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Someone else's public profile: their trust score and the comments people have
 * left. Comments can only be deleted by whoever wrote them, the person being
 * rated may reply once, and anyone can report a comment.
 */
public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";

    private ActivityUserProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser me;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        me = FirebaseAuth.getInstance().getCurrentUser();
        userId = getIntent().getStringExtra(EXTRA_USER_ID);

        binding.btnBack.setOnClickListener(v -> finish());

        if (userId == null || me == null) { finish(); return; }

        binding.recyclerRatings.setLayoutManager(new LinearLayoutManager(this));

        loadProfile();
        loadRatings();
    }

    private void loadProfile() {
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            UserProfile profile = doc.toObject(UserProfile.class);
            if (profile == null) return;

            binding.tvName.setText(profile.getName());
            binding.tvFaculty.setText(profile.getFaculty());
            binding.tvScore.setText(Ratings.describeScore(profile));
            binding.tvScore.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                    Ratings.profileColorRes(profile.getRating(), profile.getRatingCount())));
            binding.tvScore.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(this,
                            Ratings.profileBackgroundColorRes(
                                    profile.getRating(), profile.getRatingCount())));

            String bio = profile.getBio();
            binding.tvBio.setVisibility(bio == null || bio.isEmpty() ? View.GONE : View.VISIBLE);
            binding.tvBio.setText(bio);

            android.graphics.Bitmap photo = ImageUtils.decode(profile.getPhotoData());
            if (photo != null) {
                binding.ivAvatar.setImageBitmap(photo);
                binding.tvAvatarInitials.setVisibility(View.GONE);
            } else {
                binding.tvAvatarInitials.setVisibility(View.VISIBLE);
                binding.tvAvatarInitials.setText(profile.getInitials());
            }
        });
    }

    private void loadRatings() {
        // Single-field query, sorted here, so no composite index is required.
        db.collection("ratings").whereEqualTo("targetUserId", userId).get()
                .addOnSuccessListener(snapshot -> {
                    List<Rating> ratings = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Rating rating = doc.toObject(Rating.class);
                        rating.setId(doc.getId());
                        ratings.add(rating);
                    }
                    Collections.sort(ratings, (a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });

                    RatingAdapter adapter = new RatingAdapter(
                            me.getUid(), userId, new RatingAdapter.Actions() {
                        @Override public void onDelete(Rating r) { confirmDelete(r); }
                        @Override public void onReply(Rating r) { promptReply(r); }
                        @Override public void onReport(Rating r) { report(r); }
                    });
                    adapter.submit(ratings);
                    binding.recyclerRatings.setAdapter(adapter);
                    binding.tvNoRatings.setVisibility(ratings.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.tvNoRatings.setVisibility(View.VISIBLE);
                    binding.tvNoRatings.setText("Could not load ratings.\n\n" + e.getMessage());
                });
    }

    /** Only the author may remove their own comment. */
    private void confirmDelete(Rating rating) {
        new AlertDialog.Builder(this)
                .setTitle("Delete your rating?")
                .setMessage("It will be removed and the trust score recalculated.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) ->
                        db.collection("ratings").document(rating.getId()).delete()
                                .addOnSuccessListener(v -> Ratings.recalculate(userId)
                                        .addOnSuccessListener(x -> {
                                            Toast.makeText(this, "Rating deleted",
                                                    Toast.LENGTH_SHORT).show();
                                            loadProfile();
                                            loadRatings();
                                        })))
                .show();
    }

    /** The rated person gets a right of reply, so an accusation is not one-sided. */
    private void promptReply(Rating rating) {
        EditText input = new EditText(this);
        input.setHint("Your reply");
        input.setText(rating.getReply());

        new AlertDialog.Builder(this)
                .setTitle("Reply to this comment")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) ->
                        db.collection("ratings").document(rating.getId())
                                .update("reply", input.getText().toString().trim())
                                .addOnSuccessListener(v -> loadRatings()))
                .show();
    }

    private void report(Rating rating) {
        db.collection("ratings").document(rating.getId()).update("reported", true)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Reported. Thanks for flagging it.",
                            Toast.LENGTH_SHORT).show();
                    loadRatings();
                });
    }
}
