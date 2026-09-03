package com.utar.ucycle;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

/**
 * The category picker, shared by the create and edit screens.
 *
 * A fixed list keeps categories consistent so search and filtering actually
 * work, but campus items do not always fit a tidy list, so the final "Others"
 * entry reveals a free text field and whatever is typed there is stored as the
 * category.
 */
public final class CategoryPicker {

    private CategoryPicker() { }

    public static List<String> options(Context context) {
        return Arrays.asList(context.getResources().getStringArray(R.array.categories));
    }

    public static String othersLabel(Context context) {
        return context.getString(R.string.category_others);
    }

    /**
     * Fills the dropdown and shows or hides the free text field as the choice
     * changes.
     *
     * @param existing a category already saved on the listing, or null when new.
     */
    public static void attach(Context context,
                              MaterialAutoCompleteTextView dropdown,
                              TextInputLayout customLayout,
                              TextInputEditText customField,
                              String existing) {

        List<String> options = options(context);
        String others = othersLabel(context);

        dropdown.setAdapter(new ArrayAdapter<>(
                context, android.R.layout.simple_list_item_1, options));

        dropdown.setOnItemClickListener((parent, view, position, id) -> {
            boolean isOthers = others.equals(options.get(position));
            customLayout.setVisibility(isOthers ? View.VISIBLE : View.GONE);
            if (isOthers) customField.requestFocus();
        });

        if (existing == null || existing.trim().isEmpty()) {
            customLayout.setVisibility(View.GONE);
            return;
        }

        // Editing: a saved value that is not in the list must have been typed by
        // hand, so reopen it under "Others" rather than silently losing it.
        if (options.contains(existing)) {
            dropdown.setText(existing, false);
            customLayout.setVisibility(View.GONE);
        } else {
            dropdown.setText(others, false);
            customField.setText(existing);
            customLayout.setVisibility(View.VISIBLE);
        }
    }

    /** The category to save: the typed one when "Others" is chosen. */
    public static String resolve(Context context,
                                 MaterialAutoCompleteTextView dropdown,
                                 TextInputEditText customField) {
        String chosen = dropdown.getText().toString().trim();
        if (othersLabel(context).equals(chosen)) {
            return customField.getText() == null ? "" : customField.getText().toString().trim();
        }
        return chosen;
    }
}
