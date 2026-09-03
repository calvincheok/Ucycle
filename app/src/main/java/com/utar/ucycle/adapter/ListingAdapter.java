package com.utar.ucycle.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;

import com.utar.ucycle.ImageUtils;
import com.utar.ucycle.databinding.ItemListingBinding;
import com.utar.ucycle.model.Listing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.VH> {

    public interface OnClick {
        void onListingClick(Listing listing);
        /** Tapping the poster's name opens their public profile instead. */
        void onOwnerClick(Listing listing);
    }

    private final List<Listing> items = new ArrayList<>();
    private final OnClick callback;

    public ListingAdapter(OnClick callback) {
        this.callback = callback;
    }

    public void submit(List<Listing> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListingBinding binding = ItemListingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        private final ItemListingBinding binding;

        VH(ItemListingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Listing listing) {
            binding.tvTitle.setText(listing.getTitle());
            // A request has no item yet, so condition and price do not apply.
            binding.chipCondition.setVisibility(listing.isRequest()
                    ? android.view.View.GONE : android.view.View.VISIBLE);
            binding.chipCondition.setText("Condition: " + listing.getCondition() + "/10");
            binding.chipType.setText(listing.getTypeLabel());

            if (listing.getPrice() != null && !listing.isRequest()) {
                binding.tvPrice.setVisibility(View.VISIBLE);
                binding.tvPrice.setText(String.format(Locale.getDefault(), "RM %.0f", listing.getPrice()));
            } else {
                binding.tvPrice.setVisibility(View.GONE);
            }

            Bitmap photo = ImageUtils.decode(listing.getPhotoData());
            if (photo != null) {
                binding.ivPhoto.setImageBitmap(photo);
            } else {
                binding.ivPhoto.setImageDrawable(null);
            }

            binding.tvOwner.setText(listing.getOwnerName() + "  \u203A");
            binding.tvOwner.setOnClickListener(v -> callback.onOwnerClick(listing));

            binding.getRoot().setOnClickListener(v -> callback.onListingClick(listing));
        }
    }
}
