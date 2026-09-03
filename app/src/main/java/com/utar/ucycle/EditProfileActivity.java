package com.utar.ucycle;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.utar.ucycle.databinding.ActivityEditProfileBinding;
import com.utar.ucycle.model.UserProfile;

import java.util.HashMap;
import java.util.Map;

/**
 * Lets the user change their display name and, optionally, a profile picture,
 * faculty, short bio and contact detail. Only the name is required.
 */
public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser me;
    private UserProfile profile;

    /** Set only when the user picks a new picture. */
    private Uri newPhotoUri;

    private ActivityResultLauncher<PickVisualMediaRequest> picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        me = FirebaseAuth.getInstance().getCurrentUser();

        picker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        newPhotoUri = uri;
                        binding.ivAvatar.setImageURI(uri);
                        binding.tvAvatarInitials.setVisibility(View.GONE);
                        binding.btnRemovePhoto.setVisibility(View.VISIBLE);
                    }
                });

        binding.btnBack.setOnClickListener(v -> finish());
        binding.avatarFrame.setOnClickListener(v -> pickPhoto());
        binding.btnChangePhoto.setOnClickListener(v -> pickPhoto());
        binding.btnRemovePhoto.setOnClickListener(v -> removePhoto());
        binding.btnSave.setOnClickListener(v -> save());

        load();
    }

    private void pickPhoto() {
        picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    /** Clears the picture and falls back to the initials circle. */
    private void removePhoto() {
        newPhotoUri = null;
        if (profile != null) profile.setPhotoData("");
        binding.ivAvatar.setImageDrawable(null);
        binding.tvAvatarInitials.setVisibility(View.VISIBLE);
        binding.btnRemovePhoto.setVisibility(View.GONE);
    }

    private void load() {
        if (me == null) { finish(); return; }
        setLoading(true);

        db.collection("users").document(me.getUid()).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    profile = doc.toObject(UserProfile.class);
                    if (profile == null) profile = new UserProfile();
                    fillForm();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not load your profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void fillForm() {
        binding.etName.setText(profile.getName());
        binding.etFaculty.setText(profile.getFaculty());
        binding.etBio.setText(profile.getBio());
        binding.etContact.setText(profile.getContact());
        binding.tvEmail.setText(profile.getEmail());
        binding.tvAvatarInitials.setText(profile.getInitials());

        android.graphics.Bitmap photo = ImageUtils.decode(profile.getPhotoData());
        if (photo != null) {
            binding.ivAvatar.setImageBitmap(photo);
            binding.tvAvatarInitials.setVisibility(View.GONE);
            binding.btnRemovePhoto.setVisibility(View.VISIBLE);
        } else {
            binding.tvAvatarInitials.setVisibility(View.VISIBLE);
            binding.btnRemovePhoto.setVisibility(View.GONE);
        }
    }

    private void save() {
        String name = binding.etName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.tilName.setError("Enter your name");
            return;
        }
        binding.tilName.setError(null);

        setLoading(true);

        // Encoding the picture is slow, so do it off the UI thread.
        new Thread(() -> {
            String photoData = null;   // null = leave whatever is stored alone
            boolean photoFailed = false;

            if (newPhotoUri != null) {
                photoData = ImageUtils.encodeForFirestore(this, newPhotoUri);
                if (photoData == null) photoFailed = true;
            } else if (profile != null && !profile.hasPhoto()) {
                // user removed their picture
                photoData = "";
            }

            final String finalPhoto = photoData;
            final boolean finalFailed = photoFailed;
            runOnUiThread(() -> write(name, finalPhoto, finalFailed));
        }).start();
    }

    private void write(String name, String photoData, boolean photoFailed) {
        Map<String, Object> updates = new HashMap<>();
        // uid/email are included so the document is complete even when this is
        // the first time it gets written.
        updates.put("uid", me.getUid());
        if (me.getEmail() != null) updates.put("email", me.getEmail());
        updates.put("name", name);
        updates.put("faculty", binding.etFaculty.getText().toString().trim());
        updates.put("bio", binding.etBio.getText().toString().trim());
        updates.put("contact", binding.etContact.getText().toString().trim());
        if (photoData != null) updates.put("photoData", photoData);

        // set(..., merge) instead of update(): update() fails outright when the
        // user document does not exist yet, which happens if the account was
        // made before profiles were stored or the collection was cleared.
        db.collection("users").document(me.getUid()).set(updates, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    refreshNameOnMyListings(name, photoFailed);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not save: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Each listing stores the owner's name so the feed does not have to look it
     * up per item. After a rename those copies would be stale, so refresh them.
     */
    private void refreshNameOnMyListings(String name, boolean photoFailed) {
        String faculty = binding.etFaculty.getText().toString().trim();
        String display = name + (faculty.isEmpty() ? "" : " - " + faculty);

        db.collection("listings").whereEqualTo("ownerId", me.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        WriteBatch batch = db.batch();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            batch.update(doc.getReference(), "ownerName", display);
                        }
                        batch.commit();
                    }
                    finishWithMessage(photoFailed);
                })
                .addOnFailureListener(e -> finishWithMessage(photoFailed));
    }

    private void finishWithMessage(boolean photoFailed) {
        Toast.makeText(this,
                photoFailed
                        ? "Profile saved, but the picture could not be processed."
                        : "Profile updated",
                Toast.LENGTH_LONG).show();
        finish();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
    }
}
