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

    // Uses these to ensure that a second event doen't get called before everything
    // waiting for the first event had a chance to respond
    int waitingForEvent = 0;
    int waitingAfterEvent = 0;

    int currentFunctionId = 0;
    HashMap<Integer, Consumer<T>> eventFunctions = new HashMap<Integer, Consumer<T>>();

    public synchronized T waitForEvent() {
        try {
            waitingForEvent++;
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        T returnedParameter = eventParameter;

        waitingAfterEvent--;
        if (waitingAfterEvent == 0) {
            this.notify();
        }

        return returnedParameter;
    }

    public synchronized int onEvent(Consumer<T> eventFunction) {
        eventFunctions.put(currentFunctionId, eventFunction);
        return currentFunctionId++;

    }

    public synchronized void invoke(T eventParameter) {
        if (waitingAfterEvent > 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        waitingAfterEvent = waitingForEvent;
        waitingForEvent = 0;
        this.eventParameter = eventParameter;
        this.notifyAll();
        for (Consumer<T> eventFunction : eventFunctions.values()) {
            eventFunction.accept(eventParameter);
        }

    }
}