package com.networking.chatclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.networking.chatclient.WebClient.Group;
import com.networking.chatclient.WebClient.Message;

public class CommandLineInterface extends UserInterface {

    /*
     * Class for each user command
     * 
     * Takes in the command, how many arguments it accepts (including the command),
     * and a function that runs when the command is typed in by the user.
     */

    public class Command {
        String name;
        int minimumArgumentCount;
        Function<ArrayList<String>, Boolean> commandFunction;
        boolean mustBeJoined = true;

        public Command(String name, int minimumArgumentCount, Function<ArrayList<String>, Boolean> commandFunction) {
            this.name = name;
            this.minimumArgumentCount = minimumArgumentCount;
            this.commandFunction = commandFunction;
        }

        public Command(String name, int minimumArgumentCount, Function<ArrayList<String>, Boolean> commandFunction,
                boolean mustBeJoined) {
            this.name = name;
            this.minimumArgumentCount = minimumArgumentCount;
            this.commandFunction = commandFunction;
            this.mustBeJoined = mustBeJoined;
        }

        public boolean run(WebClient client, ArrayList<String> args) {
            if (mustBeJoined && !client.joined) {
                System.out.println("You must have connected to and joined a sever to perform this command.");
                System.out.println("Run the command \"connect <host> <port>\" to connect to a server.");
                System.out.println("Run the command \"join <username>\" to join a server.");
                return false;
            }

            if (args.size() < minimumArgumentCount) {
                System.out.println(name + " requires at least" + minimumArgumentCount + " arguments.");
                return false;
            }

            return commandFunction.apply(args);
        }
    }

