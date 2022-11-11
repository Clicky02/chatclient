package com.networking.chatclient;

import java.util.HashMap;
import java.util.function.Consumer;

public class ClientEvent<T> {

    T eventParameter;

    int currentFunctionId = 0;
    HashMap<Integer, Consumer<T>> eventFunctions = new HashMap<Integer, Consumer<T>>();

    public synchronized T waitForEvent() {
        try {
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return eventParameter;
    }

    public synchronized int onEvent(Consumer<T> eventFunction) {
        eventFunctions.put(currentFunctionId, eventFunction);
        return currentFunctionId++;
    }

    public synchronized void invoke(T eventParameter) {
        this.eventParameter = eventParameter;
        this.notifyAll();
    }
}