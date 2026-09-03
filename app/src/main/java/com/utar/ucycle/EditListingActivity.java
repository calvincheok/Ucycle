package com.utar.ucycle;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.utar.ucycle.databinding.ActivityEditListingBinding;
import com.utar.ucycle.model.Listing;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lets an owner change or remove a listing they posted.
 *
 * Safety rule: once someone has requested or borrowed the item, deleting it
 * would leave that person's borrow record pointing at a listing that no longer
 * exists, so delete is blocked and the price/type can no longer change.
 */
public class EditListingActivity extends AppCompatActivity {

    public static final String EXTRA_LISTING_ID = "listing_id";

    private ActivityEditListingBinding binding;
    private FirebaseFirestore db;

    private String listingId;
    private Listing listing;
    private String type = Listing.TYPE_SELL;

    /** Set only when the user picks a replacement photo. */
    private Uri newPhotoUri;

    private ActivityResultLauncher<PickVisualMediaRequest> picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditListingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        listingId = getIntent().getStringExtra(EXTRA_LISTING_ID);

        picker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        newPhotoUri = uri;
                        binding.tvUploadHint.setVisibility(View.GONE);
                        binding.ivPhoto.setImageURI(uri);
                    }
                });

        binding.btnBack.setOnClickListener(v -> finish());
        binding.photoFrame.setOnClickListener(v ->
                picker.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        binding.toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnTypeBorrow) type = Listing.TYPE_BORROW;
            else type = Listing.TYPE_SELL;
            updateTypeFields();
        });

        binding.sliderCondition.addOnChangeListener((slider, value, fromUser) ->
                binding.tvCondition.setText("Condition: " + (int) value + "/10"));

        binding.btnSave.setOnClickListener(v -> save());
        binding.btnDelete.setOnClickListener(v -> confirmDelete());

        load();
    }

    private void load() {
        if (listingId == null) { finish(); return; }
        setLoading(true);

        db.collection("listings").document(listingId).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    listing = doc.toObject(Listing.class);
                    if (listing == null) {
                        Toast.makeText(this, "This listing no longer exists",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    listing.setId(doc.getId());
                    fillForm();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not load listing: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void fillForm() {
        binding.etTitle.setText(listing.getTitle());
        CategoryPicker.attach(this, binding.etCategory, binding.tilCustomCategory,
                binding.etCustomCategory, listing.getCategory());
        binding.etDescription.setText(listing.getDescription());
        binding.sliderCondition.setValue(listing.getCondition());
        binding.tvCondition.setText("Condition: " + listing.getCondition() + "/10");

        if (listing.getPrice() != null) {
            binding.etPrice.setText(String.format(Locale.getDefault(), "%.2f", listing.getPrice()));
        }
        if (listing.getMaxBorrowDays() != null) {
            binding.etBorrowDays.setText(String.valueOf(listing.getMaxBorrowDays()));
        }

        type = listing.getType();
        if (Listing.TYPE_BORROW.equals(type)) binding.toggleType.check(R.id.btnTypeBorrow);
        else binding.toggleType.check(R.id.btnTypeSell);
        updateTypeFields();

        android.graphics.Bitmap photo = ImageUtils.decode(listing.getPhotoData());
        if (photo != null) {
            binding.ivPhoto.setImageBitmap(photo);
            binding.tvUploadHint.setVisibility(View.GONE);
        }

        applySafetyRule();
    }

    /** Locks the risky controls once the item is tied up in a transaction. */
    private void applySafetyRule() {
        boolean available = Listing.STATUS_AVAILABLE.equals(listing.getStatus());

        binding.btnDelete.setEnabled(available);
        binding.toggleType.setEnabled(available);
        binding.btnTypeSell.setEnabled(available);
        binding.btnTypeBorrow.setEnabled(available);
        binding.tilPrice.setEnabled(available);
        binding.tilBorrowDays.setEnabled(available);

        if (available) {
            binding.tvLockNote.setVisibility(View.GONE);
        } else {
            binding.tvLockNote.setVisibility(View.VISIBLE);
            binding.tvLockNote.setText(
                    "This item is currently " + listing.getStatus().toLowerCase()
                            + ", so it cannot be deleted and its price or type cannot change. "
                            + "You can still update the title, description, category, condition and photo.");
        }
    }

    private void updateTypeFields() {
        boolean sell = Listing.TYPE_SELL.equals(type);
        boolean borrow = Listing.TYPE_BORROW.equals(type);
        binding.tilPrice.setVisibility(sell ? View.VISIBLE : View.GONE);
        binding.tilBorrowDays.setVisibility(borrow ? View.VISIBLE : View.GONE);
    }

    private void save() {
        String title = binding.etTitle.getText().toString().trim();
        String category = CategoryPicker.resolve(this, binding.etCategory, binding.etCustomCategory);
        String description = binding.etDescription.getText().toString().trim();

        if (title.isEmpty()) { binding.tilTitle.setError("Enter a title"); return; }
        binding.tilTitle.setError(null);
        if (category.isEmpty()) {
            if (binding.tilCustomCategory.getVisibility() == View.VISIBLE) {
                binding.tilCustomCategory.setError("Name your category");
            } else {
                binding.tilCategory.setError("Choose a category");
            }
            return;
        }
        binding.tilCategory.setError(null);
        binding.tilCustomCategory.setError(null);

        setLoading(true);

        // Re-encoding a replacement photo is slow, so keep it off the UI thread.
        new Thread(() -> {
            String photoData = null;      // null means "leave the existing photo alone"
            boolean photoFailed = false;

            if (newPhotoUri != null) {
                photoData = ImageUtils.encodeForFirestore(this, newPhotoUri);
                if (photoData == null) photoFailed = true;
            }

            final String finalPhoto = photoData;
            final boolean finalFailed = photoFailed;
            runOnUiThread(() -> write(title, category, description, finalPhoto, finalFailed));
        }).start();
    }

    private void write(String title, String category, String description,
                       String photoData, boolean photoFailed) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("category", category);
        updates.put("description", description);
        updates.put("condition", (int) binding.sliderCondition.getValue());

        if (photoData != null) {
            updates.put("photoData", photoData);
        }

        // Price and type only move while nobody has claimed the item.
        if (Listing.STATUS_AVAILABLE.equals(listing.getStatus())) {
            updates.put("type", type);

            String priceText = binding.etPrice.getText().toString().trim();
            Double price = null;
            if (!priceText.isEmpty()) {
                try { price = Double.parseDouble(priceText); }
                catch (NumberFormatException ignored) { }
            }
            updates.put("price", price);

            String daysText = binding.etBorrowDays.getText().toString().trim();
            Integer days = null;
            if (!daysText.isEmpty()) {
                try { days = Integer.parseInt(daysText); }
                catch (NumberFormatException ignored) { }
            }
            updates.put("maxBorrowDays", days);
        }

        db.collection("listings").document(listingId).update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this,
                            photoFailed
                                    ? "Changes saved, but the new photo could not be processed."
                                    : "Changes saved",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not save: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void confirmDelete() {
        if (listing == null) return;

        if (!Listing.STATUS_AVAILABLE.equals(listing.getStatus())) {
            Toast.makeText(this,
                    "This item is " + listing.getStatus().toLowerCase() + " and cannot be deleted.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete this listing?")
                .setMessage("\"" + listing.getTitle() + "\" will be removed permanently. "
                        + "This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> delete())
                .show();
    }

    private void delete() {
        setLoading(true);
        db.collection("listings").document(listingId).delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Listing deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not delete: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
        binding.btnDelete.setEnabled(!loading
                && listing != null
                && Listing.STATUS_AVAILABLE.equals(listing.getStatus()));
    }
}
