package com.utar.ucycle;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.utar.ucycle.adapter.MessageAdapter;
import com.utar.ucycle.databinding.ActivityChatBinding;
import com.utar.ucycle.model.ChatMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_THREAD_ID = "thread_id";
    /** Alternative entry: open (or start) the thread with this person. */
    public static final String EXTRA_OTHER_USER_ID = "other_user_id";
    public static final String EXTRA_LISTING_TITLE = "listing_title";

    private ActivityChatBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser me;
    private String threadId;
    /** The person on the other side, so their profile can be opened. */
    private String otherUserId;
    private MessageAdapter adapter;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        me = FirebaseAuth.getInstance().getCurrentUser();
        threadId = getIntent().getStringExtra(EXTRA_THREAD_ID);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.chatHeader.setOnClickListener(v -> openOtherProfile());

        adapter = new MessageAdapter(me == null ? "" : me.getUid());
        binding.recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMessages.setAdapter(adapter);

        binding.btnSend.setOnClickListener(v -> send());

        if (threadId != null) {
            start();
        } else {
            // Opened from a transaction screen, which knows the person but not
            // the thread, so find the existing conversation or create one.
            resolveThreadWith(
                    getIntent().getStringExtra(EXTRA_OTHER_USER_ID),
                    getIntent().getStringExtra(EXTRA_LISTING_TITLE));
        }
    }

    private void start() {
        loadThreadTitle();
        listenForMessages();
    }

    private void resolveThreadWith(String otherId, String listingTitle) {
        if (otherId == null || me == null) { finish(); return; }
        otherUserId = otherId;
        final String title = listingTitle == null ? "" : listingTitle;

        db.collection("users").document(me.getUid()).get().addOnSuccessListener(myDoc -> {
            String myName = myDoc.getString("name");
            if (myName == null) myName = "Student";
            final String finalMyName = myName;

            db.collection("chats")
                    .whereArrayContains("participantIds", me.getUid())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            List<String> ids = (List<String>) doc.get("participantIds");
                            if (ids != null && ids.contains(otherId)
                                    && title.equals(doc.getString("listingTitle"))) {
                                threadId = doc.getId();
                                start();
                                return;
                            }
                        }
                        createThread(otherId, finalMyName, title);
                    });
        });
    }

    private void createThread(String otherId, String myName, String title) {
        db.collection("users").document(otherId).get().addOnSuccessListener(otherDoc -> {
            String otherName = otherDoc.getString("name");
            if (otherName == null) otherName = "Student";

            Map<String, Object> thread = new HashMap<>();
            thread.put("participantIds", Arrays.asList(me.getUid(), otherId));
            Map<String, String> names = new HashMap<>();
            names.put(me.getUid(), myName);
            names.put(otherId, otherName);
            thread.put("participantNames", names);
            thread.put("listingTitle", title);
            thread.put("lastMessage", "");
            thread.put("lastMessageAt", Timestamp.now());

            db.collection("chats").add(thread).addOnSuccessListener(ref -> {
                threadId = ref.getId();
                start();
            });
        });
    }

    private void loadThreadTitle() {
        if (threadId == null) return;
        db.collection("chats").document(threadId).get().addOnSuccessListener(doc -> {
            String listingTitle = doc.getString("listingTitle");
            Map<String, Object> names = (Map<String, Object>) doc.get("participantNames");
            String other = "Chat";
            if (names != null && me != null) {
                for (Map.Entry<String, Object> e : names.entrySet()) {
                    if (!e.getKey().equals(me.getUid())) {
                        other = String.valueOf(e.getValue());
                        otherUserId = e.getKey();
                        break;
                    }
                }
            }
            binding.tvChatTitle.setText(other + "  \u203A");
            binding.tvChatSubtitle.setText(listingTitle == null ? "" : listingTitle);
        });
    }

    private void listenForMessages() {
        if (threadId == null) return;
        registration = db.collection("chats").document(threadId)
                .collection("messages")
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    List<ChatMessage> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        msg.setId(doc.getId());
                        messages.add(msg);
                    }
                    adapter.submit(messages);
                    if (!messages.isEmpty()) {
                        binding.recyclerMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    /** Lets you check who you are talking to before agreeing to anything. */
    private void openOtherProfile() {
        if (otherUserId == null || otherUserId.isEmpty()) return;
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_USER_ID, otherUserId);
        startActivity(intent);
    }

    private void send() {
        String text = binding.etMessage.getText().toString().trim();
        if (text.isEmpty() || me == null || threadId == null) return;
        binding.etMessage.setText("");

        ChatMessage message = new ChatMessage(me.getUid(), text);
        db.collection("chats").document(threadId)
                .collection("messages").add(message);

        Map<String, Object> update = new HashMap<>();
        update.put("lastMessage", text);
        update.put("lastMessageAt", Timestamp.now());
        db.collection("chats").document(threadId).update(update);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
