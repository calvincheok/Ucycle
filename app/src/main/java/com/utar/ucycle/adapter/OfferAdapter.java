package com.utar.ucycle.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utar.ucycle.ImageUtils;
import com.utar.ucycle.databinding.ItemOfferBinding;
import com.utar.ucycle.model.Listing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.VH> {

    public interface OnAccept { void onAccept(Listing offer); }

    private final List<Listing> items = new ArrayList<>();
    private final OnAccept callback;

    public OfferAdapter(OnAccept callback) {
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
        return new VH(ItemOfferBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        private final ItemOfferBinding binding;

        VH(ItemOfferBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Listing offer) {
            binding.tvTitle.setText(offer.getTitle());
            binding.tvOwner.setText(offer.getOwnerName());
            binding.tvCondition.setText("Condition: " + offer.getCondition() + "/10");

            if (offer.getPrice() != null) {
                binding.tvPrice.setVisibility(View.VISIBLE);
                binding.tvPrice.setText(String.format(Locale.getDefault(), "RM %.2f", offer.getPrice()));
            } else {
                binding.tvPrice.setVisibility(View.GONE);
            }

            String description = offer.getDescription();
            binding.tvDescription.setVisibility(
                    description == null || description.isEmpty() ? View.GONE : View.VISIBLE);
            binding.tvDescription.setText(description);

            Bitmap photo = ImageUtils.decode(offer.getPhotoData());
            if (photo != null) binding.ivPhoto.setImageBitmap(photo);
            else binding.ivPhoto.setImageDrawable(null);

            binding.btnAccept.setOnClickListener(v -> callback.onAccept(offer));
        }
    }
}
