package com.me.njerucyrus.chatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersRef;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mToolbar = (Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mEmail = (TextInputLayout) findViewById(R.id.login_email);
        mPassword = (TextInputLayout) findViewById(R.id.login_password);

        mAuth = FirebaseAuth.getInstance();
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        mProgress = new ProgressDialog(LoginActivity.this);

        btnLogin = (Button) findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    String email = mEmail.getEditText().getText().toString().trim();
                    String password = mPassword.getEditText().getText().toString().trim();
                    showProgress("Authenticating", "Please wait...");
                    loginUser(email, password);
                }
            }
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            updateLoginUI();



                        } else {
                            hideProgress();
                            Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_LONG).show();


                        }
                    }
                });
    }

    private void updateLoginUI() {
        //update the user device token.
        //device token is used for sending notification and other staff in cloud messaging
        FirebaseUser mCurrentUser = mAuth.getCurrentUser();
        String deviceTokenId = FirebaseInstanceId.getInstance().getToken();

        mUsersRef.child(mCurrentUser.getUid()).child("deviceTokenId").setValue(deviceTokenId)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> createUserTask) {
                        if (createUserTask.isSuccessful()){
                            hideProgress();

                            Toast.makeText(getApplicationContext(), "Login successful", Toast.LENGTH_LONG).show();

                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }else{
                            hideProgress();
                            Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_LONG).show();

                        }
                    }
                });
    }

    private void showProgress(String title, String message) {
        mProgress.setTitle(title);
        mProgress.setMessage(message);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();
    }

    private void hideProgress() {
        if (mProgress.isShowing() && mProgress != null) {
            mProgress.dismiss();
        }
    }

    private boolean validate() {
        boolean valid = true;
        String email = mEmail.getEditText().getText().toString().trim();
        String password = mPassword.getEditText().getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            mEmail.setError("This field is required");
            valid = false;
        } else {
            mEmail.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            mPassword.setError("This field is required");
            valid = false;
        } else {
            mPassword.setError(null);
        }
        return valid;
    }
}
