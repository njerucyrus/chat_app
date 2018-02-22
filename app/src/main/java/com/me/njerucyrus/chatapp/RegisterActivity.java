package com.me.njerucyrus.chatapp;


import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;


public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout mDisplayName;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private Button btnRegister;

    private FirebaseAuth mAuth;
    private DatabaseReference mRef;

    private ProgressDialog mProgress;
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mToolbar = (Toolbar) findViewById(R.id.register_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Create account");
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        mDisplayName = (TextInputLayout) findViewById(R.id.reg_display_name);
        mEmail = (TextInputLayout) findViewById(R.id.reg_email);
        mPassword = (TextInputLayout) findViewById(R.id.reg_password);
        btnRegister = (Button) findViewById(R.id.btn_register);

        mProgress = new ProgressDialog(RegisterActivity.this);

        mAuth = FirebaseAuth.getInstance();
        //initialize database referece.
        mRef = FirebaseDatabase.getInstance().getReference();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String displayName = mDisplayName.getEditText().getText().toString().trim();
                String email = mEmail.getEditText().getText().toString().trim();
                String password = mPassword.getEditText().getText().toString().trim();

                showProgress("Creating account", "please wait...");
                registerUser(displayName, email, password);

            }
        });
    }

    private void registerUser(final String displayName, String email, String password) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            FirebaseUser currentUser = mAuth.getCurrentUser();

                            String status = "Hi there am using chat app";
                            String imageThumbnail = "default";
                            String image = "default";
                            String userUid = currentUser.getUid();
                            String deviceTokenId = FirebaseInstanceId.getInstance().getToken();
                            String  online = "false";

                            User user = new User(userUid, displayName, status, image, imageThumbnail, deviceTokenId, online);

                            mRef.child("Users").child(currentUser.getUid())
                                    .setValue(user)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                hideProgress();
                                                Toast.makeText(getApplicationContext(), "Account created successfully", Toast.LENGTH_LONG).show();
                                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                finish();
                                            } else {

                                                hideProgress();
                                                Toast.makeText(getApplicationContext(), "You got an error", Toast.LENGTH_LONG).show();
                                            }

                                        }
                                    });

                        }

                    }
                });
    }

    private void showProgress(String title, String message) {
        mProgress.setTitle(title);
        mProgress.setMessage(message);
        mProgress.setCancelable(false);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();
    }

    private void hideProgress() {
        if (mProgress.isShowing() && mProgress != null) {
            mProgress.dismiss();
        }
    }

}
