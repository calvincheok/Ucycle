package com.utar.ucycle.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class BorrowRecord {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    /** Borrower says they handed it back; waiting for the owner to confirm. */
    public static final String RETURN_PENDING = "RETURN_PENDING";
    public static final String RETURNED = "RETURNED";

    private String id = "";
    private String listingId = "";
    private String listingTitle = "";
    private String listingPhotoUrl = "";
    private String listingPhotoData = "";
    private String ownerId = "";
    private String borrowerId = "";
    private String borrowerName = "";
    private String status = PENDING;
    private Timestamp dueDate;
    /** When the owner actually confirmed the item was back. */
    private Timestamp returnedAt;
    /** True if the owner closed this without the borrower confirming. */
    private boolean forceCompleted = false;
    /** True once the due date has been changed after approval. */
    private boolean dueDateEdited = false;
    private boolean ownerRated = false;
    private boolean borrowerRated = false;
    private Timestamp createdAt = Timestamp.now();

    public BorrowRecord() { }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }
    public String getListingPhotoUrl() { return listingPhotoUrl; }
    public void setListingPhotoUrl(String listingPhotoUrl) { this.listingPhotoUrl = listingPhotoUrl; }
    public String getListingPhotoData() { return listingPhotoData; }
    public void setListingPhotoData(String listingPhotoData) { this.listingPhotoData = listingPhotoData; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getBorrowerId() { return borrowerId; }
    public void setBorrowerId(String borrowerId) { this.borrowerId = borrowerId; }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }
    public Timestamp getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Timestamp returnedAt) { this.returnedAt = returnedAt; }
    public boolean isForceCompleted() { return forceCompleted; }
    public void setForceCompleted(boolean forceCompleted) { this.forceCompleted = forceCompleted; }
    public boolean isDueDateEdited() { return dueDateEdited; }
    public void setDueDateEdited(boolean dueDateEdited) { this.dueDateEdited = dueDateEdited; }
    public boolean isOwnerRated() { return ownerRated; }
    public void setOwnerRated(boolean ownerRated) { this.ownerRated = ownerRated; }
    public boolean isBorrowerRated() { return borrowerRated; }
    public void setBorrowerRated(boolean borrowerRated) { this.borrowerRated = borrowerRated; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /** Days past the due date, or 0 when not overdue. */
    @Exclude
    public int getDaysOverdue() {
        if (dueDate == null) return 0;
        long diff = System.currentTimeMillis() - dueDate.toDate().getTime();
        if (diff <= 0) return 0;
        return (int) (diff / (24L * 60 * 60 * 1000));
    }

    /** True when the item came back after the agreed date. */
    @Exclude
    public boolean wasReturnedLate() {
        if (returnedAt == null || dueDate == null) return false;
        return returnedAt.toDate().after(dueDate.toDate());
    }

    /** Overdue / Due soon / Borrowed / Pending etc. for the status chip. */
    @Exclude
    public String getDisplayStatus() {
        if (!APPROVED.equals(status)) {
            if (PENDING.equals(status)) return "Pending";
            if (REJECTED.equals(status)) return "Rejected";
            if (RETURN_PENDING.equals(status)) return "Return pending";
            if (RETURNED.equals(status)) return forceCompleted ? "Closed by owner" : "Returned";
            return status;
        }
        if (dueDate == null) return "Borrowed";
        Date due = dueDate.toDate();
        long diff = due.getTime() - System.currentTimeMillis();
        if (diff < 0) return "Overdue";
        if (diff < 3L * 24 * 60 * 60 * 1000) return "Due Soon";
        return "On Time";
    }
}
