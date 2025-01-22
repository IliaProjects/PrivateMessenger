package com.example.messenger;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {
    public boolean onBackPressed() {
        return false;
    }
}
