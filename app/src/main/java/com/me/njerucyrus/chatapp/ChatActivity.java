package com.me.njerucyrus.chatapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class ChatActivity extends AppCompatActivity {

    private String mChatUserId;
    private Toolbar mToolbar;
    private DatabaseReference mUsersRef;
    private DatabaseReference mRootRef;
    private FirebaseUser mCurrentUser;
    private ActionBar ab;
    private TextView mTitle;
    private TextView mLastSeen;
    private CircleImageView mCircleImageView;
    private ImageButton mButtonChatAdd;
    private ImageButton mButtonChatSend;
    private EditText mEditTextMessage;
    private RecyclerView mMessageList;
    private SwipeRefreshLayout mSwipeLayout;
    private final List<Message> messageList = new ArrayList<>();
    private LinearLayoutManager mLayoutManager;
    private MessageAdapter mAdapter;

    //CHAT PAGINATION
    private final static int PAGE_ITEMS = 10;
    private int CURRENT_PAGE = 1;
    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevKey = "";

    //SEND IMAGE VARS
    private static final int GALLERY_PICK = 1;

    private StorageReference mStorageRef;

    //last seen
    private long delay = 1000; // 1 seconds after user stops typing
    private long last_text_edit = 0;

    private Runnable input_finish_checker;
    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mToolbar = (Toolbar) findViewById(R.id.chat_toolbar);
        setSupportActionBar(mToolbar);
        ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowCustomEnabled(true);
        mChatUserId = getIntent().getStringExtra("user_uid");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        mStorageRef = FirebaseStorage.getInstance().getReference("image_messages");


        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = inflater.inflate(R.layout.chat_custom_bar, null);
        ab.setCustomView(actionBarView);

        mButtonChatAdd = (ImageButton) findViewById(R.id.chat_add_btn);
        mButtonChatSend = (ImageButton) findViewById(R.id.chat_send_btn);
        mEditTextMessage = (EditText) findViewById(R.id.chat_message_view);

        //variables for dispaying messages
        mAdapter = new MessageAdapter(messageList);

        mMessageList = (RecyclerView) findViewById(R.id.messages_list);
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.message_swipe_layout);

        mLayoutManager = new LinearLayoutManager(this);
        mMessageList.setHasFixedSize(true);
        mMessageList.setLayoutManager(mLayoutManager);

        mMessageList.setAdapter(mAdapter);
        loadMessages();


        mTitle = (TextView) findViewById(R.id.custom_bar_title);
        mTitle.setText(getIntent().getStringExtra("displayName"));
        mLastSeen = (TextView) findViewById(R.id.custom_bar_last_seen);
        mCircleImageView = (CircleImageView) findViewById(R.id.custom_chat_toolbar_user_icon);
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mChatUserId);

        mUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    String online = dataSnapshot.child("online").getValue().toString();
                    final String thumbnailUrl = dataSnapshot.child("imageThumbnail").getValue().toString();
                    Picasso.with(ChatActivity.this)
                            .load(thumbnailUrl)
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.mipmap.default_avator)
                            .into(mCircleImageView, new Callback() {
                                @Override
                                public void onSuccess() {
                                    //do nothing since file retrieved locally
                                }

                                @Override
                                public void onError() {
                                    //fetch file from the online db
                                    Picasso.with(ChatActivity.this)
                                            .load(thumbnailUrl)
                                            .placeholder(R.mipmap.default_avator)
                                            .into(mCircleImageView);
                                }
                            });
                    if (online.equals("true")) {
                        mLastSeen.setText(getResources().getString(R.string.online));

                    } else {

                        long timeAgo = Long.parseLong(dataSnapshot.child("online").getValue().toString());
                        GetTimeAgo ago = new GetTimeAgo();
                        String lastSeen = ago.getTimeAgo(timeAgo, getApplicationContext());
                        mLastSeen.setText(lastSeen);


                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRootRef.child("Chats").child(mCurrentUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(mChatUserId)) {
                    Map chatMap = new HashMap();
                    chatMap.put("seen", false);
                    chatMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chats/" + mCurrentUser.getUid() + "/" + mChatUserId, chatMap);
                    chatUserMap.put("Chats/" + mChatUserId + "/" + mCurrentUser.getUid(), chatMap);
                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Log.d("CHAT_LOG", databaseError.getMessage());
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //add event listener to send message btn
        mButtonChatSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
     if(messageList.size() == 0){
         mSwipeLayout.setRefreshing(false);
     }
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                CURRENT_PAGE++;

                itemPos = 0;
                loadMoreMessages();
            }

        });

        mButtonChatAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(ChatActivity.this);
            }
        });

        // is typing feature


        handler = new Handler();

        input_finish_checker = new Runnable() {
            public void run() {
                if (System.currentTimeMillis() > (last_text_edit + delay - 500)) {

                    mRootRef.child("Messages").child(mCurrentUser.getUid()).child("typing").setValue(false);
                }
            }
        };

        mEditTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!TextUtils.isEmpty(charSequence)) {
                    //set typing true

                    mRootRef.child("Messages").child(mCurrentUser.getUid()).child("typing").setValue(true);

                } else {
                    //set typing false
                    mRootRef.child("Messages").child(mCurrentUser.getUid()).child("typing").setValue(false);
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (!TextUtils.isEmpty(charSequence)) {
                    //set typing true

                    mRootRef.child("Messages").child(mCurrentUser.getUid()).child("typing").setValue(true);

                } else {
                    //set typing false
                    mRootRef.child("Messages").child(mCurrentUser.getUid()).child("typing").setValue(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (editable.length() > 0) {
                    last_text_edit = System.currentTimeMillis();
                    handler.postDelayed(input_finish_checker, delay);
                }
            }
        });

        //typing feature
        mRootRef.child("Messages").child(mChatUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild("typing")) {
                            String typing = dataSnapshot.child("typing").getValue().toString();
                            if (typing.equals("true")) {
                                mLastSeen.setText(getResources().getString(R.string.typing));
                            } else {
                                mLastSeen.setText("online");
                                //show status last seen.
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //mLastSeen.setText(getResources().getString(R.string.online));
                    }
                });
        //end of is typing feature


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
                        .start(ChatActivity.this);
            }

            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    Uri resultUri = result.getUri();


                    File thumbnailFilePath = new File(resultUri.getPath());

                    Bitmap thumbnailBitmap = new Compressor(ChatActivity.this)
                            .setMaxHeight(200)
                            .setMaxWidth(200)
                            .setQuality(75)
                            .compressToBitmap(thumbnailFilePath);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    final byte[] thumbnailBytes = baos.toByteArray();

                    UploadTask uploadTask = mStorageRef.child(mCurrentUser.getUid()).putBytes(thumbnailBytes);
                    uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumbnailTask) {
                            String thumbnailUrl = thumbnailTask.getResult().getDownloadUrl().toString();
                            if (thumbnailTask.isSuccessful()) {
                                //send message here
                                sendImage(thumbnailUrl);
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
    protected void onStop() {
        super.onStop();
        mRootRef.child("Messages").child(mCurrentUser.getUid()).child("typing").setValue(false);
    }

    private void loadMessages() {
        DatabaseReference messagesRef = mRootRef.child("Messages").child(mCurrentUser.getUid()).child(mChatUserId);
        Query messageQuery = messagesRef.limitToLast(CURRENT_PAGE * PAGE_ITEMS);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot !=null) {
                    Message message = dataSnapshot.getValue(Message.class);
                    itemPos++;
                    String messageKey = dataSnapshot.getKey();
                    messageList.add(message);
                    if (itemPos == 1) {
                        mLastKey = messageKey;
                        mPrevKey = messageKey;
                    }

                    mAdapter.notifyDataSetChanged();
                    mMessageList.scrollToPosition(messageList.size() - 1);
                    mSwipeLayout.setRefreshing(false);
                }else{
                    mSwipeLayout.setRefreshing(false);
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                loadMoreMessages();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void loadMoreMessages() {
        DatabaseReference messagesRef = mRootRef.child("Messages").child(mCurrentUser.getUid()).child(mChatUserId);
        Query messageQuery = messagesRef.orderByKey().endAt(mLastKey).limitToLast(PAGE_ITEMS);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                if (dataSnapshot != null) {

                    Message message = dataSnapshot.getValue(Message.class);

                    String messageKey = dataSnapshot.getKey();
                    if (!mPrevKey.equals(messageKey)) {
                        messageList.add(itemPos++, message);
                    } else {
                        mPrevKey = mLastKey;

                    }

                    if (itemPos == 1) {
                        mLastKey = messageKey;
                    }

                    mAdapter.notifyDataSetChanged();
                    mSwipeLayout.setRefreshing(false);
                }else{
                    mSwipeLayout.setRefreshing(false);
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //method to send message
    private void sendMessage() {
        String message = mEditTextMessage.getText().toString();
        if (!TextUtils.isEmpty(message)) {
            //define the key for chatmessage map
            String current_user_ref = "Messages/" + mCurrentUser.getUid() + "/" + mChatUserId;
            String chat_user_ref = "Messages/" + mChatUserId + "/" + mCurrentUser.getUid();

            //get push key for message item
            DatabaseReference user_message_push = mRootRef.child("Messages")
                    .child(mCurrentUser.getUid())
                    .child(mChatUserId).push();
            String message_push_id = user_message_push.getKey();

            //create a map for message item
            Map messageMap = new HashMap();
            messageMap.put("messageKey", message_push_id);
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("from", mCurrentUser.getUid());
            messageMap.put("to", mChatUserId);
            messageMap.put("time", ServerValue.TIMESTAMP);

            //Create another map to add data to each users chat thread
            Map chatUserMessageMap = new HashMap();
            chatUserMessageMap.put(current_user_ref + "/" + message_push_id, messageMap);
            chatUserMessageMap.put(chat_user_ref + "/" + message_push_id, messageMap);
            mEditTextMessage.setText("");

            mRootRef.updateChildren(chatUserMessageMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Log.d("SEND_MSG_CHAT_LOG", databaseError.getMessage());
                    }
                }
            });

        }

    }

    private void sendImage(String thumbnailUrl) {
        //define the key for chatmessage map
        String current_user_ref = "Messages/" + mCurrentUser.getUid() + "/" + mChatUserId;
        String chat_user_ref = "Messages/" + mChatUserId + "/" + mCurrentUser.getUid();

        //get push key for message item
        DatabaseReference user_message_push = mRootRef.child("Messages")
                .child(mCurrentUser.getUid())
                .child(mChatUserId).push();
        String message_push_id = user_message_push.getKey();

        //create a map for message item
        Map messageMap = new HashMap();

        messageMap.put("messageKey", message_push_id);
        messageMap.put("message", thumbnailUrl);
        messageMap.put("seen", false);
        messageMap.put("type", "image");
        messageMap.put("from", mCurrentUser.getUid());
        messageMap.put("to", mChatUserId);
        messageMap.put("time", ServerValue.TIMESTAMP);

        //Create another map to add data to each users chat thread
        Map chatUserMessageMap = new HashMap();
        chatUserMessageMap.put(current_user_ref + "/" + message_push_id, messageMap);
        chatUserMessageMap.put(chat_user_ref + "/" + message_push_id, messageMap);
        mEditTextMessage.setText("");

        mRootRef.updateChildren(chatUserMessageMap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Log.d("SEND_MSG_CHAT_LOG", databaseError.getMessage());
                }
            }
        });
    }


}
