package com.utar.ucycle;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.utar.ucycle.databinding.ActivityMainBinding;
import com.utar.ucycle.ui.BorrowingFragment;
import com.utar.ucycle.ui.ChatListFragment;
import com.utar.ucycle.ui.HomeFragment;
import com.utar.ucycle.ui.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        if (savedInstanceState == null) {
            show(new HomeFragment());
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                show(new HomeFragment());
                return true;
            } else if (id == R.id.nav_post) {
                // Post opens its own screen, keep the previous tab selected
                startActivity(new Intent(this, CreateListingActivity.class));
                return false;
            } else if (id == R.id.nav_borrowing) {
                show(new BorrowingFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                show(new ProfileFragment());
                return true;
            } else if (id == R.id.nav_chat) {
                show(new ChatListFragment());
                return true;
            }
            return false;
        });
    }

    private void show(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
