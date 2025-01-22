package com.example.messenger;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inspector.IntFlagMapping;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;

import java.util.List;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public FrameLayout getProgressOverlay(){
        return findViewById(R.id.progress_overlay);
    }

    public void showProgressOverlay(){
        getProgressOverlay().setVisibility(View.VISIBLE);
    }

    public void hideProgressOverlay(){
        getProgressOverlay().setVisibility(View.INVISIBLE);
    }

    @Override
    public void onBackPressed() {

        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();

        boolean handled = false;
        for(Fragment f : fragmentList) {
            if(f instanceof BaseFragment) {
                handled = ((BaseFragment)f).onBackPressed();
                if(handled) {
                    break;
                }
            }
        }

        if(!handled) {
            super.onBackPressed();
        }
    }
}
