package com.mayneline.moveinmoveout;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ComparisonDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison_details);

        String room = safe(getIntent().getStringExtra("room"));
        String item = safe(getIntent().getStringExtra("item"));
        String moveInPath = safe(getIntent().getStringExtra("moveInPath"));
        String moveOutPath = safe(getIntent().getStringExtra("moveOutPath"));
        String status = safe(getIntent().getStringExtra("status"));
        String explanation = safe(getIntent().getStringExtra("explanation"));
        double similarity = getIntent().getDoubleExtra("similarity", 0.0);

        TextView textTitle = findViewById(R.id.textDetailsTitle);
        TextView textStatus = findViewById(R.id.textDetailsStatus);
        TextView textExplanation = findViewById(R.id.textDetailsExplanation);
        TextView textSimilarity = findViewById(R.id.textDetailsSimilarity);

        textTitle.setText(room + " - " + item);
        textStatus.setText("Status: " + displayStatus(status));
        textExplanation.setText("Explanation: " + explanation);
        textSimilarity.setText(String.format(java.util.Locale.US, "Similarity: %.2f", similarity));

        bindImage((ImageView) findViewById(R.id.imageDetailsMoveIn), moveInPath);
        bindImage((ImageView) findViewById(R.id.imageDetailsMoveOut), moveOutPath);
    }

    private void bindImage(ImageView view, String path) {
        if (path.isEmpty()) {
            view.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (bitmap == null) {
            view.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }
        view.setImageBitmap(bitmap);
    }

    private String displayStatus(String status) {
        if ("NO_CHANGE".equals(status)) {
            return "No Change";
        }
        if ("MEDIA_MISSING".equals(status)) {
            return "Media Missing";
        }
        return "Needs Review";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
