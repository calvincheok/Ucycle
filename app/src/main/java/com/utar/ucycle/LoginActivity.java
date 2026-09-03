package com.utar.ucycle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.utar.ucycle.databinding.ActivityLoginBinding;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    /**
     * Kept for backwards compatibility with the rest of the code.
     * The actual rule lives in {@link Config} so it can be switched in one place.
     */
    public static boolean isUtarEmail(String email) {
        return Config.isEmailAllowed(email);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsHelper.applyAll(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        // already signed in and verified -> skip login
        FirebaseUser current = auth.getCurrentUser();
        if (current != null && (current.isEmailVerified() || !Config.REQUIRE_EMAIL_VERIFICATION)) {
            goToMain();
            return;
        }

        binding.tilEmail.setHint(Config.emailHint());

        binding.btnLogin.setOnClickListener(v -> login());
        binding.btnGoSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
        binding.btnResendVerification.setOnClickListener(v -> resendVerification());
    }

    private void login() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        if (!Config.isEmailAllowed(email)) {
            binding.tilEmail.setError(Config.emailError());
            return;
        }
        binding.tilEmail.setError(null);
        if (password.isEmpty()) {
            binding.tilPassword.setError("Enter your password");
            return;
        }
        binding.tilPassword.setError(null);

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "Login failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) { setLoading(false); return; }

                    user.reload().addOnCompleteListener(r -> {
                        setLoading(false);
                        FirebaseUser refreshed = auth.getCurrentUser();
                        boolean verified = refreshed != null
                                && (refreshed.isEmailVerified() || !Config.REQUIRE_EMAIL_VERIFICATION);
                        if (verified) {
                            saveFcmToken(refreshed.getUid());
                            goToMain();
                        } else {
                            binding.tvInfo.setVisibility(View.VISIBLE);
                            binding.tvInfo.setText(
                                    "Please verify your email first. Open the link we sent to your UTAR inbox, then log in again.");
                        }
                    });
                });
    }

    /**
     * Saves the notification token and, if the user document is missing (account
     * created before profiles existed, or the collection was cleared), rebuilds a
     * minimal one so the rest of the app always has something to read.
     */
    private void saveFcmToken(String uid) {
        FirebaseUser user = auth.getCurrentUser();

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        if (user != null && user.getEmail() != null) data.put("email", user.getEmail());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                // Fall back to the part of the email before the @ as a display name.
                String fallback = "Student";
                if (user != null && user.getEmail() != null) {
                    fallback = user.getEmail().split("@")[0];
                }
                data.put("name", fallback);
                data.put("faculty", "");
            }
            db.collection("users").document(uid).set(data, SetOptions.merge());
        });

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                db.collection("users").document(uid)
                        .set(Collections.singletonMap("fcmToken", (Object) token),
                                SetOptions.merge()));
    }

    private void resendVerification() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Log in first, then resend.", Toast.LENGTH_SHORT).show();
            return;
        }
        user.sendEmailVerification().addOnCompleteListener(t ->
                Toast.makeText(this,
                        t.isSuccessful() ? "Verification email sent" : "Could not send email",
                        Toast.LENGTH_SHORT).show());
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
