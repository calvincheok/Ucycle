package com.utar.ucycle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.utar.ucycle.databinding.ActivityTransactionDetailBinding;
import com.utar.ucycle.model.BorrowRecord;
import com.utar.ucycle.model.Listing;
import com.utar.ucycle.model.SaleRecord;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * One screen for both kinds of deal, because the two lifecycles show the same
 * things: the item, the other person, where the deal has got to, and whichever
 * buttons apply to your side of it.
 *
 * Borrow:  PENDING -> APPROVED -> RETURN_PENDING -> RETURNED
 * Sale:    PENDING -> ACCEPTED -> (both confirm) -> COMPLETED
 */
public class TransactionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "type";        // "BORROW" or "SALE"
    public static final String EXTRA_ID = "record_id";
    public static final String TYPE_BORROW = "BORROW";
    public static final String TYPE_SALE = "SALE";

    private ActivityTransactionDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser me;

    private String type;
    private String recordId;
    private BorrowRecord borrow;
    private SaleRecord sale;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransactionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        me = FirebaseAuth.getInstance().getCurrentUser();
        type = getIntent().getStringExtra(EXTRA_TYPE);
        recordId = getIntent().getStringExtra(EXTRA_ID);

        binding.btnBack.setOnClickListener(v -> finish());
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recordId != null) load();
    }

    private void load() {
        if (recordId == null || me == null) { finish(); return; }
        String collection = TYPE_SALE.equals(type) ? "sales" : "borrows";

        db.collection(collection).document(recordId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "This record no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    if (TYPE_SALE.equals(type)) {
                        sale = doc.toObject(SaleRecord.class);
                        if (sale != null) { sale.setId(doc.getId()); bindSale(); }
                    } else {
                        borrow = doc.toObject(BorrowRecord.class);
                        if (borrow != null) { borrow.setId(doc.getId()); bindBorrow(); }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    // ---------------------------------------------------------------- borrow

    private void bindBorrow() {
        boolean isOwner = me.getUid().equals(borrow.getOwnerId());

        binding.tvTitle.setText(borrow.getListingTitle());
        binding.tvKind.setText("Borrow");
        binding.tvStatus.setText(borrow.getDisplayStatus());
        binding.tvOtherParty.setText((isOwner
                ? "Borrower: " + borrow.getBorrowerName()
                : "Owner of this item") + "  \u203A");
        binding.tvOtherParty.setOnClickListener(v -> openUserProfile(
                isOwner ? borrow.getBorrowerId() : borrow.getOwnerId()));
        ImageUtils.loadIntoOrHide(binding.ivPhoto, borrow.getListingPhotoData());

        StringBuilder dates = new StringBuilder();
        if (borrow.getDueDate() != null) {
            dates.append("Due ").append(dateFormat.format(borrow.getDueDate().toDate()));
            if (borrow.isDueDateEdited()) dates.append("  (date was changed)");
        }
        if (borrow.getReturnedAt() != null) {
            dates.append("\nReturned ").append(dateFormat.format(borrow.getReturnedAt().toDate()));
            dates.append(borrow.wasReturnedLate() ? "  (late)" : "  (on time)");
        }
        binding.tvDates.setVisibility(dates.length() == 0 ? View.GONE : View.VISIBLE);
        binding.tvDates.setText(dates.toString());

        binding.btnPrimary.setVisibility(View.GONE);
        binding.btnSecondary.setVisibility(View.GONE);
        binding.btnTertiary.setVisibility(View.GONE);
        binding.tvNote.setVisibility(View.GONE);

        String status = borrow.getStatus();

        if (isOwner && BorrowRecord.PENDING.equals(status)) {
            show(binding.btnPrimary, "Approve request", v -> approveBorrow());
            show(binding.btnSecondary, "Reject", v -> updateBorrow(
                    map("status", BorrowRecord.REJECTED), Listing.STATUS_AVAILABLE));

        } else if (isOwner && BorrowRecord.APPROVED.equals(status)) {
            show(binding.btnPrimary, "Change due date", v -> pickDueDate(true));
            // Only offer to close it unilaterally once the borrower is properly late.
            if (borrow.getDaysOverdue() > Config.OVERDUE_GRACE_DAYS) {
                show(binding.btnSecondary, "Close without borrower confirmation",
                        v -> confirmForceComplete());
                note("This item is " + borrow.getDaysOverdue() + " day(s) overdue.");
            }

        } else if (!isOwner && BorrowRecord.APPROVED.equals(status)) {
            show(binding.btnPrimary, "I have returned this", v -> markReturnPending());

        } else if (isOwner && BorrowRecord.RETURN_PENDING.equals(status)) {
            show(binding.btnPrimary, "Confirm I got it back", v -> confirmReturn());
            note("The borrower says they have returned this item.");

        } else if (!isOwner && BorrowRecord.RETURN_PENDING.equals(status)) {
            note("Waiting for the owner to confirm they received it.");

        } else if (BorrowRecord.RETURNED.equals(status)) {
            boolean alreadyRated = isOwner ? borrow.isOwnerRated() : borrow.isBorrowerRated();
            if (alreadyRated) {
                note("You have already rated this exchange. Thanks!");
            } else {
                String target = isOwner ? borrow.getBorrowerId() : borrow.getOwnerId();
                show(binding.btnPrimary, isOwner ? "Rate the borrower" : "Rate the owner",
                        v -> openRating(target, TYPE_BORROW));
            }
        }

        show(binding.btnTertiary, "Open chat", v -> openChat(
                me.getUid().equals(borrow.getOwnerId())
                        ? borrow.getBorrowerId() : borrow.getOwnerId(),
                borrow.getListingTitle()));
    }

    private void approveBorrow() { pickDueDate(false); }

    /**
     * The two of them agree the length in chat, so the owner simply picks the
     * date here rather than the app imposing one.
     */
    private void pickDueDate(boolean isEdit) {
        Calendar cal = Calendar.getInstance();
        if (isEdit && borrow.getDueDate() != null) cal.setTime(borrow.getDueDate().toDate());
        else cal.add(Calendar.DAY_OF_YEAR, 7);

        new android.app.DatePickerDialog(this, (view, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day, 23, 59, 0);

            Map<String, Object> update = new HashMap<>();
            update.put("dueDate", new Timestamp(picked.getTime()));
            if (isEdit) {
                update.put("dueDateEdited", true);
                updateBorrow(update, null);
            } else {
                update.put("status", BorrowRecord.APPROVED);
                updateBorrow(update, Listing.STATUS_BORROWED);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void markReturnPending() {
        updateBorrow(map("status", BorrowRecord.RETURN_PENDING), null);
        Toast.makeText(this, "The owner has been asked to confirm.", Toast.LENGTH_LONG).show();
    }

    private void confirmReturn() {
        Map<String, Object> update = new HashMap<>();
        update.put("status", BorrowRecord.RETURNED);
        update.put("returnedAt", Timestamp.now());
        updateBorrow(update, Listing.STATUS_AVAILABLE);
    }

    private void confirmForceComplete() {
        new AlertDialog.Builder(this)
                .setTitle("Close this borrow?")
                .setMessage("Use this when the borrower has not confirmed the return. "
                        + "The item goes back to available and you can still rate them.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Close it", (d, w) -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("status", BorrowRecord.RETURNED);
                    update.put("returnedAt", Timestamp.now());
                    update.put("forceCompleted", true);
                    updateBorrow(update, Listing.STATUS_AVAILABLE);
                })
                .show();
    }

    private void updateBorrow(Map<String, Object> update, String listingStatus) {
        db.collection("borrows").document(borrow.getId()).update(update)
                .addOnSuccessListener(v -> {
                    if (listingStatus != null && !borrow.getListingId().isEmpty()) {
                        db.collection("listings").document(borrow.getListingId())
                                .update("status", listingStatus);
                    }
                    load();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Could not update: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ------------------------------------------------------------------ sale

    private void bindSale() {
        boolean isSeller = me.getUid().equals(sale.getSellerId());

        binding.tvTitle.setText(sale.getListingTitle());
        binding.tvKind.setText("Sale");
        binding.tvStatus.setText(sale.getDisplayStatus());
        binding.tvOtherParty.setText((isSeller
                ? "Buyer: " + sale.getBuyerName()
                : "Seller: " + sale.getSellerName()) + "  \u203A");
        binding.tvOtherParty.setOnClickListener(v -> openUserProfile(
                isSeller ? sale.getBuyerId() : sale.getSellerId()));
        ImageUtils.loadIntoOrHide(binding.ivPhoto, sale.getListingPhotoData());

        StringBuilder info = new StringBuilder();
        if (sale.getPrice() != null) {
            info.append(String.format(Locale.getDefault(), "Price RM %.2f", sale.getPrice()));
        }
        if (sale.getCompletedAt() != null) {
            info.append("\nCompleted ").append(dateFormat.format(sale.getCompletedAt().toDate()));
        }
        binding.tvDates.setVisibility(info.length() == 0 ? View.GONE : View.VISIBLE);
        binding.tvDates.setText(info.toString());

        binding.btnPrimary.setVisibility(View.GONE);
        binding.btnSecondary.setVisibility(View.GONE);
        binding.btnTertiary.setVisibility(View.GONE);
        binding.tvNote.setVisibility(View.GONE);

        String status = sale.getStatus();

        if (isSeller && SaleRecord.PENDING.equals(status)) {
            show(binding.btnPrimary, "Accept request", v -> updateSale(
                    map("status", SaleRecord.ACCEPTED), null));
            show(binding.btnSecondary, "Reject", v -> updateSale(
                    map("status", SaleRecord.REJECTED), Listing.STATUS_AVAILABLE));

        } else if (!isSeller && SaleRecord.PENDING.equals(status)) {
            note("Waiting for the seller to accept.");
            show(binding.btnSecondary, "Cancel request", v -> updateSale(
                    map("status", SaleRecord.CANCELLED), Listing.STATUS_AVAILABLE));

        } else if (SaleRecord.ACCEPTED.equals(status)) {
            boolean iConfirmed = isSeller ? sale.isSellerConfirmed() : sale.isBuyerConfirmed();
            if (iConfirmed) {
                note("You have confirmed. Waiting for the other person.");
            } else {
                note("Meet up and hand the item over, then both of you confirm here.");
                show(binding.btnPrimary, "Confirm the deal happened", v -> confirmSale(isSeller));
            }

        } else if (SaleRecord.COMPLETED.equals(status)) {
            boolean alreadyRated = isSeller ? sale.isSellerRated() : sale.isBuyerRated();
            if (alreadyRated) {
                note("You have already rated this deal. Thanks!");
            } else {
                String target = isSeller ? sale.getBuyerId() : sale.getSellerId();
                show(binding.btnPrimary, isSeller ? "Rate the buyer" : "Rate the seller",
                        v -> openRating(target, TYPE_SALE));
            }
        }

        show(binding.btnTertiary, "Open chat", v -> openChat(
                isSeller ? sale.getBuyerId() : sale.getSellerId(), sale.getListingTitle()));
    }

    /** Marks my side confirmed, and completes the sale once both sides have. */
    private void confirmSale(boolean isSeller) {
        Map<String, Object> update = new HashMap<>();
        update.put(isSeller ? "sellerConfirmed" : "buyerConfirmed", true);

        boolean otherAlready = isSeller ? sale.isBuyerConfirmed() : sale.isSellerConfirmed();
        String listingStatus = null;
        if (otherAlready) {
            update.put("status", SaleRecord.COMPLETED);
            update.put("completedAt", Timestamp.now());
            listingStatus = Listing.STATUS_SOLD;
        }
        updateSale(update, listingStatus);
    }

    private void updateSale(Map<String, Object> update, String listingStatus) {
        db.collection("sales").document(sale.getId()).update(update)
                .addOnSuccessListener(v -> {
                    if (listingStatus != null && !sale.getListingId().isEmpty()) {
                        db.collection("listings").document(sale.getListingId())
                                .update("status", listingStatus);
                    }
                    load();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Could not update: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ---------------------------------------------------------------- shared

    private void openRating(String targetUserId, String context) {
        Intent intent = new Intent(this, RateActivity.class);
        intent.putExtra(RateActivity.EXTRA_TARGET_USER_ID, targetUserId);
        intent.putExtra(RateActivity.EXTRA_CONTEXT, context);
        intent.putExtra(RateActivity.EXTRA_RECORD_TYPE, type);
        intent.putExtra(RateActivity.EXTRA_RECORD_ID, recordId);
        startActivity(intent);
    }

    private void openUserProfile(String userId) {
        if (userId == null || userId.isEmpty()) return;
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }

    private void openChat(String otherId, String title) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, otherId);
        intent.putExtra(ChatActivity.EXTRA_LISTING_TITLE, title);
        startActivity(intent);
    }

    private void show(com.google.android.material.button.MaterialButton button,
                      String text, View.OnClickListener listener) {
        button.setVisibility(View.VISIBLE);
        button.setText(text);
        button.setOnClickListener(listener);
    }

    private void note(String text) {
        binding.tvNote.setVisibility(View.VISIBLE);
        binding.tvNote.setText(text);
    }

    private Map<String, Object> map(String key, Object value) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, value);
        return m;
    }
}
