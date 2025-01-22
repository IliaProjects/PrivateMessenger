package com.example.messenger;


import com.stfalcon.chatkit.commons.models.IUser;

import java.io.Serializable;

public class User implements Serializable, IUser {
    private static String myUsername;

    public static synchronized String getMyUsername(){
        return myUsername;
    }

    public static synchronized void setMyUsername(String myUsername){
        User.myUsername = myUsername;
    }

    private String username;

    public User(String name) {
        this.username = name;
    }

    @Override
    public String getId() {
        return username;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public String getAvatar() {
        return null;
    }
}
