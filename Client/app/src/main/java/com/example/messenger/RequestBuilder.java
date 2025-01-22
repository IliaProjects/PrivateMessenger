package com.example.messenger;

import android.util.Base64;

public class RequestBuilder {
    private String request;
    private String param;
    private String msg;

    public RequestBuilder() {
        this.request = "chat";
        this.param = "";
        this.msg = "";
    }

    public RequestBuilder putRequest(String request){
        this.request = request;
        return this;
    }

    public RequestBuilder addParam(String param){
        this.param += "&" + param;
        return this;
    }

    public RequestBuilder putMessage(String msg){
        this.msg = msg;
        return this;
    }

    public String build (){
        String result = "@" + this.request + "%" + this.param + "#" + this.msg;
        String encryptedResult = "@" + AES256.encrypt(result);
        return encryptedResult;
    }
}
