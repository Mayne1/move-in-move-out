package com.mayneline.moveinmoveout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PropertyDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PROPERTY_ID = "propertyId";
    public static final String EXTRA_ADDRESS_LINE = "addressLine";
    public static final String EXTRA_UNIT = "unit";
    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_ZIP = "zip";
    public static final String EXTRA_ROLE = "role";

    private String propertyId;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail);

        Intent intent = getIntent();
        propertyId = safe(intent.getStringExtra(EXTRA_PROPERTY_ID));
        String addressLine = safe(intent.getStringExtra(EXTRA_ADDRESS_LINE));
        String unit = safe(intent.getStringExtra(EXTRA_UNIT));
        String city = safe(intent.getStringExtra(EXTRA_CITY));
        String state = safe(intent.getStringExtra(EXTRA_STATE));
        String zip = safe(intent.getStringExtra(EXTRA_ZIP));
        role = safe(intent.getStringExtra(EXTRA_ROLE));

        TextView title = findViewById(R.id.textPropertyDetailTitle);
        TextView subtitle = findViewById(R.id.textPropertyDetailSubtitle);
        View shareButton = findViewById(R.id.buttonShareProperty);

        String fullAddress = addressLine + (unit.isEmpty() ? "" : " Unit " + unit);
        title.setText(fullAddress);
        subtitle.setText(city + ", " + state + " " + zip);

        if (!"LANDLORD".equalsIgnoreCase(role)) {
            shareButton.setVisibility(View.GONE);
        }

        shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(this, PropertyShareActivity.class);
            shareIntent.putExtra(PropertyShareActivity.EXTRA_PROPERTY_ID, propertyId);
            shareIntent.putExtra(PropertyShareActivity.EXTRA_ADDRESS_LABEL, fullAddress);
            startActivity(shareIntent);
        });
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
