package com.utar.ucycle.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.utar.ucycle.ItemDetailActivity;
import com.utar.ucycle.UserProfileActivity;
import com.utar.ucycle.adapter.ListingAdapter;
import com.utar.ucycle.databinding.FragmentHomeBinding;
import com.utar.ucycle.model.Listing;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ListingAdapter adapter;
    private ListenerRegistration registration;

    private final List<Listing> allListings = new ArrayList<>();
    private String typeFilter = "ALL";
    private String query = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new ListingAdapter(new ListingAdapter.OnClick() {
            @Override
            public void onListingClick(Listing listing) {
                Intent intent = new Intent(requireContext(), ItemDetailActivity.class);
                intent.putExtra(ItemDetailActivity.EXTRA_LISTING_ID, listing.getId());
                startActivity(intent);
            }

            @Override
            public void onOwnerClick(Listing listing) {
                Intent intent = new Intent(requireContext(), UserProfileActivity.class);
                intent.putExtra(UserProfileActivity.EXTRA_USER_ID, listing.getOwnerId());
                startActivity(intent);
            }
        });
        binding.recyclerListings.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerListings.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase();
                applyFilters();
            }
        });

        binding.chipAll.setOnClickListener(v -> { typeFilter = "ALL"; applyFilters(); });
        binding.chipSell.setOnClickListener(v -> { typeFilter = Listing.TYPE_SELL; applyFilters(); });
        binding.chipBorrow.setOnClickListener(v -> { typeFilter = Listing.TYPE_BORROW; applyFilters(); });
        binding.chipRequest.setOnClickListener(v -> { typeFilter = Listing.TYPE_REQUEST; applyFilters(); });

        listenForListings();
    }

    /**
     * Real-time feed: any listing posted by anyone appears immediately.
     *
     * Deliberately a single-field query (order by createdAt only). Combining a
     * whereEqualTo with an orderBy would need a composite index, which fails
     * silently on a fresh Firebase project until someone creates it by hand.
     * The status filter is applied in applyFilters() instead.
     */
    private void listenForListings() {
        registration = FirebaseFirestore.getInstance()
                .collection("listings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (binding == null) return;
                    if (error != null) {
                        showError(error.getMessage());
                        return;
                    }
                    if (snapshot == null) return;
                    allListings.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Listing listing = doc.toObject(Listing.class);
                        listing.setId(doc.getId());
                        allListings.add(listing);
                    }
                    applyFilters();
                });
    }

    /** Never leave the user staring at a blank screen when a query fails. */
    private void showError(String message) {
        if (binding == null) return;
        binding.tvEmpty.setVisibility(View.VISIBLE);
        binding.tvEmpty.setText("Could not load listings.\n\n" + message);
    }

    private void applyFilters() {
        if (binding == null) return;
        List<Listing> filtered = new ArrayList<>();
        for (Listing l : allListings) {
            // only items still up for grabs
            if (!Listing.STATUS_AVAILABLE.equals(l.getStatus())) continue;
            // offers belong to one person's request, never the public feed
            if (l.isOffer()) continue;

            boolean typeOk;
            if (Listing.TYPE_SELL.equals(typeFilter)) typeOk = l.isSellable();
            else if (Listing.TYPE_BORROW.equals(typeFilter)) typeOk = l.isBorrowable();
            else if (Listing.TYPE_REQUEST.equals(typeFilter)) typeOk = l.isRequest();
            else typeOk = true;

            boolean queryOk = query.isEmpty()
                    || l.getTitle().toLowerCase().contains(query)
                    || l.getCategory().toLowerCase().contains(query);

            if (typeOk && queryOk) filtered.add(l);
        }
        adapter.submit(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvEmpty.setText("No listings yet. Be the first to post!");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) registration.remove();
        binding = null;
    }
}
