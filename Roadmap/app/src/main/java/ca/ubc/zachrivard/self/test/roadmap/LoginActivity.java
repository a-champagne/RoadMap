package ca.ubc.zachrivard.self.test.roadmap;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionValues;
import android.util.Log;
import android.view.View;
import android.support.annotation.NonNull;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends Activity{

    private static final int SPLASH_DELAY_MS = 1500;

    ConstraintLayout root;

    ImageView logo;
    Button loginButton;
    EditText email, password;
    FirebaseAuth firebaseAuth;
    ProgressBar progressBar;
    private FirebaseAuth.AuthStateListener authStateListener;

    private static final String TAG = LoginActivity.class.getName();

    private App mApplication;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApplication = (App) getApplication();
        root = findViewById(R.id.root);

        loginButton = (Button) findViewById(R.id.loginButton);
        email = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        logo = findViewById(R.id.logo);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    mApplication.startMainActivityForUser(LoginActivity.this, user);

                } else {
                    Log.e(TAG, "Oof");
                }
            }
        };

        Handler handler = new Handler();
        Runnable runnable = () -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                mApplication.startMainActivityForUser(this, user);
            } else {
                TransitionManager.beginDelayedTransition(root);

                ConstraintSet cs = new ConstraintSet();
                cs.clone(this, R.layout.activity_login_view);
                cs.applyTo(root);
            }
        };

        handler.postDelayed(runnable, SPLASH_DELAY_MS);
    }

    public void login(View view) {
        String emailInput = email.getText().toString();
        String passwordInput = password.getText().toString();

        if (emailInput == null || passwordInput == null || emailInput.isEmpty() || passwordInput.isEmpty()) {
            Toast.makeText(this, "Please enter credentials.", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        firebaseAuth.signInWithEmailAndPassword(emailInput, passwordInput).addOnCompleteListener(LoginActivity.this, new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                progressBar.setVisibility(View.GONE);
                if (!task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Failed to log in.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Unsuccessful login");
                } else {
                    mApplication.refreshAuthToken();
                    Log.e(TAG, "Hell yeah brother. Cheers from Iraq");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}