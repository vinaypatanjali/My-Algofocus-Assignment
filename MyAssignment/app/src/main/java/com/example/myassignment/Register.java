package com.example.myassignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    EditText username, email, password;
    Button register;

    private static String TAG = "INFO";
    public Intent intent;

    private FirebaseAuth mAuth;
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference rootRef = db.getReference().child("Users");

    private SharedPreferences mPreferences;
    private String sharedPrefFile = "com.example.android.hellosharedprefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar().hide(); // hide the title bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //enable full screen
        setContentView(R.layout.activity_register);


        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);


        final VideoView simpleVideoView = (VideoView) findViewById(R.id.videoView);
        simpleVideoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.turtlesvideo));
        simpleVideoView.start();


        final ImageView imageView = findViewById(R.id.imageView);
        imageView.setVisibility(View.INVISIBLE);

        simpleVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //   simpleVideoView.s
                simpleVideoView.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.VISIBLE);
            }
        });

        simpleVideoView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                simpleVideoView.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.VISIBLE);
            }
        });

        username = findViewById(R.id.name);
        email = findViewById(R.id.emailhome);
        password = findViewById(R.id.password);
        register = findViewById(R.id.register);

        mAuth = FirebaseAuth.getInstance();
    }


    /**
     * Function to open
     * Login page if user already
     * have a account
     */
    public void openLogin(View view) {
        intent = new Intent(Register.this, MainActivity.class);

        startActivity(intent);
        finish();
    }


    /**
     * Function to Register
     * the user with Firebase
     */
    public void onRegister(View view) {
        register.setText(R.string.registering);

        final String usernameString = username.getText().toString();
        final String emailString = email.getText().toString();
        final String passwordString = password.getText().toString();

        if (TextUtils.isEmpty(usernameString)) {
            email.setError("Enter Username");
            register.setText(R.string.register);
            return;
        }

        if (TextUtils.isEmpty(emailString)) {
            email.setError("Enter Email ID");
            register.setText(R.string.register);
            return;
        }

        if (TextUtils.isEmpty(passwordString) || passwordString.length() < 6) {
            if (passwordString.length() < 6)
                password.setError("Password should be at least of 6 Characters");
            else
                password.setError("Enter Password");

            register.setText(R.string.register);
            return;
        }

        mAuth.createUserWithEmailAndPassword(emailString, passwordString)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.i(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            String uuid = user.getUid();
                            Map<String, String> map = new HashMap<>();
                            map.put("username", usernameString);
                            map.put("email", emailString);
                            map.put("password", passwordString);


                            rootRef.child(uuid).setValue(map);
                            rootRef.child(uuid).child("Access").setValue(LoginActivity.userAccess++);

                            Toast.makeText(Register.this, "Success", Toast.LENGTH_SHORT).show();
                            //updateUI(user);
                            intent = new Intent(Register.this, LoginActivity.class);

                            startActivity(intent);
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.i(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(Register.this, "Authentication failed: " + task.getException(),
                                    Toast.LENGTH_SHORT).show();
                            register.setText(R.string.register);
                            //updateUI(null);
                        }

                        // ...
                    }
                });
    }
}
