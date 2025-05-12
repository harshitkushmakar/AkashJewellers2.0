package com.kushmakar.akashjewellers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout; // Corrected from ImageView
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse; // Added import

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID; // Added import
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    EditText EnterName, EnterNumber;
    AppCompatButton GetOtp;
    ProgressBar progressBar;

    LinearLayout emailBtn;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    PhoneAuthProvider.ForceResendingToken resendToken;
    String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Initialize Firebase App Check ---
        // Keep the App Check initialization here
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());




        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        EnterName = findViewById(R.id.enter_name);
        EnterNumber = findViewById(R.id.phoneNumber);
        GetOtp = findViewById(R.id.getOtpButton);
        progressBar = findViewById(R.id.progressbarGetOtp);
        emailBtn = findViewById(R.id.email_btn); // Corrected findViewById

        emailBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, EmailLoginActivity.class);
            startActivity(intent);
        });

        GetOtp.setOnClickListener(view -> {
            String name = EnterName.getText().toString().trim();
            String mobileNumber = EnterNumber.getText().toString().trim();

            if (name.isEmpty()) {
                EnterName.setError("Name cannot be empty");
                EnterName.requestFocus();
                return;
            }

            if (mobileNumber.isEmpty()) {
                EnterNumber.setError("Mobile number cannot be empty");
                EnterNumber.requestFocus();
                return;
            } else if (mobileNumber.length() != 10) {
                EnterNumber.setError("Enter a valid 10-digit number");
                EnterNumber.requestFocus();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            GetOtp.setVisibility(View.INVISIBLE);

            // Call the new method to handle the integrity check and then send OTP
            prepareAndSendOTP(name, mobileNumber);
        });
    }


    private void prepareAndSendOTP(String name, String mobileNumber) {
        Log.d(TAG, "Starting Play Integrity check before sending OTP for: " + mobileNumber);

        // Create IntegrityManager
        IntegrityManager integrityManager = IntegrityManagerFactory.create(this);

        // Create a nonce (random string)
        String nonce = UUID.randomUUID().toString();
        Log.d(TAG, "Generated nonce: " + nonce);

        // Prepare the integrity token request
        IntegrityTokenRequest request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                // Use your cloud project number from the Firebase console
                .setCloudProjectNumber(335824851604L) // <-- Ensure this is your correct project number
                .build();

        // Request the integrity token
        integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener(new OnSuccessListener<IntegrityTokenResponse>() {
                    @Override
                    public void onSuccess(IntegrityTokenResponse integrityTokenResponse) {
                        Log.d(TAG, "Play Integrity Token retrieved successfully.");
                        // Get the integrity token
                        String integrityToken = integrityTokenResponse.token();

                        // --- Now, proceed with saving user data and initiating phone authentication ---

                        // Define callbacks here, specific to this verification attempt
                        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
                                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                    @Override
                                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                        Log.d(TAG, "Phone verification completed automatically.");
                                        progressBar.setVisibility(View.GONE);
                                        GetOtp.setVisibility(View.VISIBLE);
                                        signInWithPhoneAuthCredential(credential);
                                    }

                                    @Override
                                    public void onVerificationFailed(@NonNull FirebaseException e) {
                                        Log.e(TAG, "Phone verification failed", e);
                                        progressBar.setVisibility(View.GONE);
                                        GetOtp.setVisibility(View.VISIBLE);
                                        Toast.makeText(LoginActivity.this, "Phone verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        // Consider logging this failure to Crashlytics/Analytics
                                        // if you have them set up.
                                    }

                                    @Override
                                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                        Log.d(TAG, "OTP code sent successfully.");
                                        progressBar.setVisibility(View.GONE);
                                        GetOtp.setVisibility(View.VISIBLE);
                                        LoginActivity.this.verificationId = verificationId;
                                        LoginActivity.this.resendToken = token;

                                        Intent intent = new Intent(getApplicationContext(), OtpActivity.class);
                                        intent.putExtra("mobile", mobileNumber);
                                        intent.putExtra("verificationId", verificationId); // Pass the verification ID
                                        intent.putExtra("resendToken", token);
                                        startActivity(intent);
                                    }
                                };

                        // Now save user data BEFORE verifying phone number (as per your original flow)
                        saveNewUserData(name, mobileNumber);

                        // Initiate Phone Authentication with the integrity token
                        String phoneNumberE164 = "+91" + mobileNumber; // Ensure E.164 format
                        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth) // Use mAuth field
                                .setPhoneNumber(phoneNumberE164)
                                .setTimeout(60L, TimeUnit.SECONDS) // Timeout
                                .setActivity(LoginActivity.this) // Your activity
                                .setCallbacks(mCallbacks) // Callbacks
                                // Pass the integrity token obtained from Play Integrity
                          //      .setAppCheckToken(integrityToken) // <-- Pass the token here
                                .build();

                        Log.d(TAG, "Calling verifyPhoneNumber with App Check token.");
                        PhoneAuthProvider.verifyPhoneNumber(options);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle integrity API failure - prevent OTP and inform the user
                    Log.e(TAG, "Play Integrity API call failed: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    GetOtp.setVisibility(View.VISIBLE);
                    // Inform the user that a security check failed.
                    Toast.makeText(LoginActivity.this, "Security check failed. Cannot send OTP.", Toast.LENGTH_LONG).show();

                });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        Log.d(TAG, "Signing in with phone auth credential.");
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential successful.");
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        // Clear back stack on successful login
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish(); // Close LoginActivity
                    } else {
                        Log.e(TAG, "signInWithCredential failed: " + Objects.toString(task.getException(), "Unknown error"));
                        Toast.makeText(this, "Authentication failed: " + Objects.toString(task.getException() != null ? task.getException().getMessage() : "Unknown error", "Unknown error"), Toast.LENGTH_SHORT).show();
                        // Consider logging this failure to Crashlytics/Analytics
                    }
                });
    }


    private void saveNewUserData(String name, String phone) {
        Log.d(TAG, "Attempting to save new user data.");
        DatabaseReference usersRef = mDatabase.child("Phone");
        String userId = usersRef.push().getKey();
        long timestampMillis = System.currentTimeMillis();
        String readableTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestampMillis));

        if (userId != null) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("phone", phone);
            userData.put("createdTimestamp", timestampMillis);
            userData.put("createdReadable", readableTime);

            usersRef.child(userId).setValue(userData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User data saved successfully to: " + usersRef.child(userId).getKey());
                        // OTP verification is initiated AFTER getting integrity token, not here.
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save user data", e);
                        // Decide how critical this is for your flow.
                        Toast.makeText(this, "Warning: Failed to save initial user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Consider logging this failure
                    });
        } else {
            Log.e(TAG, "Failed to generate user ID for data save.");
            Toast.makeText(this, "Warning: Failed to generate user ID for initial data save.", Toast.LENGTH_SHORT).show();
            // Consider logging this failure
        }
    }
}