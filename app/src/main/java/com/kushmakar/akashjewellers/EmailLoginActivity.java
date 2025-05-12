package com.kushmakar.akashjewellers;

import static com.google.firebase.appcheck.internal.util.Logger.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EmailLoginActivity extends AppCompatActivity {

    // UI Elements
    TextView createNewAccount;
    AppCompatButton Login;
    EditText LoginEmail;
    EditText LoginPassword;
    TextView ForgotPassword;
    CheckBox showPasswordCheckBox;
    CheckBox staySignedInCheckBox;

    // SharedPreferences for "Stay Signed In"
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String PREF_STAY_SIGNED_IN = "staySignedIn";
    private static final String PREF_USER_TYPE = "userType"; // "admin" or "user"
    // Changed identifier to store UID for regular users
    private static final String PREF_USER_IDENTIFIER = "userIdentifier";
    private SharedPreferences sharedPreferences;

    // Define constants for user types and admin identifier
    private static final String ADMIN_EMAIL_IDENTIFIER = "admin"; // Or your specific admin email
    private static final String USER_TYPE_ADMIN = "admin";
    private static final String USER_TYPE_REGULAR = "user";

    // Declare FirebaseAuth
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // --- Check if user should be automatically logged in ---
        if (checkStaySignedInPreference()) {
            // If true, the appropriate dashboard was started, so finish this activity
            return; // Exit onCreate early
        }
        // --- End Auto-Login Check ---

        // Set the layout only if not auto-logging in
        setContentView(R.layout.activity_email_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find Views
        createNewAccount = findViewById(R.id.New_account);
        Login = findViewById(R.id.login);
        LoginEmail = findViewById(R.id.Login_email);
        ForgotPassword = findViewById(R.id.forgot_password);
        LoginPassword = findViewById(R.id.Login_password_text);
        showPasswordCheckBox = findViewById(R.id.checkbox_show_password);
        staySignedInCheckBox = findViewById(R.id.checkbox_stay_signed_in);

        // Listener for Show Password CheckBox
        showPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    LoginPassword.setTransformationMethod(null); // Show password
                } else {
                    LoginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance()); // Hide password
                }
                LoginPassword.setSelection(LoginPassword.getText().length()); // Move cursor to the end
            }
        });

        // Listener for Login Button
        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Validate both email and password before proceeding
                if (validateEmail() && validatePassword()) {
                    checkUserOrAdmin(); // Check if admin or regular user
                }
            }
        });

        ForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EmailLoginActivity.this);
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot, null); // Corrected second argument from 0 to null

                // Assuming you have an EditText with this ID in dialog_forgot.xml
                EditText emailBox = dialogView.findViewById(R.id.resetEmailEditText);

                builder.setView(dialogView);
                AlertDialog dialog = builder.create();

                // Assuming you have a Button/View with this ID in dialog_forgot.xml
                dialogView.findViewById(R.id.resetButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String LoginEmail = emailBox.getText().toString().trim(); // Added trim()

                        // --- Corrected Validation ---
                        if(TextUtils.isEmpty(LoginEmail) || !Patterns.EMAIL_ADDRESS.matcher(LoginEmail).matches()){
                            Toast.makeText(EmailLoginActivity.this,"Enter a valid registered email id",Toast.LENGTH_SHORT).show(); // Improved error message
                            return; // Stop execution if validation fails
                        }
                        // --- End Corrected Validation ---

                        // Assuming you have firebaseAuth initialized in your Activity
                        firebaseAuth.sendPasswordResetEmail(LoginEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Toast.makeText(EmailLoginActivity.this,"Password reset email sent", Toast.LENGTH_LONG).show(); // Improved message
                                    dialog.dismiss(); // Dismiss dialog on success

                                } else {
                                    // Handle specific errors from Firebase Auth if needed (e.g., user not found)
                                    String errorMessage = "Unable to send password reset email. Please try again.";
                                    if (task.getException() != null) {
                                        errorMessage = "Failed: " + task.getException().getMessage();
                                        // Log the error for debugging: Log.e("ResetPassword", "Failed to send reset email", task.getException());
                                    }
                                    Toast.makeText(EmailLoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    // Optionally keep the dialog open or provide specific feedback
                                }
                            }
                        });
                    }
                });

                // Assuming you have a Button/View with this ID in dialog_forgot.xml
                dialogView.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss(); // Dismiss the dialog when cancel is clicked
                    }
                });

                // Set dialog background to transparent if you have a custom background on the root of dialog_forgot.xml
                if(dialog.getWindow() != null){
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT)); // Use Color.TRANSPARENT or new ColorDrawable(0)

                }

                // Show the dialog
                dialog.show();
            }
        });
        // Listener for Create New Account Button
        createNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EmailLoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }



    private boolean checkStaySignedInPreference() {
        boolean staySignedIn = sharedPreferences.getBoolean(PREF_STAY_SIGNED_IN, false);
        if (staySignedIn) {
            String userType = sharedPreferences.getString(PREF_USER_TYPE, null);
            String userIdentifier = sharedPreferences.getString(PREF_USER_IDENTIFIER, null); // This is now UID for regular users

            if (userType != null && userIdentifier != null) {
                Intent intent;
                if (USER_TYPE_ADMIN.equals(userType)) {
                    // Auto-login as Admin
                    Toast.makeText(this, "Welcome back, Admin!", Toast.LENGTH_SHORT).show();
                    intent = new Intent(EmailLoginActivity.this, AdminDashboardActivity.class);
                    // intent.putExtra("ADMIN_ID", userIdentifier); // Pass admin identifier if needed
                } else if (USER_TYPE_REGULAR.equals(userType)) {
                    // Auto-login as Regular User
                    // You might want to re-authenticate silently with Firebase Auth here
                    // using firebaseAuth.signInWithCredential or check the current user
                    // However, for this basic implementation, we trust the shared pref and UID
                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                    if (currentUser != null && currentUser.getUid().equals(userIdentifier)) {
                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        intent = new Intent(EmailLoginActivity.this, MainActivity.class);
                        // Pass the user's UID to the dashboard
                        intent.putExtra("USER_UID", userIdentifier);
                    } else {
                        // Firebase Auth user doesn't match or isn't signed in
                        clearStaySignedInPreference();
                        return false;
                    }

                } else {
                    // Invalid user type stored, clear prefs and proceed to manual login
                    clearStaySignedInPreference();
                    return false;
                }

                startActivity(intent);
                finish(); // Finish LoginActivity so user can't go back to it
                return true; // Indicate that auto-login occurred
            } else {
                // Incomplete preferences, clear them
                clearStaySignedInPreference();
            }
        }
        return false; // Did not auto-login
    }


    private void saveStaySignedInPreference(String userType, String userIdentifier) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_STAY_SIGNED_IN, true);
        editor.putString(PREF_USER_TYPE, userType);
        editor.putString(PREF_USER_IDENTIFIER, userIdentifier);
        editor.apply(); // Use apply() for asynchronous saving
    }

    /**
     * Clears the "Stay Signed In" preferences.
     * This should be called on manual logout or if preferences are invalid.
     */
    private void clearStaySignedInPreference() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PREF_STAY_SIGNED_IN);
        editor.remove(PREF_USER_TYPE);
        editor.remove(PREF_USER_IDENTIFIER);
        editor.apply();
    }

    // --- Validation Methods ---
    public boolean validateEmail() {
        String val = LoginEmail.getText().toString().trim();
        if (val.isEmpty()) {
            LoginEmail.setError("Email cannot be Empty");
            LoginEmail.requestFocus();
            return false;
        } else {
            LoginEmail.setError(null); // Clear error if valid
            return true;
        }
    }

    public boolean validatePassword() {
        String val = LoginPassword.getText().toString().trim();
        if (val.isEmpty()) {
            LoginPassword.setError("Password cannot be Empty");
            LoginPassword.requestFocus();
            return false;
        } else {
            LoginPassword.setError(null); // Clear error if valid
            return true;
        }
    }

    // --- Login Logic ---
    public void checkUserOrAdmin() {
        String enteredEmail = LoginEmail.getText().toString().trim();
        String enteredPassword = LoginPassword.getText().toString().trim();

        // Check if the entered email matches the admin identifier (case-insensitive)
        if (enteredEmail.equalsIgnoreCase(ADMIN_EMAIL_IDENTIFIER)) {
            // Attempt Admin Login (still uses Realtime Database check)
            checkAdminCredentials(enteredPassword);
        } else {
            // Attempt Regular User Login using Firebase Authentication
            signInRegularUser(enteredEmail, enteredPassword);
        }
    }

    // Admin login logic (remains the same, using Realtime Database)
    private void checkAdminCredentials(String enteredPassword) {
        // Path to admin password in Firebase
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("admin_credentials").child("password");

        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String storedAdminPassword = snapshot.getValue(String.class);
                    if (storedAdminPassword != null && storedAdminPassword.equals(enteredPassword)) {
                        // Admin Password Correct
                        Toast.makeText(EmailLoginActivity.this, "Admin Login Successful", Toast.LENGTH_SHORT).show();

                        // --- Handle "Stay Signed In" ---
                        if (staySignedInCheckBox.isChecked()) {
                            saveStaySignedInPreference(USER_TYPE_ADMIN, ADMIN_EMAIL_IDENTIFIER);
                        } else {
                            clearStaySignedInPreference(); // Clear if unchecked
                        }
                        // --------------------------------

                        // Navigate to Admin Dashboard
                        Intent intent = new Intent(EmailLoginActivity.this, AdminDashboardActivity.class);
                        startActivity(intent);
                        finish(); // Close the login activity
                    } else {
                        // Incorrect Admin Password
                        LoginPassword.setError("Incorrect Admin Password");
                        LoginPassword.requestFocus();
                        Toast.makeText(EmailLoginActivity.this, "Admin Login Failed", Toast.LENGTH_SHORT).show();
                        clearStaySignedInPreference(); // Clear prefs on failed login attempt
                    }
                } else {
                    // Admin password node doesn't exist
                    Toast.makeText(EmailLoginActivity.this, "Admin configuration error.", Toast.LENGTH_SHORT).show();
                    LoginPassword.setError("Admin login unavailable");
                    clearStaySignedInPreference(); // Clear prefs if admin login is broken
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EmailLoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                clearStaySignedInPreference(); // Clear prefs on error
            }
        });
    }

    // Regular User Login using Firebase Authentication
    private void signInRegularUser(String userEmail, String userPassword) {
        firebaseAuth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(EmailLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                                // --- Handle "Stay Signed In" ---
                                if (staySignedInCheckBox.isChecked()) {
                                    // Save user type and UID for auto-login
                                    saveStaySignedInPreference(USER_TYPE_REGULAR, user.getUid());
                                } else {
                                    clearStaySignedInPreference(); // Clear if unchecked
                                }
                                // --------------------------------

                                // Navigate to User Dashboard
                                Intent intent = new Intent(EmailLoginActivity.this, MainActivity.class);
                                // Pass the user's UID to the dashboard
                                intent.putExtra("USER_UID", user.getUid());
                                startActivity(intent);
                                finish(); // Close the login activity
                            } else {
                                // User is null despite task success - unlikely but handle
                                Toast.makeText(EmailLoginActivity.this, "Login Failed: User data not available.",
                                        Toast.LENGTH_SHORT).show();
                                clearStaySignedInPreference(); // Clear prefs on failed login
                            }

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            // Handle specific Firebase Auth exceptions (e.g., user not found, wrong password)
                            Toast.makeText(EmailLoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();

                            // Set appropriate error messages on EditTexts based on the exception
                            if (task.getException() != null) {
                                String errorCode = task.getException().getMessage();
                                if (errorCode.contains("password") || errorCode.contains("credential")) {
                                    LoginPassword.setError("Incorrect Password");
                                    LoginPassword.requestFocus();
                                } else if (errorCode.contains("user") || errorCode.contains("record")) {
                                    LoginEmail.setError("User not found");
                                    LoginEmail.requestFocus();
                                } else {
                                    // Generic error for other cases
                                    LoginEmail.setError("Login failed");
                                    LoginEmail.requestFocus();
                                }
                            } else {
                                // Fallback generic error
                                LoginEmail.setError("Login failed");
                                LoginEmail.requestFocus();
                            }
                            clearStaySignedInPreference(); // Clear prefs on failed login
                        }
                    }
                });
    }


    // This method would typically be called from a button in the Dashboard activities
    public static void performLogout(Context context) {
        // Sign out from Firebase Authentication
        FirebaseAuth.getInstance().signOut();
        // Clear SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PREF_STAY_SIGNED_IN);
        editor.remove(PREF_USER_TYPE);
        editor.remove(PREF_USER_IDENTIFIER); // Clear the stored UID
        editor.apply();

        // Redirect to Login screen
        Intent intent = new Intent(context, EmailLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        context.startActivity(intent);

    }
}