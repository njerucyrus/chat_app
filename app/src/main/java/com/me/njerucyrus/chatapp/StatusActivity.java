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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;


public class StatusActivity extends AppCompatActivity {

    private TextInputLayout mStatus;
    private Button mSaveChanges;
    private Toolbar mToolbar;
    private DatabaseReference mRef;
    private FirebaseUser mCurrentUser;
    private ProgressDialog mProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        mToolbar = (Toolbar) findViewById(R.id.status_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Status");
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        mProgress = new ProgressDialog(StatusActivity.this);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());

        String defaultStatus = getIntent().getStringExtra("status_value");
        mStatus = (TextInputLayout) findViewById(R.id.txt_status);
        mStatus.getEditText().setText(defaultStatus);

        mSaveChanges = (Button) findViewById(R.id.btn_status_save_changes);
        mSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //DO STAFF HERE
                String status = mStatus.getEditText().getText().toString().trim();
                showProgress("Saving changes", "Please wait...");
                updateStatus(status);
            }
        });
    }

    private void updateStatus(String status) {
        mRef.child("status").setValue(status)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            hideProgress();
                            Toast.makeText(getApplicationContext(), "Changes saved", Toast.LENGTH_LONG).show();
                        }else{
                            hideProgress();
                            Toast.makeText(getApplicationContext(), "Error Occurred When saving Changes", Toast.LENGTH_LONG).show();

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
