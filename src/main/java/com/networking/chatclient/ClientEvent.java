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

    public ClientEvent() {
        this.lockingObject = this;
    }

    public ClientEvent(Object lockingObject) {
        this.lockingObject = lockingObject;
    }

    public T waitForEvent() {
        synchronized (lockingObject) {
            try {
                lockingObject.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
            return eventParameter;
        }
    }

    public int onEvent(Consumer<T> eventFunction) {
        synchronized (lockingObject) {
            eventFunctions.put(currentFunctionId, eventFunction);
            return currentFunctionId++;
        }
    }

    public void invoke(T eventParameter) {
        synchronized (lockingObject) {
            this.eventParameter = eventParameter;
            lockingObject.notifyAll();
            for (Consumer<T> eventFunction : eventFunctions.values()) {
                eventFunction.accept(eventParameter);
            }
        }
    }
}