package ca.ubc.zachrivard.self.test.roadmap;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1500;
    private static final String TAG = MainActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Handler handler = new Handler();
        Runnable runnable = () -> {
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
//            i.putExtra("tripID", "29");
//            i.putExtra("userType", "driver");
            startActivity(i);
        };

        handler.postDelayed(runnable, SPLASH_DELAY_MS);
    }
}
