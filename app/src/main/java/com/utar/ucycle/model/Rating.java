package com.utar.ucycle.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

/**
 * One person's rating of another after a completed borrow or sale.
 *
 * There is a single combined trust score per person: lending, borrowing,
 * buying and selling all feed the same number, so a profile shows one
 * reputation rather than several.
 */
public class Rating {

    private String id = "";
    /** The person being rated. */
    private String targetUserId = "";
    private String raterId = "";
    private String raterName = "";
    private int stars = 5;
    private String comment = "";
    /** The rated person may answer a comment once. */
    private String reply = "";
    private boolean reported = false;
    /** BORROW or SALE, shown as context next to the comment. */
    private String context = "BORROW";
    private Timestamp createdAt = Timestamp.now();

    public Rating() { }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public String getRaterId() { return raterId; }
    public void setRaterId(String raterId) { this.raterId = raterId; }
    public String getRaterName() { return raterName; }
    public void setRaterName(String raterName) { this.raterName = raterName; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public boolean isReported() { return reported; }
    public void setReported(boolean reported) { this.reported = reported; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Exclude
    public String getStarText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < stars ? "\u2605" : "\u2606");
        return sb.toString();
    }
}
