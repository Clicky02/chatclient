package com.networking.chatclient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/*
 * A structure that holds the information for one packet.
 * 
 * Can be used for packets coming from or going to the server. The packet is 
 * structured like so that the first line is the command, and each subsequent 
 * line is a parameter of the command. Lines are ended by the CRLF constant, 
 * and the packet is ended by the END constant (a null terminator).
 */
public class ProtocolPacket {
    final static String CRLF = "/r/n";
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

    /*
     * Creates the raw String that this packet represents.
     */
    protected String getContent() {
        StringBuilder packetBuilder = new StringBuilder(this.command + CRLF);

        for (int i = 0; i < this.parameters.size(); i++) {
            packetBuilder.append(this.parameters.get(i) + CRLF);
        }

        packetBuilder.append(END);

        return packetBuilder.toString();
    }

    /*
     * Send this packet through the outputStream.
     */
    public void send(DataOutputStream outputStream) {
        try {
            outputStream.writeBytes(getContent());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
