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
import com.utar.ucycle.databinding.ActivityCreateListingBinding;
import com.utar.ucycle.model.Listing;

public class CreateListingActivity extends AppCompatActivity {

    /** Set when this screen is opened to offer an item against someone's request. */
    public static final String EXTRA_OFFER_FOR_REQUEST_ID = "offer_for_request_id";
    /** MODE_BORROW or MODE_BUY, i.e. what the requester asked for. */
    public static final String EXTRA_OFFER_MODE = "offer_mode";

    private ActivityCreateListingBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser me;
    private Uri photoUri;
    private String type = Listing.TYPE_SELL;
    private String requestMode = Listing.MODE_BORROW;

    /** Non-empty when offering an item in reply to a request. */
    private String offerForRequestId = "";

    private ActivityResultLauncher<PickVisualMediaRequest> picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateListingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        me = FirebaseAuth.getInstance().getCurrentUser();

        picker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        photoUri = uri;
                        binding.tvUploadHint.setVisibility(View.GONE);
                        binding.ivPhoto.setImageURI(uri);
                    }
                });

        binding.btnBack.setOnClickListener(v -> finish());

        binding.photoFrame.setOnClickListener(v ->
                picker.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        offerForRequestId = getIntent().getStringExtra(EXTRA_OFFER_FOR_REQUEST_ID);
        if (offerForRequestId == null) offerForRequestId = "";

        binding.toggleType.check(R.id.btnTypeSell);
        binding.toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnTypeBorrow) type = Listing.TYPE_BORROW;
            else if (checkedId == R.id.btnTypeRequest) type = Listing.TYPE_REQUEST;
            else type = Listing.TYPE_SELL;
            updateTypeFields();
        });

        binding.toggleRequestMode.check(R.id.btnModeBorrow);
        binding.toggleRequestMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            requestMode = (checkedId == R.id.btnModeBuy) ? Listing.MODE_BUY : Listing.MODE_BORROW;
        });

        if (!offerForRequestId.isEmpty()) {
            setUpOfferMode();
        }
        updateTypeFields();

        binding.sliderCondition.addOnChangeListener((slider, value, fromUser) ->
                binding.tvCondition.setText("Condition: " + (int) value + "/10"));

        CategoryPicker.attach(this, binding.etCategory, binding.tilCustomCategory,
                binding.etCustomCategory, null);

        binding.btnPost.setOnClickListener(v -> post());
    }

    /**
     * Offering an item against a request is just a normal post with the type
     * already decided by what the requester asked for, so the chooser goes away.
     */
    private void setUpOfferMode() {
        String mode = getIntent().getStringExtra(EXTRA_OFFER_MODE);
        type = Listing.MODE_BUY.equals(mode) ? Listing.TYPE_SELL : Listing.TYPE_BORROW;

        binding.toggleType.setVisibility(View.GONE);
        binding.tvOfferHint.setVisibility(View.VISIBLE);
        binding.tvOfferHint.setText(Listing.MODE_BUY.equals(mode)
                ? "You are offering this item for sale to the person who asked for it. "
                        + "It stays private until they accept."
                : "You are offering to lend this item to the person who asked for it. "
                        + "It stays private until they accept.");
        binding.btnPost.setText("Send offer");
    }

    private void updateTypeFields() {
        boolean request = Listing.TYPE_REQUEST.equals(type);
        boolean sell = Listing.TYPE_SELL.equals(type);
        boolean borrow = Listing.TYPE_BORROW.equals(type);

        // A request describes something the person does not have yet, so there is
        // no condition, price or photo to give.
        binding.tvRequestHint.setVisibility(request ? View.VISIBLE : View.GONE);
        binding.toggleRequestMode.setVisibility(request ? View.VISIBLE : View.GONE);
        binding.photoFrame.setVisibility(request ? View.GONE : View.VISIBLE);
        binding.tvCondition.setVisibility(request ? View.GONE : View.VISIBLE);
        binding.sliderCondition.setVisibility(request ? View.GONE : View.VISIBLE);

        binding.tilPrice.setVisibility(sell && !request ? View.VISIBLE : View.GONE);
        binding.tilBorrowDays.setVisibility(borrow && !request ? View.VISIBLE : View.GONE);
    }

    private void post() {
        String title = binding.etTitle.getText().toString().trim();
        String category = CategoryPicker.resolve(this, binding.etCategory, binding.etCustomCategory);
        String description = binding.etDescription.getText().toString().trim();

        if (title.isEmpty()) { binding.tilTitle.setError("Enter a title"); return; }
        binding.tilTitle.setError(null);
        if (category.isEmpty()) {
            // Blank means either nothing picked, or "Others" chosen with no name given.
            if (binding.tilCustomCategory.getVisibility() == View.VISIBLE) {
                binding.tilCustomCategory.setError("Name your category");
            } else {
                binding.tilCategory.setError("Choose a category");
            }
            return;
        }
        binding.tilCategory.setError(null);
        binding.tilCustomCategory.setError(null);
        if (me == null) return;

        setLoading(true);

        // Encoding a photo can take a moment, so keep it off the UI thread.
        new Thread(() -> {
            String photoData = "";
            boolean photoFailed = false;

            if (photoUri != null) {
                photoData = ImageUtils.encodeForFirestore(this, photoUri);
                if (photoData == null) {
                    photoData = "";
                    photoFailed = true;
                }
            }

            final String finalPhoto = photoData;
            final boolean finalPhotoFailed = photoFailed;
            runOnUiThread(() -> save(title, category, description, finalPhoto, finalPhotoFailed));
        }).start();
    }

    /**
     * The listing is always saved, even when the photo could not be processed.
     * Losing someone's whole post because of one bad image would be worse than
     * saving it without a picture.
     */
    private void save(String title, String category, String description,
                      String photoData, boolean photoFailed) {

        db.collection("users").document(me.getUid()).get().addOnSuccessListener(userDoc -> {
            String name = userDoc.getString("name");
            String faculty = userDoc.getString("faculty");
            String owner = (name == null ? "Student" : name)
                    + (faculty == null || faculty.isEmpty() ? "" : " - " + faculty);

            Listing listing = new Listing();
            listing.setOwnerId(me.getUid());
            listing.setOwnerName(owner);
            listing.setTitle(title);
            listing.setCategory(category);
            listing.setDescription(description);
            listing.setCondition((int) binding.sliderCondition.getValue());
            listing.setType(type);
            listing.setPhotoData(photoData);
            listing.setRequestMode(requestMode);
            listing.setOfferForRequestId(offerForRequestId);
            // An offer waits for the requester to decide, so it never hits the feed.
            if (!offerForRequestId.isEmpty()) {
                listing.setStatus(Listing.STATUS_RESERVED);
            }

            String priceText = binding.etPrice.getText().toString().trim();
            if (!priceText.isEmpty()) {
                try { listing.setPrice(Double.parseDouble(priceText)); }
                catch (NumberFormatException ignored) { }
            }
            String daysText = binding.etBorrowDays.getText().toString().trim();
            if (!daysText.isEmpty()) {
                try { listing.setMaxBorrowDays(Integer.parseInt(daysText)); }
                catch (NumberFormatException ignored) { }
            }

            db.collection("listings").add(listing)
                    .addOnSuccessListener(ref -> {
                        String message;
                        if (!offerForRequestId.isEmpty()) {
                            message = "Offer sent. You will hear back if they accept.";
                        } else if (photoFailed) {
                            message = "Listing posted, but the photo could not be processed.";
                        } else {
                            message = Listing.TYPE_REQUEST.equals(type)
                                    ? "Request posted" : "Listing posted";
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Failed to post: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });

        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(this, "Could not load your profile: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnPost.setEnabled(!loading);
    }
}
