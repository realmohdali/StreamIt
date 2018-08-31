package it.stream.streamit;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import it.stream.streamit.database.ConnectionCheck;

public class Offline extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);
    }

    public void tryAgain(View view) {
        if (ConnectionCheck.isConnected(this)) {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (ConnectionCheck.isConnected(this)) {
            super.onBackPressed();
        }
    }
}
