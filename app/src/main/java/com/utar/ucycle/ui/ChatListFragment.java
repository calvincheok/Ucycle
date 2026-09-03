package com.utar.ucycle.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.utar.ucycle.ChatActivity;
import com.utar.ucycle.UserProfileActivity;
import com.utar.ucycle.adapter.ChatThreadAdapter;
import com.utar.ucycle.databinding.FragmentChatListBinding;
import com.utar.ucycle.model.ChatThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatListFragment extends Fragment {

    private FragmentChatListBinding binding;
    private ChatThreadAdapter adapter;
    private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return;

        final String myId = me.getUid();
        adapter = new ChatThreadAdapter(myId, new ChatThreadAdapter.OnClick() {
            @Override
            public void onThreadClick(ChatThread thread) {
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_THREAD_ID, thread.getId());
                startActivity(intent);
            }

            @Override
            public void onAvatarClick(ChatThread thread) {
                String otherId = null;
                for (String id : thread.getParticipantIds()) {
                    if (!id.equals(myId)) { otherId = id; break; }
                }
                if (otherId == null) return;
                Intent intent = new Intent(requireContext(), UserProfileActivity.class);
                intent.putExtra(UserProfileActivity.EXTRA_USER_ID, otherId);
                startActivity(intent);
            }
        });
        binding.recyclerChats.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerChats.setAdapter(adapter);

        // Array-contains only, sorted in the app, so no composite index is needed.
        registration = FirebaseFirestore.getInstance()
                .collection("chats")
                .whereArrayContains("participantIds", me.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (binding == null) return;
                    if (error != null) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        binding.tvEmpty.setText("Could not load chats.\n\n" + error.getMessage());
                        return;
                    }
                    if (snapshot == null) return;

                    List<ChatThread> threads = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ChatThread thread = doc.toObject(ChatThread.class);
                        thread.setId(doc.getId());
                        threads.add(thread);
                    }

                    Collections.sort(threads, (a, b) -> {
                        if (a.getLastMessageAt() == null || b.getLastMessageAt() == null) return 0;
                        return b.getLastMessageAt().compareTo(a.getLastMessageAt());
                    });

                    adapter.submit(threads);
                    binding.tvEmpty.setVisibility(threads.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.tvEmpty.setText("No chats yet. Start one from an item page!");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) registration.remove();
        binding = null;
    }
}
