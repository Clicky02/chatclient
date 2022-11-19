package com.networking.chatclient;

/*
 * The base class for interface types.
 * This project has two of these: GraphicalInterface and CommandLineInterface
 */
public abstract class UserInterface implements Runnable {
    final ChatClient client;

    public UserInterface(ChatClient client) {
        this.client = client;
    }

}
