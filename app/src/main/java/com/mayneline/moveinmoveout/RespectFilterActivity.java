package com.mayneline.moveinmoveout;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class RespectFilterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respect_filter);

        EditText inputEditText = findViewById(R.id.editTextRespectInput);
        EditText outputEditText = findViewById(R.id.editTextRespectOutput);

        findViewById(R.id.buttonRespectTranslate).setOnClickListener(v -> {
            String input = inputEditText.getText().toString().trim();
            outputEditText.setText("Professional: " + input);
        });
    }
}
