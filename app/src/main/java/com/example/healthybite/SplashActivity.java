package com.example.healthybite;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {

    private ImageView imgFlag;
    private TextView tvGreeting;

    private FusedLocationProviderClient fused;
    private SharedPreferences prefs;

    private static final String PREFS = "hb_prefs";
    private static final String KEY_ISO = "country_iso";

    // Map sapaan (tambah negara lain sesukamu)
    private static final Map<String, String> GREETINGS = new HashMap<String, String>() {{
        put("id", "Halo");
        put("us", "Hello");
        put("gb", "Hello");
        put("fr", "Bonjour");
        put("jp", "こんにちは");
        put("en", "Hello"); // jaga-jaga
    }};

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fine = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false));
                boolean coarse = Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false));
                if (fine || coarse) detectAndApply();
                else useFallbackThenGo();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        imgFlag = findViewById(R.id.imgFlag);
        tvGreeting = findViewById(R.id.tvGreeting);

        fused = LocationServices.getFusedLocationProviderClient(this);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Tampilkan default dulu agar UI tidak kosong
        applyCountry("id");

        // Mulai deteksi + lanjut ke Main
        askPermissionThenDetect();
    }

    /* ---------- Deteksi + transisi ---------- */

    private void askPermissionThenDetect() {
        boolean fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) detectAndApply();
        else permissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void detectAndApply() {
        // Ambil 1 lokasi terbaru; sederhana & non-deprecated
        CancellationTokenSource cts = new CancellationTokenSource();
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                .addOnSuccessListener(loc -> {
                    String iso = null;
                    if (loc != null) {
                        iso = getIsoFromGeocoder(loc.getLatitude(), loc.getLongitude());
                    }
                    if (iso == null) iso = getFallbackIso();

                    iso = normalizeIso(iso);
                    prefs.edit().putString(KEY_ISO, iso).apply();
                    applyCountry(iso);

                    goToMainDelayed(); // lanjut ke Main setelah tampilkan sapaan/flag
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lokasi gagal, pakai fallback.", Toast.LENGTH_SHORT).show();
                    useFallbackThenGo();
                });
    }

    private void useFallbackThenGo() {
        String iso = normalizeIso(getFallbackIso());
        prefs.edit().putString(KEY_ISO, iso).apply();
        applyCountry(iso);
        goToMainDelayed();
    }

    private void goToMainDelayed() {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 1500); // 1.5 detik
    }

    /* ---------- Helpers ---------- */

    private void applyCountry(@NonNull String isoLowerRaw) {
        String isoLower = normalizeIso(isoLowerRaw);

        // Bendera dari drawable: flag_<iso>.png (huruf kecil)
        int resId = getResources().getIdentifier("flag_" + isoLower, "drawable", getPackageName());
        if (resId != 0) {
            imgFlag.setImageResource(resId);
            imgFlag.setVisibility(View.VISIBLE);
        } else {
            int def = getResources().getIdentifier("flag_id", "drawable", getPackageName());
            if (def != 0) {
                imgFlag.setImageResource(def);
                imgFlag.setVisibility(View.VISIBLE);
            } else {
                imgFlag.setVisibility(View.GONE);
            }
        }

        // Sapaan
        String greet = GREETINGS.getOrDefault(isoLower, "Hello");
        tvGreeting.setText(greet);
    }

    private String normalizeIso(String iso) {
        if (iso == null) return "id";
        iso = iso.trim().toLowerCase(Locale.ROOT);
        if ("uk".equals(iso)) return "gb"; // alias umum
        if (iso.length() > 2) iso = iso.substring(0, 2);
        return iso.isEmpty() ? "id" : iso;
    }

    private String getIsoFromGeocoder(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> list = geocoder.getFromLocation(lat, lon, 1);
            if (list != null && !list.isEmpty()) {
                String iso = list.get(0).getCountryCode();
                if (iso != null && !iso.trim().isEmpty()) return iso;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getFallbackIso() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String net = tm.getNetworkCountryIso();
                if (net != null && !net.isEmpty()) return net;
                String sim = tm.getSimCountryIso();
                if (sim != null && !sim.isEmpty()) return sim;
            }
        } catch (Exception ignored) {}
        String localeIso = Locale.getDefault().getCountry();
        return (localeIso == null || localeIso.isEmpty()) ? "ID" : localeIso;
    }
}
