package com.networking.chatclient;

public abstract class UserInterface implements Runnable {
    final WebClient client;

    public UserInterface(WebClient client) {
        this.client = client;
    }

}
