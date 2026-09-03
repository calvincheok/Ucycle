package com.utar.ucycle.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatThread {
    private String id = "";
    private List<String> participantIds = new ArrayList<>();
    private Map<String, String> participantNames = new HashMap<>();
    private String lastMessage = "";
    private Timestamp lastMessageAt = Timestamp.now();
    private String listingTitle = "";

    public ChatThread() { }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }
    public Map<String, String> getParticipantNames() { return participantNames; }
    public void setParticipantNames(Map<String, String> participantNames) { this.participantNames = participantNames; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public Timestamp getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Timestamp lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }

    @Exclude
    public String getOtherName(String myId) {
        if (participantNames == null) return "Student";
        for (Map.Entry<String, String> e : participantNames.entrySet()) {
            if (!e.getKey().equals(myId)) return e.getValue();
        }
        return "Student";
    }
}
