package com.me.njerucyrus.chatapp;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TableLayout;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mUsersRef;
    private Toolbar mToolbar;
    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar)findViewById(R.id.main_page_toolbar);
        //set the toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Chat App");

        mViewPager = (ViewPager)findViewById(R.id.mainTabPager);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout = (TabLayout)findViewById(R.id.main_page_tab_layout);
        mTabLayout.setupWithViewPager(mViewPager);
        //initialize firebase auth
        mAuth = FirebaseAuth.getInstance();

        mCurrentUser = mAuth.getCurrentUser();
       mUsersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mCurrentUser ==null){
            updateUI();
        }else{
            mUsersRef.child("online").setValue("true");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        long timestamp = System.currentTimeMillis();
        String serverTime = ""+timestamp;

        mUsersRef.child("online").setValue(serverTime);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         super.onOptionsItemSelected(item);
         int id = item.getItemId();
         if (id == R.id.menu_logout){
             mAuth.signOut();
             updateUI();
         }
         else if(id==R.id.menu_settings){
            //take me to account settings
             startActivity(new Intent(MainActivity.this, SettingsActivity.class));
         }
         else if (id == R.id.menu_all_users){
            //take me to all users activity
             startActivity(new Intent(MainActivity.this, UsersActivity.class));
         }


         return true;
    }

    private void updateUI() {
        startActivity(new Intent(MainActivity.this, StartActivity.class));
        finish();
    }
}
