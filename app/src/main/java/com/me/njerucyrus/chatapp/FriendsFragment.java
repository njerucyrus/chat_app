package com.me.njerucyrus.chatapp;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {

    private RecyclerView mFriendsRecylerView;
    private DatabaseReference mFriendsRef;
    private DatabaseReference mUsersRef;
    private FirebaseUser mCurrentUser;
    private View mView;
    private FirebaseRecyclerAdapter<Friend, FriendsFragment.FriendsViewHolder> firebaseRecyclerAdapter;


    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_friends, container, false);
        mFriendsRecylerView = (RecyclerView) mView.findViewById(R.id.friendsRecyclerView);
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mFriendsRef = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrentUser.getUid());
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        mFriendsRecylerView.setHasFixedSize(true);
        mFriendsRecylerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Query query = mFriendsRef;
        query.keepSynced(true);


        FirebaseRecyclerOptions<Friend> options =
                new FirebaseRecyclerOptions.Builder<Friend>()
                        .setQuery(query, Friend.class)
                        .build();

        firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<Friend, FriendsFragment.FriendsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final FriendsViewHolder holder, int position, @NonNull Friend model) {
                        final String userKey = getRef(position).getKey();
                        //run query to get the user detail
                        mUsersRef.child(userKey).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot != null) {
                                    final User userFriend = dataSnapshot.getValue(User.class);
                                    holder.setDisplayName(dataSnapshot.child("displayName").getValue().toString());
                                    holder.setImageAvator(userFriend.getImageThumbnail());
                                    holder.setStatus(userFriend.getStatus());
                                    holder.setOnlineIcon(userFriend.getOnline());

                                    holder.mView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            CharSequence options[] = new CharSequence[]{"View Profile", "Send Message"};
                                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    if (i == 0) {
                                                        startActivity(new Intent(getContext(), UserProfileActivity.class)
                                                                .putExtra("user_uid", userKey));
                                                    } else if (i == 1) {
                                                        //got to chat activity
                                                        startActivity(new Intent(mView.getContext(), ChatActivity.class)
                                                                .putExtra("user_uid", userKey)
                                                                .putExtra("displayName", userFriend.getDisplayName())
                                                        );
                                                    }
                                                }
                                            });

                                         builder.show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });





                    }


                    @Override
                    public FriendsFragment.FriendsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.single_user_layout, parent, false);

                        return new FriendsFragment.FriendsViewHolder(view);

                    }

                };
        mFriendsRecylerView.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {
        View mView;

        public FriendsViewHolder(View itemView) {
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

    @Override
    public void onStop() {
        super.onStop();
        firebaseRecyclerAdapter.stopListening();
    }
}
