package com.networking.chatclient;

public final class ServerProtocol {

    public enum ServerCommand {
        VERIFY_USERNAME,
        USER_JOIN_NOTIF,
        USER_LEAVE_NOTIF,
        SEND_MESSAGE_LABEL,
        SEND_USER_LIST,
        SEND_GROUPS_LIST,
        SEND_MESSAGE_CONTENT,
        BAD_MESSAGE
    }

    public static ProtocolPacket parseResponse(String str) {
        ProtocolPacket packet = new ProtocolPacket();

        String[] lines = str.split(ProtocolPacket.CRLF);

        packet.setCommand(lines[0].trim());

        for (int i = 1; i < lines.length - 1; i++) {
            packet.addParameter(lines[i].trim());
        }

        return packet;
    }

    public static ServerCommand getServerCommand(ProtocolPacket packet) {
        return ServerCommand.valueOf(packet.getCommand());
    }
}
