package com.me.njerucyrus.chatapp;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{


    private List<Message> mMessageList;
    private DatabaseReference mUserDatabase;

    public MessageAdapter(List<Message> mMessageList) {

        this.mMessageList = mMessageList;

    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_single_message_layout ,parent, false);

        return new MessageViewHolder(v);

    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        public TextView messageText;
        public CircleImageView profileImage;
        public ImageView mImageMessageView;
        public TextView displayName;
        public RelativeLayout mMessageContainer;
        public ImageView messageImage;
        public TextView messageTime;
        public RelativeLayout.LayoutParams params;

        View mView;
        public MessageViewHolder(View view) {
            super(view);
            mView = view;
            messageText = (TextView) view.findViewById(R.id.message_single_text);
            profileImage = (CircleImageView) view.findViewById(R.id.message_user_icon);
            mMessageContainer = (RelativeLayout)view.findViewById(R.id.message_layout_container);
            messageTime = (TextView) view.findViewById(R.id.message_time);
            mImageMessageView = (ImageView)view.findViewById(R.id.image_message);
            params = (RelativeLayout.LayoutParams)mMessageContainer.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            //            displayName = (TextView) view.findViewById(R.id.name_text_layout);
//            messageImage = (ImageView) view.findViewById(R.id.message_image_layout);

        }
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder viewHolder, int i) {

        final Message c = mMessageList.get(i);

        final String from_user = c.getFrom();
        final String messageKey = c.getMessageKey();
        final String messageToKey = c.getTo();
        String message_type = c.getType();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (from_user.equals(currentUser.getUid())){


            viewHolder.mMessageContainer.setBackgroundResource(R.drawable.message_text_background);

            viewHolder.messageText.setTextColor(Color.WHITE);
            viewHolder.messageTime.setTextColor(Color.WHITE);
            viewHolder.profileImage.setVisibility(View.INVISIBLE);
            viewHolder.mMessageContainer.setGravity(Gravity.END);
            viewHolder.mMessageContainer.setLayoutParams(viewHolder.params);
        }else{
            viewHolder.profileImage.setVisibility(View.VISIBLE);
            viewHolder.mMessageContainer.setBackgroundResource(R.drawable.message_text_background_receiver);
            viewHolder.messageText.setTextColor(Color.BLACK);
            viewHolder.messageTime.setTextColor(Color.BLACK);

            viewHolder.messageTime.setCompoundDrawablesWithIntrinsicBounds(null,null,null,null);

        }

//        viewHolder.mView.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                CharSequence options[] = new CharSequence[]{"Delete this message"};
//                AlertDialog.Builder builder = new AlertDialog.Builder(viewHolder.mView.getContext());
//
//                builder.setItems(options, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, final int i) {
//                        if (i == 0) {
//
//                            final FirebaseUser mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
//                            //delete your own message
//                            DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference()
//                                        .child("Messages").child(mCurrentUser.getUid()).child(messageToKey)
//                                        .child(messageKey);
//
//                            messagesRef.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
//                                @Override
//                                public void onComplete(@NonNull Task<Void> task) {
//                                    if (task.isSuccessful()){
//                                        Toast.makeText(viewHolder.mView.getContext(), "Message deleted by "+mCurrentUser.getUid()+" From "+from_user, Toast.LENGTH_LONG).show();
//                                    }
//                                }
//                            });
//                        }
//                    }
//                });
//
//                builder.show();
//                return true;
//            }
//        });
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(from_user);


        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                User user  = dataSnapshot.getValue(User.class);


//                viewHolder.displayName.setText(name);

                Picasso.with(viewHolder.profileImage.getContext()).load(user.getImageThumbnail())
                        .placeholder(R.mipmap.default_avator).into(viewHolder.profileImage);



            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if(message_type.equals("text")) {

            viewHolder.messageText.setText(c.getMessage());
            viewHolder.mImageMessageView.setVisibility(View.INVISIBLE);


        }

        if (message_type.equals("image")) {

            viewHolder.messageText.setVisibility(View.INVISIBLE);

            Picasso.with(viewHolder.mView.getContext()).load(c.getMessage())
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .placeholder(R.mipmap.default_avator)
                    .into(viewHolder.mImageMessageView, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError() {
                            Picasso.with(viewHolder.mImageMessageView.getContext()).load(c.getMessage())
                                    .placeholder(R.mipmap.default_avator).into(viewHolder.mImageMessageView);


                        }
                    });

        }

    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }


}