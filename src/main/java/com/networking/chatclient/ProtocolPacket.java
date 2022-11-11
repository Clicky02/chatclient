package com.networking.chatclient;

import java.net.Socket;
import java.util.ArrayList;

public class ProtocolPacket {
    final static String CRLF = "\r\n";
    final static String END = "\0";
    protected String command = "";
    protected ArrayList<String> parameters = new ArrayList<String>();

    protected void setCommand(String command) {
        this.command = command;
    }

    protected void addParameter(Object parameter) {
        this.parameters.add(parameter.toString());
    }

    public String getCommand() {
        return command;
    }

    public ArrayList<String> getParameters() {
        return parameters;
    }

    protected String getContent() {
        StringBuilder packetBuilder = new StringBuilder(this.command + CRLF);

        for (int i = 0; i < this.parameters.size(); i++) {
            packetBuilder.append(this.parameters.get(i) + CRLF);
        }

        packetBuilder.append(END);

        return packetBuilder.toString();
    }

    public void send(Socket socket) {
        System.out.println(getContent());
    }
}
