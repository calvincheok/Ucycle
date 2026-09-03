package com.utar.ucycle.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utar.ucycle.databinding.ItemMyListingBinding;
import com.utar.ucycle.model.Listing;

import java.util.ArrayList;
import java.util.List;

public class MyListingAdapter extends RecyclerView.Adapter<MyListingAdapter.VH> {

    public interface OnClick { void onListingClick(Listing listing); }

    private final List<Listing> items = new ArrayList<>();
    private final OnClick callback;

    public MyListingAdapter(OnClick callback) {
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
        return new VH(ItemMyListingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Listing listing = items.get(position);
        holder.binding.tvTitle.setText(listing.getTitle());
        holder.binding.tvMeta.setText(listing.getType() + " - " + listing.getStatus());
        holder.binding.getRoot().setOnClickListener(v -> callback.onListingClick(listing));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemMyListingBinding binding;
        VH(ItemMyListingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
