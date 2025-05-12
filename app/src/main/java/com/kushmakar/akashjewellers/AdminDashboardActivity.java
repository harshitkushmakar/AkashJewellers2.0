package com.kushmakar.akashjewellers;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Firebase Imports
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    // Add a TAG for logging
    private static final String TAG = "AdminDashboardActivity";

    // Declare UI elements from your layout
    private EditText goldPriceEditText, silverPriceEditText, goldRTGSPriceEditText, silverRTGSPriceEditText;
    private TextView timestampTextView; // Renamed for clarity (was timestampEditText)
    AppCompatButton currentTimeButton, saveButton;

    // Declare Firebase Database reference
    // This reference now points directly to the node we want to update
    private DatabaseReference priceUpdateNodeReference;

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard); // Use your layout file name

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // --- Initialize Firebase ---
        // Point DIRECTLY to the "price_updates" node in your Firebase Realtime Database.
        // We will be setting the value OF THIS NODE, not pushing children under it.
        priceUpdateNodeReference = FirebaseDatabase.getInstance().getReference("price_updates");
        Log.d(TAG, "Firebase reference points to: " + priceUpdateNodeReference.toString());



        // Price input fields
        goldPriceEditText = findViewById(R.id.goldPriceEditText);
        silverPriceEditText = findViewById(R.id.silverPriceEditText);
        goldRTGSPriceEditText = findViewById(R.id.goldRTGSPriceEditText);
        silverRTGSPriceEditText = findViewById(R.id.silverRTGSPriceEditText);

        // Timestamp display TextView and buttons
        timestampTextView = findViewById(R.id.timestampEditText); // Make sure ID matches your XML
        currentTimeButton = findViewById(R.id.currentTimeButton);
        saveButton = findViewById(R.id.saveButton);



        // Listener for the button to update the DISPLAY timestamp
        currentTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date currentDate = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault());
                String formattedDateTime = sdf.format(currentDate);
                timestampTextView.setText(formattedDateTime); // Update the TextView
                Toast.makeText(AdminDashboardActivity.this, "Display time updated", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener for the button to SAVE data to Firebase
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOrUpdatePriceDataToFirebase(); // Call the updated saving method
            }
        });

        // Optional: Set initial display time on load
        SimpleDateFormat initialSdf = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault());
        timestampTextView.setText(initialSdf.format(new Date()));

    } // End of onCreate

    // --- Method to Save or Update Data to Firebase ---
    // Renamed for clarity
    private void saveOrUpdatePriceDataToFirebase() {
        // 1. Read values from EditText fields
        String goldPriceStr = goldPriceEditText.getText().toString().trim();
        String silverPriceStr = silverPriceEditText.getText().toString().trim();
        String goldRTGSPriceStr = goldRTGSPriceEditText.getText().toString().trim();
        String silverRTGSPriceStr = silverRTGSPriceEditText.getText().toString().trim();

        // 2. Validate input
        if (TextUtils.isEmpty(goldPriceStr) || TextUtils.isEmpty(silverPriceStr) ||
                TextUtils.isEmpty(goldRTGSPriceStr) || TextUtils.isEmpty(silverRTGSPriceStr)) {
            Toast.makeText(this, "Please fill in all price fields", Toast.LENGTH_SHORT).show();
            return; // Stop execution if any field is empty
        }

        double goldPrice, silverPrice, goldRTGSPrice, silverRTGSPrice;
        try {
            // Attempt to parse strings to doubles
            goldPrice = Double.parseDouble(goldPriceStr);
            silverPrice = Double.parseDouble(silverPriceStr);
            goldRTGSPrice = Double.parseDouble(goldRTGSPriceStr);
            silverRTGSPrice = Double.parseDouble(silverRTGSPriceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format in one or more fields", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "NumberFormatException: ", e);
            return; // Stop execution if parsing fails
        }

        // 3. Prepare data for Firebase
        // Use a Map to structure the data
        Map<String, Object> priceData = new HashMap<>();
        priceData.put("gold_price", goldPrice);
        priceData.put("silver_price", silverPrice);
        priceData.put("gold_rtgs_price", goldRTGSPrice);
        priceData.put("silver_rtgs_price", silverRTGSPrice);
        // Use Firebase ServerValue.TIMESTAMP for a reliable server-side timestamp
        priceData.put("timestamp", ServerValue.TIMESTAMP);

        // 4. Save data to Firebase Realtime Database
        // --- THIS IS THE KEY CHANGE ---
        // Instead of push().setValue(), use setValue() directly on the reference.
        // This will OVERWRITE the data at the "price_updates" node location.
        Log.d(TAG, "Attempting to update data at: " + priceUpdateNodeReference.toString());
        priceUpdateNodeReference.setValue(priceData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Data saved/updated successfully
                        Toast.makeText(AdminDashboardActivity.this, "Prices updated successfully!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Data updated successfully at " + priceUpdateNodeReference.getKey());
                        // Optional: Clear fields after successful save/update
                        goldPriceEditText.setText("");
                        silverPriceEditText.setText("");
                        goldRTGSPriceEditText.setText("");
                        silverRTGSPriceEditText.setText("");
                        // Consider *not* clearing fields if the admin might want to make small adjustments
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to save/update data
                        Toast.makeText(AdminDashboardActivity.this, "Failed to update prices: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to update data at " + priceUpdateNodeReference.getKey(), e);
                    }
                });
    }

}