package com.mayneline.moveinmoveout;

import android.os.Bundle;
import android.widget.TextView;
import com.mayneline.moveinmoveout.BuildConfig;
import androidx.appcompat.app.AppCompatActivity;

public class BuildInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Simple, no-BS UI so you can SEE the build info immediately.
        TextView tv = new TextView(this);
        tv.setPadding(40, 40, 40, 40);
        tv.setTextSize(18f);

        String text =
                "Build Info\n\n" +
                        "App ID: " + BuildConfig.APPLICATION_ID + "\n" +
                        "Build Type: " + BuildConfig.BUILD_TYPE + "\n" +
                        "Version Name: " + BuildConfig.VERSION_NAME + "\n" +
                        "Version Code: " + BuildConfig.VERSION_CODE + "\n";

        tv.setText(text);
        setContentView(tv);
    }
}