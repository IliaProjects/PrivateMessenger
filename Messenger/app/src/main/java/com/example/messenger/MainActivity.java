package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.messenger.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

public class MainActivity extends BaseActivity {

    private final static String SHARED_PREFERENCES_KEY_USERNAME = "USERNAME";
    private final static String PREFERENCE_FILE_KEY = "SELF_INFO";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sharedPref = this.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        context = this;

        //setSupportActionBar(binding.toolbar);

        //NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        //appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    public void logout(){
        String request = new RequestBuilder().putRequest("LOGOUT").build();
        showProgressOverlay();

        (new Thread(new RequestRunnable(request, result -> {
            editor.putString(SHARED_PREFERENCES_KEY_USERNAME, "");
            editor.commit();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            context.startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            hideProgressOverlay();
            finish();
        }, () -> {
            editor.putString(SHARED_PREFERENCES_KEY_USERNAME, "");
            editor.commit();
            Intent intent = new Intent(MainActivity.this, StartActivity.class);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            context.startActivity(intent);
            hideProgressOverlay();
            finish();
        }))).start();
    }

    @Override
    public void onBackPressed() {
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        Fragment fragment = fragmentList.get(0).getChildFragmentManager().getPrimaryNavigationFragment();
        if (fragment != null && fragment instanceof EnterRoomMainFragment) {
            String request = new RequestBuilder().putRequest("LOGOUT").build();
            (new Thread(new RequestRunnable(request))).start();
        }

        this.finish();
        System.exit(0);
        //super.onBackPressed();
    }

    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}