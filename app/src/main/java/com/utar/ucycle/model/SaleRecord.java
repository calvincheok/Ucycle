package com.utar.ucycle.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

/**
 * A purchase agreement between two students. Money never moves through the app;
 * they meet in person and each side confirms the deal actually happened, which
 * is what stops a no-show from counting as a completed sale.
 */
public class SaleRecord {

    public static final String PENDING = "PENDING";     // buyer asked, seller has not replied
    public static final String ACCEPTED = "ACCEPTED";   // seller agreed, meeting to be arranged
    public static final String COMPLETED = "COMPLETED"; // both sides confirmed the handover
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    private String id = "";
    private String listingId = "";
    private String listingTitle = "";
    private String listingPhotoData = "";
    private String sellerId = "";
    private String sellerName = "";
    private String buyerId = "";
    private String buyerName = "";
    private String status = PENDING;
    private Double price;

    private boolean buyerConfirmed = false;
    private boolean sellerConfirmed = false;
    private boolean buyerRated = false;
    private boolean sellerRated = false;

    private Timestamp completedAt;
    private Timestamp createdAt = Timestamp.now();

    public SaleRecord() { }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }
    public String getListingPhotoData() { return listingPhotoData; }
    public void setListingPhotoData(String listingPhotoData) { this.listingPhotoData = listingPhotoData; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public boolean isBuyerConfirmed() { return buyerConfirmed; }
    public void setBuyerConfirmed(boolean buyerConfirmed) { this.buyerConfirmed = buyerConfirmed; }
    public boolean isSellerConfirmed() { return sellerConfirmed; }
    public void setSellerConfirmed(boolean sellerConfirmed) { this.sellerConfirmed = sellerConfirmed; }
    public boolean isBuyerRated() { return buyerRated; }
    public void setBuyerRated(boolean buyerRated) { this.buyerRated = buyerRated; }
    public boolean isSellerRated() { return sellerRated; }
    public void setSellerRated(boolean sellerRated) { this.sellerRated = sellerRated; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Exclude
    public String getDisplayStatus() {
        switch (status) {
            case PENDING:   return "Pending";
            case ACCEPTED:  return waitingOn();
            case COMPLETED: return "Completed";
            case REJECTED:  return "Rejected";
            case CANCELLED: return "Cancelled";
            default:        return status;
        }
    }

    private String waitingOn() {
        if (buyerConfirmed && !sellerConfirmed) return "Awaiting seller";
        if (!buyerConfirmed && sellerConfirmed) return "Awaiting buyer";
        return "Accepted";
    }
}
