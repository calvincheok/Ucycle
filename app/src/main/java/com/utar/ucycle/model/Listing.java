package com.utar.ucycle.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

public class Listing implements Serializable {

    public static final String TYPE_SELL = "SELL";
    public static final String TYPE_BORROW = "BORROW";
    /** A "does anyone have this" post, made by someone looking for an item. */
    public static final String TYPE_REQUEST = "REQUEST";

    /** For TYPE_REQUEST: what the requester actually wants to do. */
    public static final String MODE_BORROW = "BORROW";
    public static final String MODE_BUY = "BUY";

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_BORROWED = "BORROWED";
    public static final String STATUS_SOLD = "SOLD";
    /** An item offered in reply to a request, waiting for the requester to decide. */
    public static final String STATUS_RESERVED = "RESERVED";
    /** A request that has been fulfilled or cancelled. */
    public static final String STATUS_CLOSED = "CLOSED";

    private String id = "";
    private String ownerId = "";
    private String ownerName = "";
    private String title = "";
    private String description = "";
    private String category = "";
    private int condition = 8;
    private String type = TYPE_SELL;
    private Double price;
    private Integer maxBorrowDays;
    /** Legacy field from the Firebase Storage version. Kept so old documents still load. */
    private String photoUrl = "";
    /** Compressed Base64 JPEG. Stored here because Cloud Storage now needs billing. */
    private String photoData = "";
    private String status = STATUS_AVAILABLE;
    /** Only meaningful on a TYPE_REQUEST post: MODE_BORROW or MODE_BUY. */
    private String requestMode = MODE_BORROW;
    /** Set when this item was posted as an offer against someone's request. */
    private String offerForRequestId = "";
    private Timestamp createdAt = Timestamp.now();

    public Listing() { }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getCondition() { return condition; }
    public void setCondition(int condition) { this.condition = condition; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getMaxBorrowDays() { return maxBorrowDays; }
    public void setMaxBorrowDays(Integer maxBorrowDays) { this.maxBorrowDays = maxBorrowDays; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getPhotoData() { return photoData; }
    public void setPhotoData(String photoData) { this.photoData = photoData; }

    @Exclude
    public boolean hasPhoto() {
        return photoData != null && !photoData.isEmpty();
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRequestMode() { return requestMode; }
    public void setRequestMode(String requestMode) { this.requestMode = requestMode; }
    public String getOfferForRequestId() { return offerForRequestId; }
    public void setOfferForRequestId(String offerForRequestId) { this.offerForRequestId = offerForRequestId; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Exclude
    public boolean isBorrowable() { return TYPE_BORROW.equals(type); }

    @Exclude
    public boolean isSellable() { return TYPE_SELL.equals(type); }

    @Exclude
    public boolean isRequest() { return TYPE_REQUEST.equals(type); }

    /** Offers live only inside the request they answer, never on the home feed. */
    @Exclude
    public boolean isOffer() {
        return offerForRequestId != null && !offerForRequestId.isEmpty();
    }

    @Exclude
    public String getTypeLabel() {
        if (isRequest()) {
            return MODE_BUY.equals(requestMode) ? "Wants to buy" : "Wants to borrow";
        }
        return TYPE_BORROW.equals(type) ? "Borrow" : "Sell";
    }
}
