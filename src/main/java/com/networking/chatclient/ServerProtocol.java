package com.networking.chatclient;

/*
 * A namespace for types and functions for decoding server messages
 */
public final class ServerProtocol {

    public enum ServerCommand {
        VERIFY_USERNAME(1),
        USER_JOIN_NOTIF(2),
        USER_LEAVE_NOTIF(2),
        SEND_MESSAGE_LABEL(5),
        SEND_USER_LIST(1),
        SEND_GROUPS_LIST(2),
        SEND_MESSAGE_CONTENT(3),
        BAD_MESSAGE(0);

        int minParameters;

        ServerCommand(int minParameters) {
            this.minParameters = minParameters;
        }
    }

    public static ProtocolPacket parseResponse(String str) {
        ProtocolPacket packet = new ProtocolPacket();

        String[] lines = str.split(ProtocolPacket.CRLF);

        packet.setCommand(lines[0].trim());

        for (int i = 1; i < lines.length; i++) {
            packet.addParameter(lines[i].trim());
        }

        return packet;
    }

    public static ServerCommand getServerCommand(ProtocolPacket packet) {
        try {
            return ServerCommand.valueOf(packet.getCommand());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid Server Command");
            return null;
        }
    }
}
