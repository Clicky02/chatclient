package com.networking.chatclient;

import java.util.HashMap;
import java.util.function.Consumer;

/*
 * A general class used for client events.
 * 
 * Code can interact with these events in 2 ways. It can "listen" to the event 
 * (have a function be called when the event occurs), or it can wait for the 
 * event (block until the event occurs).
 */
public class ClientEvent<T> {

    T eventParameter;

    Object lockingObject;
    int currentFunctionId = 0;
    HashMap<Integer, Consumer<T>> eventFunctions = new HashMap<Integer, Consumer<T>>();

    public ClientEvent(Object lockingObject) {
        this.lockingObject = lockingObject;
    }

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
        for (Consumer<T> eventFunction : eventFunctions.values()) {
            eventFunction.accept(eventParameter);
        }

    }
}