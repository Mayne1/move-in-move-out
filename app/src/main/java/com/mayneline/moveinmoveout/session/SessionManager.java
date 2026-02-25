package com.mayneline.moveinmoveout.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public final class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREFS = "session_prefs";
    private static final String KEY_UID = "uid";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";

    public interface SessionCallback {
        void onLoaded(@Nullable String role);
    }

    private static volatile SessionManager INSTANCE;

    private final SharedPreferences prefs;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    private String uid;
    private String email;
    private String role;
    private boolean loading;

    private SessionManager(Context context) {
        Context appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();

        this.uid = safe(prefs.getString(KEY_UID, ""));
        this.email = safe(prefs.getString(KEY_EMAIL, ""));
        this.role = safe(prefs.getString(KEY_ROLE, ""));
        this.loading = false;
    }

    public static SessionManager getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (SessionManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SessionManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public synchronized void updateUser(@Nullable FirebaseUser user, @NonNull String caller) {
        if (user == null) {
            Log.d(TAG, "updateUser caller=" + caller + " user=null");
            return;
        }
        uid = safe(user.getUid());
        email = safe(user.getEmail());
        prefs.edit()
                .putString(KEY_UID, uid)
                .putString(KEY_EMAIL, email)
                .apply();
        Log.d(TAG, "updateUser caller=" + caller + " uid=" + uid + " email=" + email);
    }

    public synchronized void loadSession(@NonNull String caller, @NonNull SessionCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            loading = false;
            Log.d(TAG, "loadSession caller=" + caller + " currentUser=null role=" + role);
            callback.onLoaded(role.isEmpty() ? null : role);
            return;
        }

        updateUser(user, caller + "#loadSession");
        loading = true;
        Log.d(TAG, "loadSession caller=" + caller + " started cachedRole=" + role);

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String remoteRole = snapshot == null ? null : snapshot.getString("role");
                    if (remoteRole != null && !remoteRole.trim().isEmpty()) {
                        setRole(remoteRole, caller + "#remote");
                    } else {
                        Log.d(TAG, "loadSession caller=" + caller + " remoteRole empty, keeping cached role=" + role);
                    }
                    synchronized (SessionManager.this) {
                        loading = false;
                    }
                    callback.onLoaded(getRole(caller + "#callback"));
                })
                .addOnFailureListener(e -> {
                    synchronized (SessionManager.this) {
                        loading = false;
                    }
                    Log.d(TAG, "loadSession caller=" + caller + " failed, keeping cached role=" + role + " error=" + e.getMessage());
                    callback.onLoaded(getRole(caller + "#callback_fail"));
                });
    }

    @Nullable
    public synchronized String getRole() {
        return getRole("unknown");
    }

    @Nullable
    public synchronized String getRole(@NonNull String screen) {
        String value = role == null || role.isEmpty() ? null : role;
        Log.d(TAG, "getRole screen=" + screen + " value=" + value);
        return value;
    }

    public synchronized void setRole(@Nullable String value) {
        setRole(value, "unknown");
    }

    public synchronized void setRole(@Nullable String value, @NonNull String caller) {
        role = safe(value);
        prefs.edit().putString(KEY_ROLE, role).apply();
        Log.d(TAG, "setRole caller=" + caller + " value=" + role);
    }

    public synchronized String getUid() {
        return uid;
    }

    public synchronized String getEmail() {
        return email;
    }

    public synchronized boolean isLoading() {
        return loading;
    }

    public synchronized void clearSession() {
        clearSession("unknown");
    }

    public synchronized void clearSession(@NonNull String caller) {
        uid = "";
        email = "";
        role = "";
        loading = false;
        prefs.edit().clear().apply();
        Log.d(TAG, "clearSession caller=" + caller);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
