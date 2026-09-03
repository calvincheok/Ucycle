package com.utar.ucycle.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utar.ucycle.databinding.ItemChatThreadBinding;
import com.utar.ucycle.model.ChatThread;

import java.util.ArrayList;
import java.util.List;

public class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.VH> {

    public interface OnClick {
        void onThreadClick(ChatThread thread);
        /** Tapping the avatar opens the other person's profile. */
        void onAvatarClick(ChatThread thread);
    }

    private final List<ChatThread> items = new ArrayList<>();
    private final String myId;
    private final OnClick callback;

    public ChatThreadAdapter(String myId, OnClick callback) {
        this.myId = myId;
        this.callback = callback;
    }

    public void submit(List<ChatThread> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemChatThreadBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        private final ItemChatThreadBinding binding;

        VH(ItemChatThreadBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatThread thread) {
            String other = thread.getOtherName(myId);
            binding.tvName.setText(other);
            binding.tvListingTitle.setText(thread.getListingTitle());
            binding.tvLastMessage.setText(thread.getLastMessage());
            binding.tvAvatar.setText(other.isEmpty()
                    ? "?" : other.substring(0, 1).toUpperCase());
            binding.tvAvatar.setOnClickListener(v -> callback.onAvatarClick(thread));
            binding.getRoot().setOnClickListener(v -> callback.onThreadClick(thread));
        }
    }
}
