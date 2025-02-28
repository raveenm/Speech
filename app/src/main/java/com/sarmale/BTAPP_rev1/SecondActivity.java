package com.sarmale.BTAPP_rev1;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SecondActivity extends AppCompatActivity {

    private String receivedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        TextView textViewDisplay = findViewById(R.id.textViewDisplay);
        EditText editTextSearch = findViewById(R.id.editTextSearch);
        Button buttonSearch = findViewById(R.id.buttonSearch);
        Button buttonReset = findViewById(R.id.buttonReset);
        Button buttonBack = findViewById(R.id.buttonBack);

        // Get the text from the Intent
        receivedText = getIntent().getStringExtra("text_key");

        // Display the received text
        textViewDisplay.setText(receivedText);

        // Search Button Click Listener
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchText = editTextSearch.getText().toString().trim();
                if (!searchText.isEmpty()) {
                    String result = findLinesContainingWord(receivedText, searchText);
                    textViewDisplay.setText(result);
                } else {
                    textViewDisplay.setText("No input provided");
                }

                // Hide the keyboard after clicking search
                hideKeyboard();
            }
        });

        // Reset Button Click Listener
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textViewDisplay.setText(receivedText); // Reset to original text
                editTextSearch.setText(""); // Clear the search input
                hideKeyboard(); // Hide keyboard when resetting
            }
        });

        // Back Button Click Listener
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Closes SecondActivity and goes back to MainActivity
            }
        });
    }

    // Function to find ALL lines containing the searched word
    private String findLinesContainingWord(String text, String word) {
        if (text != null) {
            String[] lines = text.split("\n"); // Split text into lines
            StringBuilder result = new StringBuilder();

            for (String line : lines) {
                if (line.contains(word)) {
                    result.append(line).append("\n"); // Append matching lines
                }
            }

            return result.length() > 0 ? "Found:\n" + result.toString().trim() : "Word not found!";
        }
        return "No text available!";
    }

    // Function to hide the keyboard
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
