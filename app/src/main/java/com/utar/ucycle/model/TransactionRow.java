package com.utar.ucycle.model;

import com.google.firebase.Timestamp;

/**
 * A borrow and a sale look the same in a list, so both collapse into this shape
 * for display. The type decides which detail screen a tap opens.
 */
public class TransactionRow {

    public static final String TYPE_BORROW = "BORROW";
    public static final String TYPE_SALE = "SALE";

    public final String id;
    public final String type;
    public final String title;
    public final String photoData;
    public final String status;
    public final String subtitle;
    public final Timestamp sortKey;

    public TransactionRow(String id, String type, String title, String photoData,
                          String status, String subtitle, Timestamp sortKey) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.photoData = photoData;
        this.status = status;
        this.subtitle = subtitle;
        this.sortKey = sortKey;
    }

    public static TransactionRow of(BorrowRecord record, boolean ownerView) {
        String subtitle = ownerView
                ? "Borrow request from " + record.getBorrowerName()
                : "Borrowing";
        return new TransactionRow(
                record.getId(), TYPE_BORROW, record.getListingTitle(),
                record.getListingPhotoData(), record.getDisplayStatus(),
                subtitle, record.getCreatedAt());
    }

    public static TransactionRow of(SaleRecord record, boolean sellerView) {
        String subtitle = sellerView
                ? "Purchase request from " + record.getBuyerName()
                : "Buying from " + record.getSellerName();
        return new TransactionRow(
                record.getId(), TYPE_SALE, record.getListingTitle(),
                record.getListingPhotoData(), record.getDisplayStatus(),
                subtitle, record.getCreatedAt());
    }
}
