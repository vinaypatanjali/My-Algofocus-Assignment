package com.example.myassignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    public static int userAccess = 0;
    public static final int RC_SIGN_IN = 1;
    Button button;
    EditText email;
    EditText password;
    ProgressBar progressBar;
    LoginButton facebookButton;
    String usernameString = "";
    private SharedPreferences mPreferences;
    private String sharedPrefFile = "com.example.android.hellosharedprefs";
    protected static GoogleSignInClient mGoogleSignInClient;
    CallbackManager callbackManager;
    private static String TAG = "INFO";
    protected static FirebaseAuth mAuth = MainActivity.mAuth;
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference rootRef = db.getReference().child("Users");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar().hide(); // hide the title bar

        setContentView(R.layout.activity_login);


        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);


        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);


        email = findViewById(R.id.emailhome);
        button = findViewById(R.id.signin);
        password = findViewById(R.id.password);
        progressBar = findViewById(R.id.progressBar);
        facebookButton = findViewById(R.id.facebookLogin);

        progressBar.setVisibility(View.INVISIBLE);


        // Initialize Facebook Login button
        callbackManager = CallbackManager.Factory.create();
        // LoginButton loginButton = mBinding.buttonFacebookLogin;
        facebookButton.setReadPermissions("email", "public_profile","user_friends");
        facebookButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                progressBar.setVisibility(View.VISIBLE);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                progressBar.setVisibility(View.INVISIBLE);
                // ...
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                showToast("facebook:onError: "+error+"");
                progressBar.setVisibility(View.INVISIBLE);

                // ...
            }
        });
// ...

        // Set the dimensions of the sign-in button.
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.sign_in_button:
                        progressBar.setVisibility(View.VISIBLE);
                        googleSignIn();
                        break;
                    // ...
                }
            }
        });

    }


    /**
     * Function to
     * show some message/toast
     * to user
     */
    public void showToast(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
    }


    /**
     * Function to handle
     * facebook login
     */
    private void handleFacebookAccessToken(AccessToken token) {

        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();


                            String usernameString = user.getDisplayName();
                            String emailString = user.getEmail();

                            SharedPreferences.Editor preferencesEditor1 = mPreferences.edit();
                            preferencesEditor1.putString("username", usernameString);


                            preferencesEditor1.putString("email", emailString);


                            preferencesEditor1.apply();
                            Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
                            intent.putExtra("username", usernameString);
                            intent.putExtra("email", emailString);



                            startActivityForResult(intent, RC_SIGN_IN);
                            finish();

                        } else {
                            // If sign in fails, display a message to the user.
                            try {
                                progressBar.setVisibility(View.INVISIBLE);
                                Log.w(TAG, "signInWithCredential:failure", task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication failed or Account already exists with other provider.",
                                        Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }

                        // ...
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }


    /**
     * Function to open
     * Registration Activity
     */
    public void openRegister(View view) {
        Intent intent = new Intent(LoginActivity.this, Register.class);
        startActivity(intent);
        finish();
    }


    /**
     * Function to register details
     * with firebase
     * database
     */
    public void onSignin(View view) {

        final String emailString = email.getText().toString();
        final String passwordString = password.getText().toString();


        if (TextUtils.isEmpty(emailString)) {
            email.setError("Enter Email ID");

            return;
        }

        if (TextUtils.isEmpty(passwordString)) {
            if (passwordString.length() < 6)
                password.setError("Password should be at least of Characters");
            else
                password.setError("Enter Password");

            return;
        }


        progressBar.setVisibility(View.VISIBLE);
        button.setText("Signing in...");

        mAuth.signInWithEmailAndPassword(emailString, passwordString)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            final String uid = user.getUid();


                            Log.i(uid, user.toString());


                            final Gson gson = new Gson();
                            final JsonParser parser = new JsonParser();

                            DatabaseReference reference = rootRef.child(uid).child("username");
                            rootRef.child(uid).child("Access").setValue(userAccess++);


                            reference.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    final String usernameFirebase1 = snapshot.getValue().toString();

                                    SharedPreferences.Editor preferencesEditor1 = mPreferences.edit();
                                    preferencesEditor1.putString("username", usernameFirebase1);


                                    preferencesEditor1.putString("email", emailString);
                                    preferencesEditor1.putString("password", passwordString);

                                    preferencesEditor1.apply();


                                    final Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
                                    intent.putExtra("username", usernameFirebase1);
                                    intent.putExtra("password", passwordString);
                                    intent.putExtra("email", emailString);
                                    startActivity(intent);


                                    Log.i("datasnapshot", "" + snapshot.getValue());
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                            button.setText("Login");
                            progressBar.setVisibility(View.INVISIBLE);

                        }

                        // ...
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("Failure", e.getMessage());
            }
        });

    }


    /**
     * Funtion to
     * sign in with google
     */
    private void googleSignIn() {

        // Configure sign-in to request the user's ID, email address, and basic
// profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        MainActivity.mGoogleSignInClient = mGoogleSignInClient;
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                progressBar.setVisibility(View.INVISIBLE);
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }


    /**
     * Function to verify
     * deatils for sign in
     * with google
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            String usernameString = user.getDisplayName();
                            String emailString = user.getEmail();

                            SharedPreferences.Editor preferencesEditor1 = mPreferences.edit();
                            preferencesEditor1.putString("username", usernameString);
                            // preferencesEditor1.apply();

                            preferencesEditor1.putString("email", emailString);


                            preferencesEditor1.apply();
                            Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
                            intent.putExtra("username", usernameString);
                            intent.putExtra("email", emailString);

                            // intent.putExtra("parceable",(par)mAuth);

                            startActivityForResult(intent, RC_SIGN_IN);
                            finish();
                            // updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            progressBar.setVisibility(View.INVISIBLE);
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            //Snackbar.make( this,"Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            //updateUI(null);
                        }

                        // ...
                    }
                });
    }


}
