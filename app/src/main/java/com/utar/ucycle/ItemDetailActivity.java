package com.utar.ucycle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.utar.ucycle.databinding.ActivityItemDetailBinding;
import com.utar.ucycle.model.BorrowRecord;
import com.utar.ucycle.model.ChatThread;
import com.utar.ucycle.model.Listing;
import com.utar.ucycle.model.SaleRecord;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LISTING_ID = "listing_id";

    private ActivityItemDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser me;
    private Listing listing;
    private String listingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItemDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        me = FirebaseAuth.getInstance().getCurrentUser();
        listingId = getIntent().getStringExtra(EXTRA_LISTING_ID);

        binding.btnBack.setOnClickListener(v -> finish());
        loadListing();
    }

    private void loadListing() {
        if (listingId == null) { finish(); return; }
        db.collection("listings").document(listingId).get()
                .addOnSuccessListener(this::bind)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load item", Toast.LENGTH_SHORT).show());
    }

    private void bind(DocumentSnapshot doc) {
        listing = doc.toObject(Listing.class);
        if (listing == null) { finish(); return; }
        listing.setId(doc.getId());

        binding.tvTitle.setText(listing.getTitle());
        binding.tvCategory.setText(listing.getCategory());
        binding.tvDescription.setText(listing.getDescription());
        binding.tvOwner.setText("Posted by " + listing.getOwnerName() + "  \u203A");
        binding.tvOwner.setOnClickListener(v -> openUserProfile(listing.getOwnerId()));
        binding.chipType.setText(listing.getType());
        binding.chipCondition.setText("Condition: " + listing.getCondition() + "/10");

        if (listing.getPrice() != null) {
            binding.tvPrice.setVisibility(View.VISIBLE);
            binding.tvPrice.setText(String.format(Locale.getDefault(), "RM %.2f", listing.getPrice()));
        } else {
            binding.tvPrice.setVisibility(View.GONE);
        }

        android.graphics.Bitmap photo = ImageUtils.decode(listing.getPhotoData());
        if (photo != null) {
            binding.ivPhoto.setImageBitmap(photo);
        }

        boolean isMine = me != null && me.getUid().equals(listing.getOwnerId());
        boolean available = Listing.STATUS_AVAILABLE.equals(listing.getStatus());

        // A request post behaves differently: the poster manages offers, and
        // everyone else can offer an item they own.
        if (listing.isRequest()) {
            bindRequest(isMine);
            return;
        }

        if (isMine) {
            binding.tvStatusNote.setVisibility(View.VISIBLE);
            binding.tvStatusNote.setText("This is your listing (" + listing.getStatus() + ")");
            binding.btnPrimary.setVisibility(View.GONE);
            binding.btnChat.setVisibility(View.GONE);
        } else if (!available) {
            binding.tvStatusNote.setVisibility(View.VISIBLE);
            binding.tvStatusNote.setText("Currently " + listing.getStatus().toLowerCase());
            binding.btnPrimary.setEnabled(false);
            binding.btnPrimary.setText("Not available");
            binding.btnChat.setVisibility(View.VISIBLE);
            binding.btnChat.setOnClickListener(v -> openChat());
        } else {
            binding.tvStatusNote.setVisibility(View.GONE);
            binding.btnChat.setVisibility(View.VISIBLE);
            binding.btnChat.setOnClickListener(v -> openChat());

            if (listing.isBorrowable()) {
                binding.btnPrimary.setText("Request to borrow");
                binding.btnPrimary.setOnClickListener(v -> requestBorrow());
            } else {
                binding.btnPrimary.setText("Request to buy");
                binding.btnPrimary.setOnClickListener(v -> requestBuy());
            }
        }
    }

    private void requestBorrow() {
        if (me == null || listing == null) return;
        binding.btnPrimary.setEnabled(false);

        db.collection("users").document(me.getUid()).get().addOnSuccessListener(userDoc -> {
            String myName = userDoc.getString("name");
            if (myName == null) myName = "Student";

            BorrowRecord record = new BorrowRecord();
            record.setListingId(listing.getId());
            record.setListingTitle(listing.getTitle());
            record.setListingPhotoData(listing.getPhotoData());
            record.setOwnerId(listing.getOwnerId());
            record.setBorrowerId(me.getUid());
            record.setBorrowerName(myName);

            db.collection("borrows").add(record).addOnSuccessListener(ref -> {
                db.collection("listings").document(listing.getId())
                        .update("status", Listing.STATUS_REQUESTED);
                binding.btnPrimary.setText("Request sent");
                Toast.makeText(this,
                        "Borrow request sent. The owner will review it.",
                        Toast.LENGTH_LONG).show();
            }).addOnFailureListener(e -> {
                binding.btnPrimary.setEnabled(true);
                Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show();
            });
        });
    }

    /** A "does anyone have this" post, seen either by its author or by a helper. */
    private void bindRequest(boolean isMine) {
        binding.chipCondition.setVisibility(View.GONE);
        binding.tvPrice.setVisibility(View.GONE);

        if (isMine) {
            binding.tvStatusNote.setVisibility(View.VISIBLE);
            binding.tvStatusNote.setText(
                    "This is your request. Offers from others stay private until you accept one.");
            binding.btnPrimary.setVisibility(View.VISIBLE);
            binding.btnPrimary.setText("View offers");
            binding.btnPrimary.setOnClickListener(v -> {
                Intent intent = new Intent(this, RequestOffersActivity.class);
                intent.putExtra(RequestOffersActivity.EXTRA_REQUEST_ID, listing.getId());
                startActivity(intent);
            });
            binding.btnChat.setVisibility(View.VISIBLE);
            binding.btnChat.setText("Cancel this request");
            binding.btnChat.setOnClickListener(v -> cancelRequest());
        } else {
            binding.tvStatusNote.setVisibility(View.GONE);
            binding.btnPrimary.setVisibility(View.VISIBLE);
            binding.btnPrimary.setText("I have this");
            binding.btnPrimary.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreateListingActivity.class);
                intent.putExtra(CreateListingActivity.EXTRA_OFFER_FOR_REQUEST_ID, listing.getId());
                intent.putExtra(CreateListingActivity.EXTRA_OFFER_MODE, listing.getRequestMode());
                startActivity(intent);
            });
            binding.btnChat.setVisibility(View.VISIBLE);
            binding.btnChat.setText("Chat with them");
            binding.btnChat.setOnClickListener(v -> openChat());
        }
    }

    private void openUserProfile(String userId) {
        if (userId == null || userId.isEmpty()) return;
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }

    private void cancelRequest() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel this request?")
                .setMessage("It will be removed from the feed. Any offers made to you are withdrawn.")
                .setNegativeButton("Keep it", null)
                .setPositiveButton("Cancel request", (d, w) ->
                        db.collection("listings").document(listing.getId())
                                .update("status", Listing.STATUS_CLOSED)
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(this, "Request cancelled", Toast.LENGTH_SHORT).show();
                                    finish();
                                }))
                .show();
    }

    /**
     * Buying works like borrowing: a record is created, the seller accepts, then
     * both sides confirm the handover after meeting in person.
     */
    private void requestBuy() {
        if (me == null || listing == null) return;
        binding.btnPrimary.setEnabled(false);

        db.collection("users").document(me.getUid()).get().addOnSuccessListener(userDoc -> {
            String myName = userDoc.getString("name");
            if (myName == null) myName = "Student";

            SaleRecord sale = new SaleRecord();
            sale.setListingId(listing.getId());
            sale.setListingTitle(listing.getTitle());
            sale.setListingPhotoData(listing.getPhotoData());
            sale.setSellerId(listing.getOwnerId());
            sale.setSellerName(listing.getOwnerName());
            sale.setBuyerId(me.getUid());
            sale.setBuyerName(myName);
            sale.setPrice(listing.getPrice());

            db.collection("sales").add(sale).addOnSuccessListener(ref -> {
                db.collection("listings").document(listing.getId())
                        .update("status", Listing.STATUS_REQUESTED);
                binding.btnPrimary.setText("Request sent");
                Toast.makeText(this,
                        "Purchase request sent. Track it under Borrowing.",
                        Toast.LENGTH_LONG).show();
            }).addOnFailureListener(e -> {
                binding.btnPrimary.setEnabled(true);
                Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show();
            });
        });
    }

    /** Finds an existing chat thread for this item + pair of users, else creates one. */
    private void openChat() {
        if (me == null || listing == null) return;
        final String myId = me.getUid();

        db.collection("users").document(myId).get().addOnSuccessListener(userDoc -> {
            String myName = userDoc.getString("name");
            if (myName == null) myName = "Student";
            final String finalMyName = myName;

            db.collection("chats")
                    .whereArrayContains("participantIds", myId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            List<String> ids = (List<String>) doc.get("participantIds");
                            String title = doc.getString("listingTitle");
                            if (ids != null && ids.contains(listing.getOwnerId())
                                    && listing.getTitle().equals(title)) {
                                launchChat(doc.getId());
                                return;
                            }
                        }
                        createThread(myId, finalMyName);
                    });
        });
    }

    private void createThread(String myId, String myName) {
        ChatThread thread = new ChatThread();
        thread.setParticipantIds(Arrays.asList(myId, listing.getOwnerId()));
        Map<String, String> names = new HashMap<>();
        names.put(myId, myName);
        names.put(listing.getOwnerId(), listing.getOwnerName());
        thread.setParticipantNames(names);
        thread.setListingTitle(listing.getTitle());

        db.collection("chats").add(thread)
                .addOnSuccessListener(ref -> launchChat(ref.getId()));
    }

    private void launchChat(String threadId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_THREAD_ID, threadId);
        startActivity(intent);
    }
}
