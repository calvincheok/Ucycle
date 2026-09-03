package com.utar.ucycle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.utar.ucycle.adapter.OfferAdapter;
import com.utar.ucycle.databinding.ActivityRequestOffersBinding;
import com.utar.ucycle.model.BorrowRecord;
import com.utar.ucycle.model.Listing;
import com.utar.ucycle.model.SaleRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * The offers people have made against one request.
 *
 * Several people may answer the same request, so nothing lands in the
 * requester's list automatically. They compare the offers here and accept one,
 * which starts a real borrow or sale and releases everyone else's item.
 */
public class RequestOffersActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "request_id";

    private ActivityRequestOffersBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser me;

    private String requestId;
    private Listing request;
    private OfferAdapter adapter;
    private final List<Listing> offers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRequestOffersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        me = FirebaseAuth.getInstance().getCurrentUser();
        requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new OfferAdapter(this::confirmAccept);
        binding.recyclerOffers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerOffers.setAdapter(adapter);

        load();
    }

    private void load() {
        if (requestId == null || me == null) { finish(); return; }

        db.collection("listings").document(requestId).get().addOnSuccessListener(doc -> {
            request = doc.toObject(Listing.class);
            if (request != null) {
                request.setId(doc.getId());
                binding.tvRequestTitle.setText(request.getTitle());
                binding.tvRequestMode.setText(request.getTypeLabel());
            }
            loadOffers();
        });
    }

    private void loadOffers() {
        // Single-field query, so no composite index is needed.
        db.collection("listings").whereEqualTo("offerForRequestId", requestId).get()
                .addOnSuccessListener(snapshot -> {
                    offers.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Listing offer = doc.toObject(Listing.class);
                        offer.setId(doc.getId());
                        // Only offers still waiting for a decision.
                        if (Listing.STATUS_RESERVED.equals(offer.getStatus())) {
                            offers.add(offer);
                        }
                    }
                    adapter.submit(offers);
                    binding.tvEmpty.setVisibility(offers.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                    binding.tvEmpty.setText("Could not load offers.\n\n" + e.getMessage());
                });
    }

    private void confirmAccept(Listing offer) {
        new AlertDialog.Builder(this)
                .setTitle("Accept this offer?")
                .setMessage("A deal will start with " + offer.getOwnerName()
                        + ". The other offers are withdrawn and your request closes.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Accept", (d, w) -> accept(offer))
                .show();
    }

    /** Turns the chosen offer into a normal borrow or sale record. */
    private void accept(Listing offer) {
        if (me == null || request == null) return;

        db.collection("users").document(me.getUid()).get().addOnSuccessListener(userDoc -> {
            String myName = userDoc.getString("name");
            if (myName == null) myName = "Student";

            if (Listing.MODE_BUY.equals(request.getRequestMode())) {
                SaleRecord sale = new SaleRecord();
                sale.setListingId(offer.getId());
                sale.setListingTitle(offer.getTitle());
                sale.setListingPhotoData(offer.getPhotoData());
                sale.setSellerId(offer.getOwnerId());
                sale.setSellerName(offer.getOwnerName());
                sale.setBuyerId(me.getUid());
                sale.setBuyerName(myName);
                sale.setPrice(offer.getPrice());
                // The requester picked it, so the seller's acceptance is implied.
                sale.setStatus(SaleRecord.ACCEPTED);
                db.collection("sales").add(sale).addOnSuccessListener(ref -> finishAccept(offer));

            } else {
                BorrowRecord borrow = new BorrowRecord();
                borrow.setListingId(offer.getId());
                borrow.setListingTitle(offer.getTitle());
                borrow.setListingPhotoData(offer.getPhotoData());
                borrow.setOwnerId(offer.getOwnerId());
                borrow.setBorrowerId(me.getUid());
                borrow.setBorrowerName(myName);
                // Owner still sets the due date when approving.
                borrow.setStatus(BorrowRecord.PENDING);
                db.collection("borrows").add(borrow).addOnSuccessListener(ref -> finishAccept(offer));
            }
        });
    }

    /** Marks the winning item as taken, frees the rest, and closes the request. */
    private void finishAccept(Listing accepted) {
        db.collection("listings").document(accepted.getId())
                .update("status", Listing.STATUS_REQUESTED);

        for (Listing other : offers) {
            if (!other.getId().equals(accepted.getId())) {
                // Withdrawn offers go back to being the owner's private item.
                db.collection("listings").document(other.getId())
                        .update("status", Listing.STATUS_CLOSED);
            }
        }

        db.collection("listings").document(requestId)
                .update("status", Listing.STATUS_CLOSED)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this,
                            "Offer accepted. Track it under Deals.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MainActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                });
    }
}
