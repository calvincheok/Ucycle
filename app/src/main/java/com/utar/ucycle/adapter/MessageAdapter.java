package com.utar.ucycle.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utar.ucycle.databinding.ItemMessageReceivedBinding;
import com.utar.ucycle.databinding.ItemMessageSentBinding;
import com.utar.ucycle.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> items = new ArrayList<>();
    private final String myId;

    public MessageAdapter(String myId) {
        this.myId = myId;
    }

    public void submit(List<ChatMessage> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return myId.equals(items.get(position).getSenderId()) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            return new SentVH(ItemMessageSentBinding.inflate(inflater, parent, false));
        }
        return new ReceivedVH(ItemMessageReceivedBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = items.get(position);
        if (holder instanceof SentVH) {
            ((SentVH) holder).binding.tvMessage.setText(message.getText());
        } else {
            ((ReceivedVH) holder).binding.tvMessage.setText(message.getText());
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class SentVH extends RecyclerView.ViewHolder {
        final ItemMessageSentBinding binding;
        SentVH(ItemMessageSentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ReceivedVH extends RecyclerView.ViewHolder {
        final ItemMessageReceivedBinding binding;
        ReceivedVH(ItemMessageReceivedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
