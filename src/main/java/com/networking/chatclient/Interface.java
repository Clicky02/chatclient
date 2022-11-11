package com.networking.chatclient;

public abstract class Interface implements Runnable {
    final WebClient client;

    public Interface(WebClient client) {
        this.client = client;
    }

}
