package com.utar.ucycle.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.utar.ucycle.ImageUtils;
import com.utar.ucycle.R;
import com.utar.ucycle.databinding.ItemTransactionBinding;
import com.utar.ucycle.model.TransactionRow;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {

    public interface OnClick { void onRowClick(TransactionRow row); }

    private final List<TransactionRow> items = new ArrayList<>();
    private final OnClick callback;

    public TransactionAdapter(OnClick callback) {
        this.callback = callback;
    }

    public void submit(List<TransactionRow> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        VH(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TransactionRow row) {
            binding.tvTitle.setText(row.title);
            binding.tvSubtitle.setText(row.subtitle);
            binding.tvStatus.setText(row.status);

            Bitmap photo = ImageUtils.decode(row.photoData);
            if (photo != null) binding.ivPhoto.setImageBitmap(photo);
            else binding.ivPhoto.setImageDrawable(null);

            int bg, fg;
            switch (row.status) {
                case "Overdue":
                    bg = R.color.danger_light; fg = R.color.danger; break;
                case "Due Soon":
                case "Return pending":
                case "Awaiting seller":
                case "Awaiting buyer":
                    bg = R.color.amber_light; fg = R.color.amber; break;
                case "Rejected":
                case "Cancelled":
                    bg = R.color.danger_light; fg = R.color.danger; break;
                default:
                    bg = R.color.ucycle_green_light; fg = R.color.ucycle_green_dark; break;
            }
            binding.tvStatus.setBackgroundTintList(
                    ContextCompat.getColorStateList(binding.getRoot().getContext(), bg));
            binding.tvStatus.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), fg));

            binding.getRoot().setOnClickListener(v -> callback.onRowClick(row));
        }
    }
}
