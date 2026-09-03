package com.utar.ucycle.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.utar.ucycle.Ratings;
import com.utar.ucycle.databinding.ItemRatingBinding;
import com.utar.ucycle.model.Rating;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RatingAdapter extends RecyclerView.Adapter<RatingAdapter.VH> {

    public interface Actions {
        void onDelete(Rating rating);
        void onReply(Rating rating);
        void onReport(Rating rating);
    }

    private final List<Rating> items = new ArrayList<>();
    private final String myId;
    /** Whose profile this list belongs to, i.e. who may reply. */
    private final String profileOwnerId;
    private final Actions actions;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

    public RatingAdapter(String myId, String profileOwnerId, Actions actions) {
        this.myId = myId;
        this.profileOwnerId = profileOwnerId;
        this.actions = actions;
    }

    public void submit(List<Rating> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemRatingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        private final ItemRatingBinding binding;

        VH(ItemRatingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Rating rating) {
            binding.tvStars.setText(rating.getStarText());
            // Red / amber / green so a bad review is obvious at a glance.
            binding.tvStars.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(), Ratings.colorRes(rating.getStars())));
            binding.tvRater.setText(rating.getRaterName());
            binding.tvContext.setText("SALE".equals(rating.getContext()) ? "Sale" : "Borrow");

            if (rating.getCreatedAt() != null) {
                binding.tvDate.setText(dateFormat.format(rating.getCreatedAt().toDate()));
            } else {
                binding.tvDate.setText("");
            }

            String comment = rating.getComment();
            binding.tvComment.setVisibility(
                    comment == null || comment.isEmpty() ? View.GONE : View.VISIBLE);
            binding.tvComment.setText(comment);

            String reply = rating.getReply();
            boolean hasReply = reply != null && !reply.isEmpty();
            binding.tvReply.setVisibility(hasReply ? View.VISIBLE : View.GONE);
            binding.tvReply.setText("Reply: " + (hasReply ? reply : ""));

            binding.tvReported.setVisibility(rating.isReported() ? View.VISIBLE : View.GONE);

            // Only the author may delete their own comment.
            boolean mine = myId.equals(rating.getRaterId());
            binding.btnDelete.setVisibility(mine ? View.VISIBLE : View.GONE);
            binding.btnDelete.setOnClickListener(v -> actions.onDelete(rating));

            // Only the person being rated gets a right of reply.
            boolean iAmRated = myId.equals(profileOwnerId);
            binding.btnReply.setVisibility(iAmRated ? View.VISIBLE : View.GONE);
            binding.btnReply.setText(hasReply ? "Edit reply" : "Reply");
            binding.btnReply.setOnClickListener(v -> actions.onReply(rating));

            // Anyone other than the author can flag it.
            binding.btnReport.setVisibility(mine || rating.isReported() ? View.GONE : View.VISIBLE);
            binding.btnReport.setOnClickListener(v -> actions.onReport(rating));
        }
    }
}
