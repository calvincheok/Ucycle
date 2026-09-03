package com.utar.ucycle.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;

import com.utar.ucycle.ImageUtils;
import com.utar.ucycle.R;
import com.utar.ucycle.databinding.ItemBorrowBinding;
import com.utar.ucycle.model.BorrowRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BorrowAdapter extends RecyclerView.Adapter<BorrowAdapter.VH> {

    public interface Actions {
        void onApprove(BorrowRecord record);
        void onReject(BorrowRecord record);
        void onMarkReturned(BorrowRecord record);
    }

    private final List<BorrowRecord> items = new ArrayList<>();
    private final Actions actions;
    private boolean ownerView = false;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

    public BorrowAdapter(Actions actions) {
        this.actions = actions;
    }

    public void setOwnerView(boolean ownerView) {
        this.ownerView = ownerView;
    }

    public void submit(List<BorrowRecord> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBorrowBinding binding = ItemBorrowBinding.inflate(
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
        private final ItemBorrowBinding binding;

        VH(ItemBorrowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BorrowRecord record) {
            binding.tvTitle.setText(record.getListingTitle());

            if (record.getDueDate() != null) {
                binding.tvDue.setVisibility(View.VISIBLE);
                binding.tvDue.setText("Due " + dateFormat.format(record.getDueDate().toDate()));
            } else {
                binding.tvDue.setVisibility(View.GONE);
            }

            if (ownerView) {
                binding.tvRequester.setVisibility(View.VISIBLE);
                binding.tvRequester.setText("Requested by " + record.getBorrowerName());
            } else {
                binding.tvRequester.setVisibility(View.GONE);
            }

            String status = record.getDisplayStatus();
            binding.tvStatus.setText(status);

            int bg, fg;
            switch (status) {
                case "Overdue":
                    bg = R.color.danger_light; fg = R.color.danger; break;
                case "Due Soon":
                    bg = R.color.amber_light; fg = R.color.amber; break;
                case "On Time":
                case "Returned":
                    bg = R.color.ucycle_green_light; fg = R.color.ucycle_green_dark; break;
                default:
                    bg = R.color.ucycle_green_light; fg = R.color.text_secondary; break;
            }
            binding.tvStatus.setBackgroundTintList(
                    ContextCompat.getColorStateList(binding.getRoot().getContext(), bg));
            binding.tvStatus.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), fg));

            Bitmap photo = ImageUtils.decode(record.getListingPhotoData());
            if (photo != null) {
                binding.ivPhoto.setImageBitmap(photo);
            } else {
                binding.ivPhoto.setImageDrawable(null);
            }

            // Owner action buttons
            boolean pending = BorrowRecord.PENDING.equals(record.getStatus());
            boolean approved = BorrowRecord.APPROVED.equals(record.getStatus());

            binding.layoutApproveReject.setVisibility(
                    ownerView && pending ? View.VISIBLE : View.GONE);
            binding.btnReturned.setVisibility(
                    ownerView && approved ? View.VISIBLE : View.GONE);

            binding.btnApprove.setOnClickListener(v -> actions.onApprove(record));
            binding.btnReject.setOnClickListener(v -> actions.onReject(record));
            binding.btnReturned.setOnClickListener(v -> actions.onMarkReturned(record));
        }
    }
}
