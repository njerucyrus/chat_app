package com.me.njerucyrus.chatapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView mUsersRecyclerView;
    private DatabaseReference mRef;
    private FirebaseRecyclerAdapter<User, UsersViewHolder> firebaseRecyclerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);
        mToolbar = (Toolbar) findViewById(R.id.users_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mUsersRecyclerView = (RecyclerView) findViewById(R.id.usersRecyclerView);
        mUsersRecyclerView.setHasFixedSize(true);
        mUsersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRef = FirebaseDatabase.getInstance().getReference().child("Users");

    }

    @Override
    protected void onStart() {
        super.onStart();


        Query query = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users");
        query.keepSynced(true);


        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(query, User.class)
                        .build();

        firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<User, UsersViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull UsersViewHolder holder, int position, @NonNull final User model) {
                        holder.setDisplayName(model.getDisplayName());
                        holder.setStatus(model.getStatus());
                        holder.setImageAvator(model.getImageThumbnail());
                        holder.setOnlineIcon(model.getOnline());
                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                startActivity(new Intent(UsersActivity.this, UserProfileActivity.class)
                                        .putExtra("user_uid", model.getUserUid())
                                        .putExtra("image_thumbnail", model.getImageThumbnail())
                                        .putExtra("display_name", model.getDisplayName())
                                        .putExtra("status", model.getStatus())
                                );
                            }
                        });
                    }

                    @Override
                    public UsersViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.single_user_layout, parent, false);

                        return new UsersViewHolder(view);

                    }
                };

        mUsersRecyclerView.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseRecyclerAdapter.stopListening();



    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {
        View mView;

        public UsersViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }


        public void setDisplayName(String displayName) {
            TextView mSingleUserDisplayName = (TextView) mView.findViewById(R.id.single_user_display_name);
            mSingleUserDisplayName.setText(displayName);
        }

        public void setStatus(String status) {
            TextView mSingleUserStatus = (TextView) mView.findViewById(R.id.single_user_status);
            mSingleUserStatus.setText(status);
        }

        public void setImageAvator(final String avatorUrl) {
            final CircleImageView mCircleImageView = (CircleImageView) mView.findViewById(R.id.single_user_default_avator);
            if (!avatorUrl.equalsIgnoreCase("default")) {
                Picasso.with(mView.getContext()).load(avatorUrl)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.mipmap.default_avator)
                        .into(mCircleImageView, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError() {
                                Picasso.with(mView.getContext()).load(avatorUrl)
                                        .placeholder(R.mipmap.default_avator)
                                        .into(mCircleImageView);
                            }
                        });
            }
        }

        public void setOnlineIcon(String online) {
            ImageView onlineIcon = (ImageView) mView.findViewById(R.id.online_icon);
            if (online.equals("true")) {
                onlineIcon.setImageResource(R.mipmap.online_icon);
            } else {
                onlineIcon.setImageResource(R.mipmap.offline_icon);
            }
        }

    }
}
