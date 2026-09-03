package com.utar.ucycle.model;

import com.google.firebase.firestore.Exclude;

public class UserProfile {
    private String uid = "";
    private String name = "";
    private String email = "";
    private String faculty = "";

    /** Optional profile picture, stored as a compressed Base64 JPEG. */
    private String photoData = "";
    /** Optional short "about me" line shown on the profile. */
    private String bio = "";
    /** Optional contact detail (phone / WhatsApp / Telegram handle). */
    private String contact = "";

    private double rating = 0;
    private int ratingCount = 0;
    private String fcmToken = "";

    public UserProfile() { }

    public UserProfile(String uid, String name, String email, String faculty) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.faculty = faculty;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }
    public String getPhotoData() { return photoData; }
    public void setPhotoData(String photoData) { this.photoData = photoData; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    @Exclude
    public String getInitials() {
        if (name == null || name.trim().isEmpty()) return "?";
        String t = name.trim();
        return t.length() >= 2 ? t.substring(0, 2).toUpperCase() : t.toUpperCase();
    }

    @Exclude
    public boolean hasPhoto() {
        return photoData != null && !photoData.isEmpty();
    }
}
