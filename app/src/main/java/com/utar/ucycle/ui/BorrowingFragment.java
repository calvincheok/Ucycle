package com.utar.ucycle.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.utar.ucycle.TransactionDetailActivity;
import com.utar.ucycle.adapter.TransactionAdapter;
import com.utar.ucycle.databinding.FragmentBorrowingBinding;
import com.utar.ucycle.model.BorrowRecord;
import com.utar.ucycle.model.SaleRecord;
import com.utar.ucycle.model.TransactionRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything the user is involved in, borrowing and buying alike.
 *
 * Tab 0 = deals where I am the borrower or buyer.
 * Tab 1 = deals on my own items, where I am the owner or seller.
 *
 * Each query filters on a single field and the results are merged and sorted
 * here, so Firestore never needs a composite index.
 */
public class BorrowingFragment extends Fragment {

    private FragmentBorrowingBinding binding;
    private TransactionAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseUser me;

    private int tab = 0;

    private final List<TransactionRow> borrowRows = new ArrayList<>();
    private final List<TransactionRow> saleRows = new ArrayList<>();

    /**
     * Guards against duplicated rows. Two queries run per refresh and each one
     * answers whenever it likes, so a refresh triggered while an earlier pair is
     * still in flight used to let the old answers append a second copy. Every
     * refresh takes the next token and late replies carrying an old one are
     * thrown away.
     */
    private int loadToken = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBorrowingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        me = FirebaseAuth.getInstance().getCurrentUser();

        adapter = new TransactionAdapter(row -> {
            Intent intent = new Intent(requireContext(), TransactionDetailActivity.class);
            intent.putExtra(TransactionDetailActivity.EXTRA_TYPE, row.type);
            intent.putExtra(TransactionDetailActivity.EXTRA_ID, row.id);
            startActivity(intent);
        });
        binding.recyclerBorrows.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBorrows.setAdapter(adapter);

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) { tab = t.getPosition(); reload(); }
            @Override public void onTabUnselected(TabLayout.Tab t) { }
            @Override public void onTabReselected(TabLayout.Tab t) { }
        });

        reload();
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        if (me == null || binding == null) return;

        final int token = ++loadToken;
        borrowRows.clear();
        saleRows.clear();

        boolean ownerView = tab == 1;
        String borrowField = ownerView ? "ownerId" : "borrowerId";
        String saleField = ownerView ? "sellerId" : "buyerId";

        db.collection("borrows").whereEqualTo(borrowField, me.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (binding == null || token != loadToken) return;
                    // Rebuild this half outright rather than appending to it.
                    borrowRows.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        BorrowRecord record = doc.toObject(BorrowRecord.class);
                        record.setId(doc.getId());
                        borrowRows.add(TransactionRow.of(record, ownerView));
                    }
                    publish();
                })
                .addOnFailureListener(e -> { if (token == loadToken) showError(e); });

        db.collection("sales").whereEqualTo(saleField, me.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (binding == null || token != loadToken) return;
                    saleRows.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        SaleRecord record = doc.toObject(SaleRecord.class);
                        record.setId(doc.getId());
                        saleRows.add(TransactionRow.of(record, ownerView));
                    }
                    publish();
                })
                .addOnFailureListener(e -> { if (token == loadToken) showError(e); });
    }

    /** Merges both sources, newest first. */
    private void publish() {
        if (binding == null) return;

        List<TransactionRow> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (TransactionRow row : borrowRows) {
            if (seen.add(row.type + ":" + row.id)) all.add(row);
        }
        for (TransactionRow row : saleRows) {
            if (seen.add(row.type + ":" + row.id)) all.add(row);
        }
        Collections.sort(all, (a, b) -> {
            if (a.sortKey == null || b.sortKey == null) return 0;
            return b.sortKey.compareTo(a.sortKey);
        });

        adapter.submit(all);
        binding.tvEmpty.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvEmpty.setText(tab == 0
                ? "Nothing yet. Ask to borrow or buy something from the home page."
                : "No requests on your items yet.");
    }

    private void showError(Exception e) {
        if (binding == null) return;
        binding.tvEmpty.setVisibility(View.VISIBLE);
        binding.tvEmpty.setText("Could not load this list.\n\n" + e.getMessage());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
