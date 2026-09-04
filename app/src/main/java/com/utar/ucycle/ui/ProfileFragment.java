package com.utar.ucycle.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.utar.ucycle.EditListingActivity;
import com.utar.ucycle.EditProfileActivity;
import com.utar.ucycle.ImageUtils;
import com.utar.ucycle.LoginActivity;
import com.utar.ucycle.databinding.FragmentProfileBinding;
import com.utar.ucycle.databinding.ItemMyListingBinding;
import com.utar.ucycle.model.BorrowRecord;
import com.utar.ucycle.model.Listing;
import com.utar.ucycle.model.UserProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        loadProfile();

        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        loadListings();
        loadImpact();

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadProfile() {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null || binding == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(me.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    UserProfile profile = doc.toObject(UserProfile.class);
                    if (profile == null) return;

                    binding.tvName.setText(profile.getName());
                    binding.tvFaculty.setText(profile.getFaculty());
                    binding.tvRating.setText(profile.getRatingCount() > 0
                            ? String.format(Locale.getDefault(), "%.1f", profile.getRating())
                            : "-");

                    // Optional picture, otherwise fall back to initials.
                    android.graphics.Bitmap photo = ImageUtils.decode(profile.getPhotoData());
                    if (photo != null) {
                        binding.ivAvatar.setImageBitmap(photo);
                        binding.tvAvatar.setVisibility(View.GONE);
                    } else {
                        binding.ivAvatar.setImageDrawable(null);
                        binding.tvAvatar.setVisibility(View.VISIBLE);
                        binding.tvAvatar.setText(profile.getInitials());
                    }

                    // Optional details are hidden when left blank.
                    String bio = profile.getBio();
                    binding.tvBio.setVisibility(bio == null || bio.isEmpty() ? View.GONE : View.VISIBLE);
                    binding.tvBio.setText(bio);

                    String contact = profile.getContact();
                    binding.tvContact.setVisibility(
                            contact == null || contact.isEmpty() ? View.GONE : View.VISIBLE);
                    binding.tvContact.setText(contact);
                });
    }

    private void loadListings() {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null || binding == null) return;

        FirebaseFirestore.getInstance()
                .collection("listings").whereEqualTo("ownerId", me.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (binding == null) return;
                    List<Listing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Listing listing = doc.toObject(Listing.class);
                        listing.setId(doc.getId());
                        listings.add(listing);
                    }
                    showListings(listings);
                    binding.tvListingCount.setText(String.valueOf(listings.size()));
                });
    }

    /**
     * Adds one row per listing straight into the container. Every listing is
     * rendered, however many there are, and tapping one opens the edit screen.
     */
    private void showListings(List<Listing> listings) {
        if (binding == null) return;

        binding.listingsContainer.removeAllViews();
        binding.tvNoListings.setVisibility(listings.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Listing listing : listings) {
            ItemMyListingBinding row =
                    ItemMyListingBinding.inflate(inflater, binding.listingsContainer, false);
            row.tvTitle.setText(listing.getTitle());
            row.tvMeta.setText(listing.getTypeLabel() + " - " + listing.getStatus());
            row.getRoot().setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), EditListingActivity.class);
                intent.putExtra(EditListingActivity.EXTRA_LISTING_ID, listing.getId());
                startActivity(intent);
            });
            binding.listingsContainer.addView(row.getRoot());
        }
    }

    /**
     * "Items saved from landfill" counts completed exchanges. Only a single
     * equality filter is sent to Firestore and the status is counted here, so
     * no composite index is required.
     */
    private void loadImpact() {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null || binding == null) return;

        FirebaseFirestore.getInstance()
                .collection("borrows").whereEqualTo("ownerId", me.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (binding == null) return;
                    int completed = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        if (BorrowRecord.RETURNED.equals(doc.getString("status"))) completed++;
                    }
                    binding.tvCompleted.setText(String.valueOf(completed));
                    binding.tvImpact.setText(completed + " items saved from landfill");
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Values may have changed on an edit screen, so pull them again.
        loadProfile();
        loadListings();
        loadImpact();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
