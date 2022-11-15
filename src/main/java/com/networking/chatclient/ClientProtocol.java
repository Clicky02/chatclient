package com.networking.chatclient;

public final class ClientProtocol {
    public enum MessageAction {
        POST,
        RETRIEVE
    }

    public enum GroupAction {
        JOIN,
        LEAVE,
        USERS,
        LIST
    }

    public static ProtocolPacket createJoinPacket(String username) {
        ProtocolPacket packet = new ProtocolPacket();
        packet.setCommand("JOIN");
        packet.addParameter(username);
        return packet;
    }

    public static ProtocolPacket createLeavePacket() {
        ProtocolPacket packet = new ProtocolPacket();
        packet.setCommand("LEAVE");
        return packet;
    }

    public static ProtocolPacket createMessagePacket(MessageAction action, int groupId, int messageId,
            String subject, String content) {
        ProtocolPacket packet = new ProtocolPacket();
        packet.setCommand("MESSAGE");
        packet.addParameter(action);
        packet.addParameter(groupId);
        packet.addParameter(messageId);
        packet.addParameter(subject);
        packet.addParameter(content);
        return packet;
    }

    public static ProtocolPacket createGroupPacket(GroupAction action, int groupId) {
        ProtocolPacket packet = new ProtocolPacket();
        packet.setCommand("GROUP");
        packet.addParameter(action);
        packet.addParameter(groupId);
        return packet;
    }

    public static ProtocolPacket createDisconnectPacket() {
        ProtocolPacket packet = new ProtocolPacket();
        packet.setCommand("DISCONNECT");
        return packet;
    }
}
