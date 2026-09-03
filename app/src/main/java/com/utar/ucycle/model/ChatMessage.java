package com.utar.ucycle.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class ChatMessage {
    private String id = "";
    private String senderId = "";
    private String text = "";
    private Timestamp sentAt = Timestamp.now();

    public ChatMessage() { }

    public ChatMessage(String senderId, String text) {
        this.senderId = senderId;
        this.text = text;
    }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Timestamp getSentAt() { return sentAt; }
    public void setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }
}
