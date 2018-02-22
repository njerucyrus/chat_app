package com.me.njerucyrus.chatapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private DatabaseReference mFriendReequestsRef;
    private DatabaseReference mRootRef;
    private DatabaseReference mFriendsRef;
    private DatabaseReference mUserProfileRef;
    private DatabaseReference mNotificationRef;
    private DatabaseReference mUsersRef;
    private FirebaseUser mCurrentUser;
    private Button mButtonSendFriendRequest;
    private Button mButtonDeclineFriendRequest;
    private TextView userProfileStatus;
    private TextView userProfileDisplayName;
    private ImageView userProfileImageView;
    private ImageView onlineIcon;
    private User userProfile;
    private int mCurrentState;
    final private static int NOT_FRIENDS = 0;
    final private static int REQUEST_SENT = 1;
    final private static int REQUEST_RECEIVED = 2;
    final private static int FRIENDS = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        userProfileImageView = (ImageView) findViewById(R.id.user_profile_image);
        userProfileDisplayName = (TextView) findViewById(R.id.user_profile_display_name);
        userProfileStatus = (TextView) findViewById(R.id.user_profile_status);
        mButtonDeclineFriendRequest = (Button) findViewById(R.id.btn_decline_friend_request);
        mButtonSendFriendRequest = (Button) findViewById(R.id.btn_send_friend_request);

        onlineIcon = (ImageView)findViewById(R.id.online_icon);


        //initialize firebase refs and user instance
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());

        mFriendReequestsRef = FirebaseDatabase.getInstance().getReference().child("FriendRequests");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFriendsRef = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        //use firebase to load status


        mUserProfileRef = FirebaseDatabase.getInstance().getReference().child("Users").child(getIntent().getStringExtra("user_uid"));

        mUserProfileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                userProfile = dataSnapshot.getValue(User.class);
                userProfileDisplayName.setText(userProfile.getDisplayName());
                userProfileStatus.setText(userProfile.getStatus());

                if (!userProfile.getImageThumbnail().equalsIgnoreCase("default")) {
                    Picasso.with(UserProfileActivity.this)
                            .load(userProfile.getImageThumbnail())
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.mipmap.default_avator)
                            .into(userProfileImageView, new Callback() {
                                @Override
                                public void onSuccess() {
                                    //do nothing since file retrieved locally
                                }

                                @Override
                                public void onError() {
                                    //fetch file from the online db
                                    Picasso.with(UserProfileActivity.this)
                                            .load(userProfile.getImageThumbnail())
                                            .placeholder(R.mipmap.default_avator)
                                            .into(userProfileImageView);
                                }
                            });
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //do nothing on error
            }
        });
        //enable offline capability for user profile query
        mUserProfileRef.keepSynced(true);



        //firebase friend request logic
        mCurrentState = NOT_FRIENDS;

        mButtonDeclineFriendRequest.setVisibility(View.INVISIBLE);
        mButtonDeclineFriendRequest.setEnabled(false);

        if (mCurrentUser.getUid().equals(getIntent().getStringExtra("user_uid"))) {

            mButtonSendFriendRequest.setEnabled(false);
            mButtonSendFriendRequest.setVisibility(View.INVISIBLE);

            mButtonDeclineFriendRequest.setVisibility(View.INVISIBLE);
            mButtonDeclineFriendRequest.setEnabled(false);

        }


        mFriendReequestsRef.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(getIntent().getStringExtra("user_uid"))) {
                    String request_type = dataSnapshot.child(getIntent().getStringExtra("user_uid"))
                            .child("request_type").getValue().toString();

                    if (request_type.equals("received")) {
                        mCurrentState = REQUEST_RECEIVED;
                        mButtonSendFriendRequest.setText(getResources().getString(R.string.accept_friend_request));
                        mButtonDeclineFriendRequest.setVisibility(View.VISIBLE);
                        mButtonDeclineFriendRequest.setEnabled(true);
                    } else if (request_type.equals("sent")) {
                        mCurrentState = REQUEST_SENT;
                        mButtonSendFriendRequest.setText(getResources().getString(R.string.cancel_friend_request));

                        mButtonDeclineFriendRequest.setVisibility(View.INVISIBLE);
                        mButtonDeclineFriendRequest.setEnabled(false);
                    }

                } else {
                    mFriendsRef.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(getIntent().getStringExtra("user_uid"))) {

                                mCurrentState = FRIENDS;
                                mButtonSendFriendRequest.setText("Unfriend this Person");
                                mButtonDeclineFriendRequest.setVisibility(View.INVISIBLE);
                                mButtonDeclineFriendRequest.setEnabled(false);

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        final String userId = getIntent().getStringExtra("user_uid");
        mButtonSendFriendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mButtonSendFriendRequest.setEnabled(false);

                if (!mCurrentUser.getUid().equals(userId)) {

                    //SEND FRIEND REQUEST IF IN NOT_FRIENDS STATE
                    if (mCurrentState == NOT_FRIENDS) {
                        DatabaseReference newNotificationRef = mRootRef.child("Notifications").push();
                        String notificationId = newNotificationRef.getKey();
                        HashMap<String, String> notificationData = new HashMap<>();
                        notificationData.put("from", mCurrentUser.getUid());
                        notificationData.put("type", "friend_request");

                        Map requestMap = new HashMap();
                        requestMap.put("FriendRequests/" + mCurrentUser.getUid() + "/" + userId + "/request_type", "sent");
                        requestMap.put("FriendRequests/" + userId + "/" + mCurrentUser.getUid() + "/request_type", "received");
                        requestMap.put("Notifications/" + userId + "/" + notificationId, notificationData);
                        mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    //show error
                                    Toast.makeText(getApplicationContext(), "Error occurred while sending friend request",
                                            Toast.LENGTH_LONG
                                    ).show();
                                    mButtonSendFriendRequest.setEnabled(true);
                                } else {
                                    Toast.makeText(getApplicationContext(), "Friend request sent",
                                            Toast.LENGTH_LONG).show();
                                    mButtonSendFriendRequest.setEnabled(true);
                                    mButtonSendFriendRequest.setText(getResources().getString(R.string.cancel_friend_request));
                                    mCurrentState = REQUEST_SENT;
                                }
                            }
                        });

                    }

                    //CANCEL FRIEND REQUEST IF REQUEST WAS SENT

                    if (mCurrentState == REQUEST_SENT) {
                        mFriendReequestsRef.child(mCurrentUser.getUid()).child(userId).removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        mFriendReequestsRef.child(userId).child(mCurrentUser.getUid())
                                                .removeValue()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        mCurrentState = NOT_FRIENDS;
                                                        mButtonSendFriendRequest.setEnabled(true);
                                                        mButtonSendFriendRequest.setText(getResources().getString(R.string.btn_send_friend_request));
                                                        mButtonDeclineFriendRequest.setVisibility(View.INVISIBLE);

                                                    }
                                                });
                                    }
                                });
                    }

                    //ACCEPT FRIEND IF IN REQUEST  FRIEND STATE

                    if (mCurrentState == REQUEST_RECEIVED) {


                        final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                        Map friendsMap = new HashMap();
                        friendsMap.put("Friends/" + mCurrentUser.getUid() + "/" + userId + "/date", currentDate);
                        friendsMap.put("Friends/" + userId + "/" + mCurrentUser.getUid() + "/date", currentDate);


                        friendsMap.put("FriendRequests/" + mCurrentUser.getUid() + "/" + userId, null);
                        friendsMap.put("FriendRequests/" + userId + "/" + mCurrentUser.getUid(), null);


                        mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {


                                if (databaseError == null) {

                                    mButtonSendFriendRequest.setEnabled(true);
                                    mCurrentState = FRIENDS;
                                    mButtonSendFriendRequest.setText("Unfriend this Person");

                                    mButtonDeclineFriendRequest.setVisibility(View.INVISIBLE);
                                    mButtonDeclineFriendRequest.setEnabled(false);


                                } else {

                                    String error = databaseError.getMessage();

                                    Toast.makeText(UserProfileActivity.this, error, Toast.LENGTH_SHORT).show();


                                }

                            }
                        });
                    }

                    //UN FRIEND IF IN  FRIENDS STATE
                    if (mCurrentState == FRIENDS) {
                        Map unFriendRequest = new HashMap();
                        unFriendRequest.put("Friends/"+mCurrentUser.getUid()+"/"+userId, null);
                        unFriendRequest.put("Friends/"+userId+"/"+mCurrentUser.getUid(), null);

                        mRootRef.updateChildren(unFriendRequest, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError ==null){
                                    mCurrentState = NOT_FRIENDS;
                                    mButtonSendFriendRequest.setEnabled(true);
                                    mButtonSendFriendRequest.setText(getResources().getString(R.string.btn_send_friend_request));
                                    mButtonDeclineFriendRequest.setVisibility(View.INVISIBLE);
                                }else{
                                    Toast.makeText(getApplicationContext(), "An error occurred", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    }


                } else {
                    Toast.makeText(getApplicationContext(),
                            "Cannot send friend request to yourself",
                            Toast.LENGTH_LONG
                    ).show();
                    mButtonSendFriendRequest.setEnabled(true);
                }
            }
        });

        mButtonDeclineFriendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //decline
                if (mCurrentState == REQUEST_RECEIVED) {
                    Map declineFriendRequestMap = new HashMap();

                    declineFriendRequestMap.put("FriendRequests/" + mCurrentUser.getUid() + "/" + userId, null);
                    declineFriendRequestMap.put("FriendRequests/" + userId + "/" + mCurrentUser.getUid(), null);
                    mRootRef.updateChildren(declineFriendRequestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError == null){
                                mButtonDeclineFriendRequest.setEnabled(false);
                                mButtonDeclineFriendRequest.setVisibility(View.INVISIBLE);
                                mCurrentState = NOT_FRIENDS;
                                mButtonSendFriendRequest.setText(getResources().getString(R.string.btn_send_friend_request));
                                Toast.makeText(getApplicationContext(), "Request declined!", Toast.LENGTH_SHORT).show();

                            }else{
                                Toast.makeText(getApplicationContext(), "Error occurred", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();

    }



    @Override
    protected void onStop() {
        super.onStop();



    }
}