package com.networking.chatclient;

public abstract class UserInterface implements Runnable {
    final ChatClient client;

    public UserInterface(ChatClient client) {
        this.client = client;
    }

}
