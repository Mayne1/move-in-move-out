package com.mayneline.moveinmoveout;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class LegalTranslatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal_translator);

        EditText inputEditText = findViewById(R.id.editTextLegalInput);
        EditText outputEditText = findViewById(R.id.editTextLegalOutput);

        findViewById(R.id.buttonLegalTranslate).setOnClickListener(v -> {
            String input = inputEditText.getText().toString().trim();
            outputEditText.setText("Plain English: " + input);
        });
    }
}
