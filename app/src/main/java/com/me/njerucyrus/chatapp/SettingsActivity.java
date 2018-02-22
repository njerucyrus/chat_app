package com.me.njerucyrus.chatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    private DatabaseReference mRef;
    private StorageReference mStorageRef;
    private StorageReference mStorageThumbnailRef;
    private FirebaseUser mCurrentUser;
    private TextView mDisplayName;
    private TextView mStatus;
    private CircleImageView mCircleImageView;
    private Button mStatusButton;
    private Button mImageBtn;
    private static final int GALLERY_PICK = 1;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mStatus = (TextView) findViewById(R.id.settings_status);
        mDisplayName = (TextView) findViewById(R.id.settings_displayname);
        mCircleImageView = (CircleImageView) findViewById(R.id.profile_image);

        mRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());
        mRef.keepSynced(true);
        mStorageRef = FirebaseStorage.getInstance().getReference().child("profile_images");
        mStorageThumbnailRef = FirebaseStorage.getInstance().getReference().child("profile_images").child("thumbnails");

        getAccountInfo();
        mProgress = new ProgressDialog(SettingsActivity.this);

        mStatusButton = (Button) findViewById(R.id.btn_change_status);
        mStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SettingsActivity.this, StatusActivity.class)
                        .putExtra("status_value", mStatus.getText().toString())
                );
            }
        });

        mImageBtn = (Button) findViewById(R.id.btn_change_image);
        mImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(SettingsActivity.this);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {

            if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                CropImage.activity(imageUri)
                        .setAspectRatio(1, 1)
                        .setMinCropWindowSize(500, 500)
                        .start(SettingsActivity.this);
            }

            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    Uri resultUri = result.getUri();
                    showProgress("Uploading", "Please wait...");

                    File thumbnailFilePath = new File(resultUri.getPath());

                    Bitmap thumbnailBitmap = new Compressor(SettingsActivity.this)
                            .setMaxHeight(200)
                            .setMaxWidth(200)
                            .setQuality(75)
                            .compressToBitmap(thumbnailFilePath);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    final byte[] thumbnailBytes = baos.toByteArray();


                    mStorageRef.child(mCurrentUser.getUid() + ".jpg")
                            .putFile(resultUri)
                            .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        //add a image download link to the user
                                        final String downloadUrl = task.getResult().getDownloadUrl().toString();

                                        UploadTask uploadTask = mStorageThumbnailRef.child(mCurrentUser.getUid()).putBytes(thumbnailBytes);
                                        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumbnailTask) {
                                                 String thumbnailUrl = thumbnailTask.getResult().getDownloadUrl().toString();
                                                if (thumbnailTask.isSuccessful()){
                                                    Map uploadData = new HashMap();
                                                    uploadData.put("image", downloadUrl);
                                                    uploadData.put("imageThumbnail", thumbnailUrl);

                                                    mRef.updateChildren(uploadData)
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        hideProgress();
                                                                        Toast.makeText(getApplicationContext(), "Profile image uploaded successfully", Toast.LENGTH_LONG).show();

                                                                    } else {
                                                                        hideProgress();
                                                                        Toast.makeText(getApplicationContext(),
                                                                                "Error occurred while uploading profile image please try again latter",
                                                                                Toast.LENGTH_LONG).show();
                                                                    }
                                                                }
                                                            });
                                                }

                                            }
                                        });


                                    } else {
                                        hideProgress();
                                        Toast.makeText(getApplicationContext(),
                                                "Error occurred while uploading profile image please try again latter",
                                                Toast.LENGTH_LONG).show();

                                    }
                                }
                            });

                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        mRef.child("online").setValue("true");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRef.child("online").setValue(ServerValue.TIMESTAMP.toString());

    }

    private void getAccountInfo() {
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    final User user = dataSnapshot.getValue(User.class);
                    mDisplayName.setText(user.getDisplayName());
                    mStatus.setText(user.getStatus());
                    if (!user.getImage().equalsIgnoreCase("default")) {
                        Picasso.with(getApplicationContext()).load(user.getImageThumbnail())
                                .networkPolicy(NetworkPolicy.OFFLINE)
                                .placeholder(R.mipmap.default_avator)
                                .into(mCircleImageView, new Callback() {
                                    @Override
                                    public void onSuccess() {

                                    }

                                    @Override
                                    public void onError() {
                                        Picasso.with(getApplicationContext()).load(user.getImageThumbnail())

                                                .placeholder(R.mipmap.default_avator)
                                                .into(mCircleImageView);
                                    }
                                });
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

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
