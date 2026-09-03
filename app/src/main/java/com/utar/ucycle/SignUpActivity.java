package com.utar.ucycle;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.utar.ucycle.databinding.ActivitySignUpBinding;
import com.utar.ucycle.model.UserProfile;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        binding.tilEmail.setHint(Config.emailHint());

        binding.btnSignUp.setOnClickListener(v -> signUp());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void signUp() {
        String name = binding.etName.getText().toString().trim();
        String faculty = binding.etFaculty.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        if (name.isEmpty()) {
            binding.tilName.setError("Enter your name");
            return;
        }
        binding.tilName.setError(null);

        if (!Config.isEmailAllowed(email)) {
            binding.tilEmail.setError(Config.emailError());
            return;
        }
        binding.tilEmail.setError(null);

        if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            return;
        }
        binding.tilPassword.setError(null);

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "Sign up failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) { setLoading(false); return; }

                    UserProfile profile = new UserProfile(user.getUid(), name, email, faculty);
                    FirebaseFirestore.getInstance()
                            .collection("users").document(user.getUid())
                            .set(profile);

                    if (!Config.REQUIRE_EMAIL_VERIFICATION) {
                        setLoading(false);
                        binding.tvInfo.setVisibility(View.VISIBLE);
                        binding.tvInfo.setText("Account created. Go back and log in.");
                        auth.signOut();
                        return;
                    }

                    user.sendEmailVerification().addOnCompleteListener(t -> {
                        setLoading(false);
                        binding.tvInfo.setVisibility(View.VISIBLE);
                        binding.tvInfo.setText(
                                "Account created. We sent a verification link to " + email +
                                        ". Open it, then go back and log in.");
                        // signed-out state until verified, keeps login logic simple
                        auth.signOut();
                    });
                });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSignUp.setEnabled(!loading);
    }
}
