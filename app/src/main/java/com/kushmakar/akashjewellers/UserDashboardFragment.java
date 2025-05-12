package com.kushmakar.akashjewellers;



import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.kushmakar.akashjewellers.databinding.FragmentUserDashboardBinding; // Generated binding class
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class UserDashboardFragment extends Fragment {

    private static final String TAG = "UserDashboardFragment"; // Changed TAG
    private static final String PREF_LAST_LOGIN = "last_login";
    private static final long LOGOUT_THRESHOLD = TimeUnit.DAYS.toMillis(15);

    private FragmentUserDashboardBinding binding; // View binding object

    private TextView tvGoldPrice, tvGoldUpdateTime;
    private TextView tvSilverPrice, tvSilverUpdateTime;
    private TextView tvGoldUpiPrice, tvGoldUpiUpdateTime;
    private TextView tvSilverUpiPrice, tvSilverUpiUpdateTime;

    private DatabaseReference priceUpdateNodeReference;
    private ValueEventListener priceValueEventListener;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;

    public UserDashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment using view binding
        binding = FragmentUserDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot(); // Return the root view
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth (use getInstance() in Fragment)
        mAuth = FirebaseAuth.getInstance();

        // Initialize SharedPreferences (use requireActivity() to get Activity context)
        // Consider using PreferenceManager.getDefaultSharedPreferences for simplicity
        sharedPreferences = requireActivity().getSharedPreferences("user_prefs", requireContext().MODE_PRIVATE);


        // Check for inactivity and logout if needed
        checkLastLogin();

        // Update the last login time
        updateLastLogin();

        // Get references to TextViews from the binding object
        tvGoldPrice = binding.goldPrice;
        tvGoldUpdateTime = binding.goldUpdateTime;
        tvSilverPrice = binding.silverPrice;
        tvSilverUpdateTime = binding.silverUpdateTime;
        tvGoldUpiPrice = binding.goldUpiPrice;
        tvGoldUpiUpdateTime = binding.goldUpiUpdateTime;
        tvSilverUpiPrice = binding.silverUpiPrice;
        tvSilverUpiUpdateTime = binding.silverUpiUpdateTime;

        // Initialize Firebase Database reference (use getInstance() in Fragment)
        priceUpdateNodeReference = FirebaseDatabase.getInstance().getReference("price_updates");

        // Setup the real-time listener for price updates
        setupFirebaseListener();

        // Any other view-related setup or listeners for elements within this fragment
        // can be set up here using the binding object.
        // Example: If you had a button with ID 'refresh_button' in fragment_user_dashboard.xml
        // binding.refreshButton.setOnClickListener(v -> {
        //     // Handle refresh action
        // });
    }

    private void checkLastLogin() {
        long lastLoginTime = sharedPreferences.getLong(PREF_LAST_LOGIN, 0);
        long currentTime = System.currentTimeMillis();

        if (lastLoginTime != 0 && (currentTime - lastLoginTime) > LOGOUT_THRESHOLD) {
            // User hasn't opened the app for more than 15 days, log them out
            mAuth.signOut();
            // Use requireActivity() to show Toast from the Activity context
            Toast.makeText(requireActivity(), "Logged out due to inactivity.", Toast.LENGTH_LONG).show();
            navigateToLoginActivity();
        }
    }

    private void updateLastLogin() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_LAST_LOGIN, System.currentTimeMillis());
        editor.apply();
        Log.d(TAG, "Last login time updated.");
    }

    private void navigateToLoginActivity() {
        // Use requireActivity() to start the activity from the Activity context
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // Finish the hosting activity if needed, though typically you might just replace the fragment
        // requireActivity().finish();
    }

    // setupBottomNavigation method should remain in MainActivity

    private void setupFirebaseListener() {
        priceValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Ensure you have a PriceData class matching your Firebase structure
                    PriceData currentPrices = dataSnapshot.getValue(PriceData.class);
                    if (currentPrices != null) {
                        Log.d(TAG, "Data received: Gold=" + currentPrices.getGold_price() + ", Timestamp=" + currentPrices.getTimestamp());
                        updatePriceUI(currentPrices);
                    } else {
                        Log.w(TAG, "PriceData object is null.");
                        clearPriceUI("Data Error");
                    }
                } else {
                    Log.w(TAG, "price_updates node does not exist.");
                    clearPriceUI("No Data");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read price data.", databaseError.toException());
                // Use requireActivity() to show Toast from the Activity context
                Toast.makeText(requireActivity(), "Failed to load price data.", Toast.LENGTH_SHORT).show();
                clearPriceUI("Load Error");
            }
        };
        priceUpdateNodeReference.addValueEventListener(priceValueEventListener);
        Log.d(TAG, "Firebase ValueEventListener attached.");
    }

    private void updatePriceUI(PriceData prices) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(2);

        String formattedTime = "--";
        if (prices.getTimestamp() != null) {
            try {
                // Use requireContext() or requireActivity() for getting resources if needed
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault());
                Date resultDate = new Date(prices.getTimestamp());
                formattedTime = sdf.format(resultDate);
            } catch (Exception e) {
                Log.e(TAG, "Error formatting timestamp", e);
                formattedTime = "Invalid Date";
            }
        }

        tvGoldPrice.setText(prices.getGold_price() != null ? currencyFormat.format(prices.getGold_price()) : "N/A");
        tvGoldUpdateTime.setText(formattedTime);

        tvSilverPrice.setText(prices.getSilver_price() != null ? currencyFormat.format(prices.getSilver_price()) : "N/A");
        tvSilverUpdateTime.setText(formattedTime);

        tvGoldUpiPrice.setText(prices.getGold_rtgs_price() != null ? currencyFormat.format(prices.getGold_rtgs_price()) : "N/A");
        tvGoldUpiUpdateTime.setText(formattedTime);

        tvSilverUpiPrice.setText(prices.getSilver_rtgs_price() != null ? currencyFormat.format(prices.getSilver_rtgs_price()) : "N/A");
        tvSilverUpiUpdateTime.setText(formattedTime);
    }

    private void clearPriceUI(String message) {
        tvGoldPrice.setText(message);
        tvSilverPrice.setText("");
        tvGoldUpiPrice.setText("");
        tvSilverUpiPrice.setText("");

        String placeholderTime = "--";
        tvGoldUpdateTime.setText(placeholderTime);
        tvSilverUpdateTime.setText(placeholderTime);
        tvGoldUpiUpdateTime.setText(placeholderTime);
        tvSilverUpiUpdateTime.setText(placeholderTime);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the binding object
        binding = null;
        Log.d(TAG, "onDestroyView: Binding nulled.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove the Firebase listener to prevent memory leaks
        if (priceUpdateNodeReference != null && priceValueEventListener != null) {
            priceUpdateNodeReference.removeEventListener(priceValueEventListener);
            Log.d(TAG, "Firebase ValueEventListener removed in onDestroy.");
        }
        Log.d(TAG, "onDestroy: Fragment destroyed.");
    }
}