    /*
     * The list of all commands that can be used.
     */
    Command[] commands = {
            new Command("help", 1, (args) -> {
                System.out.print("Available Commands: ");
                for (int i = 0; i < this.commands.length; i++) {
                    Command c = this.commands[i];
                    System.out.print(c.name);
                    if (i != this.commands.length - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.print("\n");
                return true;
            }, false),

            new Command("connect", 3, (args) -> {
                if (client.socket != null && client.socket.isConnected()) {
                    System.out.println("You are already connected to a server.");
                    return false;
                }

                try {
                    String host = args.get(1);
                    int port = Integer.parseInt(args.get(2));
                    client.connect(host, port);
                    return true;
                } catch (NumberFormatException e) {
                    System.out.println("The port must be an integer.");
                    return false;
                } catch (Exception e) {
                    System.out.println("Error Connecting.");
                    return false;
                }
            }, false),

            new Command("join", 2, (args) -> {
                if (client.socket == null || !client.socket.isConnected()) {
                    System.out.println("You must first connect to a server.");
                    System.out.println("Run the command \"connect <host> <port>\" to connect to a server.");
                    return false;
                }

                String username = args.get(1);
                if (client.join(username)) {
                    System.out.println("Joined server.");
                } else {
                    System.out.println("Invalid username.");
                }

                return true;
            }, false),

            new Command("post", 3, (args) -> {
                String subject = args.get(1);
                String content = args.get(2);
                client.postMessage(0, subject, content);
                System.out.println("Message Posted.");
                return true;
            }),

            new Command("users", 1, (args) -> {
                Group g = client.retrieveGroupUsers(0);

                System.out.print("Users for global group: ");
                for (int i = 0; i < g.users.size(); i++) {
                    System.out.print(g.users.get(i));
                    if (i != g.users.size() - 1) {
                        System.out.print(", ");
                    }
                    System.out.print("\n");
                }
                return true;
            }),

            // TODO : What?
            new Command("leave", 1, (args) -> {
                for (int id : client.userGroups) {
                    client.leaveGroup(id);
                }
                client.joined = false;
                System.out.println("Left server.\n");
                return true;
            }),

            new Command("message", 2, (args) -> {
                int messageId = getMessageIdFromArgument(0, args.get(1));
                if (messageId == -1) {
                    System.out.println("Invalid message Id.");
                    return false;
                }
                Message m = client.retrieveMessage(0, messageId);
                System.out.println("Message " + m.messageId + ": " + m.getContent());
                return true;

            }),

            new Command("exit", 1, (args) -> {
                try {
                    System.out.println("Disconnecting...");
                    client.disconnect();

                    System.exit(0);
                    return true;
                } catch (IOException e) {
                    System.out.println("Something went wrong.");
                    return false;
                }
            }, false),

            new Command("groups", 1, (args) -> {
                ArrayList<Group> groups = client.retrieveGroups();

                System.out.print("Groups: ");
                for (int i = 0; i < groups.size(); i++) {
                    Group group = groups.get(i);

                    System.out.print(group.name + " (" + group.id + ")");
                    if (i != groups.size() - 1) {
                        System.out.print(", ");
                    }
                    System.out.print("\n");
                }
                return true;
            }),

            new Command("groupjoin", 2, (args) -> {
                // client.retrieveGroups();

                int groupId = getGroupIdFromArgument(args.get(1));
                if (groupId == -1) {
                    System.out.println("Invalid Group.");
                    return false;
                }

                client.joinGroup(groupId);
                System.out.println("Joined group.");

                return true;
            }),

            new Command("grouppost", 4, (args) -> {
                // client.retrieveGroups();

                int groupId = getGroupIdFromArgument(args.get(1));
                if (groupId == -1) {
                    System.out.println("Invalid Group.\n");
                    return false;
                }

                String subject = args.get(1);
                String content = args.get(2);
                client.postMessage(groupId, subject, content);
                System.out.println("Message Posted.\n");
                return true;
            }),

            new Command("groupusers", 2, (args) -> {
                // client.retrieveGroups();

                int groupId = getGroupIdFromArgument(args.get(1));
                if (groupId == -1) {
                    System.out.println("Invalid Group.\n");
                    return false;
                }

                Group g = client.retrieveGroupUsers(groupId);

                System.out.print("Users for " + g.name + ": ");
                for (int i = 0; i < g.users.size(); i++) {
                    System.out.print(g.users.get(i));
                    if (i != g.users.size() - 1) {
                        System.out.print(", ");
                    }
                    System.out.print("\n\n");
                }

                return true;
            }),

            new Command("groupleave", 2, (args) -> {
                // client.retrieveGroups();

                int groupId = getGroupIdFromArgument(args.get(1));
                if (groupId == -1) {
                    System.out.println("Invalid Group.\n");
                    return false;
                }

                client.leaveGroup(groupId);
                System.out.println("Left group.\n");
                return true;
            }),

            new Command("groupmessage", 2, (args) -> {
                // client.retrieveGroups();

                int groupId = getGroupIdFromArgument(args.get(1));
                if (groupId == -1) {
                    System.out.println("Invalid Group.\n");
                    return false;
                }

                int messageId = getMessageIdFromArgument(groupId, args.get(1));
                if (messageId == -1) {
                    System.out.println("Invalid message Id.\n");
                    return false;
                }
                Message m = client.retrieveMessage(groupId, messageId);
                System.out.println("Message " + m.messageId + ": " + m.getContent());
                return true;

            }),

    };

    BufferedReader reader; // The buffered reader for System.in

    public CommandLineInterface(WebClient client) {
        super(client);

        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {

        client.receiveMessageLabelEvent.onEvent((payload) -> {
            System.out.print("Message Label Event");
            synchronized (this) {
                Message m = payload.labelMessage;
                System.out.print("\b\b\n");
                System.out.println("(" + m.messageId + ") " + m.subject + " " + m.username + " " + m.postDate);
                System.out.print("> ");
            }
        });

        // boolean connected = false;
        // while (!connected) {
        // try {
        // System.out.print("Enter the host: ");
        // String host = reader.readLine();

        // System.out.print("Enter the port: ");
        // int port = Integer.parseInt(reader.readLine());

        // client.connect(host, port);
        // connected = true;
        // } catch (IOException e) {
        // System.out.println("IO Exception Occured: " + e.getMessage());
        // } catch (NumberFormatException e) {
        // System.out.println("The port must be an integer.\n");
        // }
        // }

        // System.out.print("Connected!");

        // while (!client.joined) {
        // String username = getString("Enter your username: ");
        // if (!client.join(username)) {
        // System.out.println("Invalid Username. ");
        // }
        // }

        System.out.println("Enter a command or enter help to get a list of commands.");

        while (true) {
            String input = getString("> ");

            ArrayList<String> args = new ArrayList<String>();
            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(input);
            while (m.find()) {
                args.add(m.group(1).replace("\"", ""));
            }

            if (!args.isEmpty())
                runCommand(args);
        }
    }

    private synchronized void runCommand(ArrayList<String> args) {
        String commandName = args.get(0);
        for (Command command : commands) {
            if (command.name.equalsIgnoreCase(commandName)) {
                command.run(client, args);
                return;
            }
        }

        System.out.println("No command with name " + commandName + ".");
    }

    private int getGroupIdFromArgument(String argument) {
        int groupId = -1;
        try {
            groupId = Integer.parseInt(argument);

        } catch (NumberFormatException e) {
            String groupName = argument;
            for (Group g : client.groups.values()) {
                if (g.name == groupName) {
                    groupId = g.id;
                }
            }
        }

        return groupId;
    }

    // TODO : Make sure id exists
    private int getMessageIdFromArgument(int groupId, String argument) {
        int messageId = -1;
        try {
            messageId = Integer.parseInt(argument);
        } catch (NumberFormatException e) {

        }

        return messageId;
    }

    private String getString(String prompt) {
        String result = null;
        while (result == null) {
            try {
                System.out.print(prompt);
                result = reader.readLine();
            } catch (IOException e) {
                System.out.println("IO Exception Occured: " + e.getMessage());
            }
        }

        return result;
    }

}
