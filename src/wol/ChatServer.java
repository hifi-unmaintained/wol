/*
 * Copyright (c) 2012 Toni Spets <toni.spets@iki.fi>
 * 
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package wol;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import wol.ChatChannel.GameFullException;
import wol.ChatChannel.InvalidKeyException;
import wol.ChatChannel.UserBannedException;
import wol.ChatChannel.UserExistsException;
import wol.ChatChannel.UserNotOperatorException;
import static wol.ChatServer.NumericReplies.*;
import static wol.ChatChannel.ChannelFlags.*;
import wol.ChatChannel.UserNotOnChannelException;

/**
 * The Westwood Online Chat server
 *
 * @author Toni Spets
 */
public class ChatServer extends TCPServer {

    public class NumericReplies {
        final static public int RPL_LOCALE              = 309;
        final static public int RPL_LOCALESET           = 310;
        final static public int RPL_LISTSTART           = 321;
        final static public int RPL_LISTGAME            = 326;
        final static public int RPL_LIST                = 327;
        final static public int RPL_CODEPAGE            = 328;
        final static public int RPL_CODEPAGESET         = 329;
        final static public int RPL_ENDOFLIST           = 323;
        final static public int RPL_TOPIC               = 332;
        final static public int RPL_NAMREPLY            = 353;
        final static public int RPL_SQUADINFO           = 358;
        final static public int RPL_ENDOFNAMES          = 366;
        final static public int RPL_MOTDSTART           = 375;
        final static public int RPL_MOTD                = 372;
        final static public int RPL_ENDOFMOTD           = 376;
        final static public int RPL_PAGE                = 389;
        final static public int RPL_FINDUSEREX          = 398;
        final static public int ERR_NOSUCHNICK          = 401;
        final static public int ERR_NOSUCHCHANNEL       = 403;
        final static public int ERR_NONICKNAMEGIVEN     = 431;
        final static public int ERR_ERRORNEUSNICKNAME   = 432;
        final static public int ERR_NICKNAMEINUSE       = 433;
        final static public int ERR_SQUADIDNOEXIST      = 439;
        final static public int ERR_USERNOTINCHANNEL    = 441;
        final static public int ERR_NOTONCHANNEL        = 442;
        final static public int ERR_NEEDMOREPARAMS      = 461;
        final static public int ERR_ALREADYREGISTERED   = 462;
        final static public int ERR_PASSWDMISMATCH      = 464;
        final static public int ERR_CHANNELISFULL       = 471;
        final static public int ERR_BANNEDFROMCHAN      = 474;
        final static public int ERR_BADCHANNELKEY       = 475;
        final static public int ERR_CHANOPRIVSNEEDED    = 482;
    }

    Pattern ircPattern;

    HashMap<String, ChatChannel> channels;
    HashMap<String, ChatClient> clients;

    protected ChatServer(InetAddress address, int port, Selector selector) throws IOException {
        super(address, port, selector);
        ircPattern = Pattern.compile("^(:([^ ]+) )?([^ ]+) ?(.*)");

        clients = new HashMap<String, ChatClient>();
        channels = new HashMap<String, ChatChannel>();

        // Red Alert lobbies
        channels.put("#Lob_21_0", new ChatChannel("#Lob_21_0", null, "zotclot9", 21, 0, 0, false, 0, CHAN_LOBBY|CHAN_OFFICIAL|CHAN_PERMANENT));
        channels.put("#Lob_21_1", new ChatChannel("#Lob_21_1", null, "progamer", 21, 0, 0, false, 0, CHAN_LOBBY|CHAN_OFFICIAL|CHAN_PERMANENT));
        // TiberianSun lobbies
        channels.put("#Lob_18_0", new ChatChannel("#Lob_18_0", null, "zotclot9", 18, 0, 0, false, 0, CHAN_LOBBY|CHAN_OFFICIAL|CHAN_PERMANENT));
        System.out.println("ChatServer listening on " + address + ":" + port);
    }

    /**
     * Write channel user list to client
     * 
     * @param client    target client
     * @param channel   source channel
     */
    void putChannelNames(ChatClient client, ChatChannel channel) {

        ArrayList<ChatClient> clients = channel.getUsers();
        for (Iterator<ChatClient> i = clients.iterator(); i.hasNext();) {
            ChatClient c = i.next();
            // FIXME: highly inefficient, concat up to 512 bytes
            putReply(client, RPL_NAMREPLY, ((channel.getFlags() & CHAN_OFFICIAL) > 0 ? "* " : "= ") + channel.getName() + " :" + (channel.getOwner() == c ? "@" : "") + c.getNick() + ",0," + c.getLongIp());
        }

        putReply(client, RPL_ENDOFNAMES, channel.getName() + " :End of names");
    }

    /**
     * Write a server reply to client without any params
     * 
     * @param client    target client
     * @param code      RPL/ERR code
     */
    protected void putReply(ChatClient client, int code) {
        client.putString(":" + WOL.hostname + " " + code + " " + client.getNick());
    }

    /**
     * Write a server reply to client with params
     * 
     * @param client    target client
     * @param code      RPL/ERR code
     * @param params    pre-formatted params
     */
    protected void putReply(ChatClient client, int code, String params) {
        client.putString(":" + WOL.hostname + " " + code + " " + client.getNick() + " " + params);
    }

    /**
     * Write a command reply to client with params
     * 
     * @param client    target client
     * @param command   command name
     * @param params    pre-formatted params
     */
    protected void putReply(ChatClient client, String command, String params) {
        client.putString(":" + client.getNick() + "!u@h " + command + " " + params);
    }

    /**
     * Write a command reply from one client to another
     * 
     * @param from      source client
     * @param to        target client
     * @param command   command name
     * @param params    pre-formatted params
     */
    protected void putMessage(ChatClient from, ChatClient to, String command, String params) {
        to.putString(":" + from.getNick() + "!u@h " + command + " " + params);
    }

    /**
     * Write a command reply from one client to channel including the sender
     * 
     * @param channel   target channel
     * @param client    source client
     * @param command   command name
     * @param params    pre-formatted params
     */
    protected void putReplyChannel(ChatChannel channel, ChatClient client, String command, String params) {
        putReplyChannel(channel, client, command, params, false);
    }

    /**
     * Write a command reply from one client to channel
     * 
     * @param channel   target channel
     * @param client    source client
     * @param command   command name
     * @param params    pre-formatted params
     * @param skipFrom  skip source client
     */
    protected void putReplyChannel(ChatChannel channel, ChatClient client, String command, String params, boolean skipFrom) {
        String message = ":" + client.getNick() + "!u@h " + command + " " + params;
        ArrayList<ChatClient> clients = channel.getUsers();
        for (Iterator<ChatClient> i = clients.iterator(); i.hasNext();) {
            ChatClient to = i.next();
            if (!skipFrom || to != client) {
                to.putString(message);
            }
        }
    }

    /**
     * Write a raw command to client
     * 
     * @param client    target client
     * @param command   command name
     * @param params    pre-formatted params
     */
    protected void putCommand(ChatClient client, String command, String params) {
        client.putString(command + " " + params);
    }

    /**
     * Called when client sends CVERS command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onCvers(ChatClient client, String[] params) { }

    /**
     * Called when client sends PASS command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onPass(ChatClient client, String[] params) {

        if (params.length < 1) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        if (!params[0].equals("supersecret")) {
            putReply(client, ERR_PASSWDMISMATCH, ":Password incorrect ("+params[0]+")");
            client.disconnect();
            return;
        }

        client.havePassword = true;
    }

    /**
     * Called when client sends NICK command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onNick(ChatClient client, String[] params) {

        if (params.length < 1) {
            putReply(client, ERR_NEEDMOREPARAMS, "NICK :Not enough parameters");
            return;
        }

        if (params[0].length() == 0) {
            putReply(client, ERR_NONICKNAMEGIVEN, ":No nickname given");
            return;
        }

        if (params[0].length() > 9) {
            putReply(client, ERR_ERRORNEUSNICKNAME, params[0] + " :Errorneus nickname");
            return;
        }

        if (clients.containsKey(params[0])) {
            putReply(client, ERR_NICKNAMEINUSE, params[0] + " :Nickname is already in use");
            return;
        }

        client.setNick(params[0]);
    }

    /**
     * Called when client sends APGAR command (login password)
     * @param client    source client
     * @param params    params
     */
    protected void onApgar(ChatClient client, String[] params) { }

    /**
     * Called when client sends SERIAL command (cd-key)
     * <p>
     * Note: We should probably never implement this as we can't validate them
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onSerial(ChatClient client, String[] params) { }

    /**
     * Called when client sends USER command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onUser(ChatClient client, String[] params) {

        if (params.length < 4) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        if (client.registered) {
            putReply(client, ERR_ALREADYREGISTERED, ":You have already registered");
            return;
        }

        if (!client.havePassword) {
            putReply(client, ERR_PASSWDMISMATCH, ":Password incorrect");
            client.disconnect();
            return;
        }

        if (client.getNick() != null) {
            client.registered = true;
            clients.put(client.getNick(), client);

            putReply(client, RPL_MOTDSTART, ":- Welcome to Westwood Online!");
            putReply(client, RPL_ENDOFMOTD);
        }
    }

    /**
     * Called when client sends VERCHK command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onVerchk(ChatClient client, String[] params) { }

    /**
     * Called when client sends SETOPT command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onSetOpt(ChatClient client, String[] params) {
        if (params.length < 1) {
            putReply(client, ERR_NEEDMOREPARAMS, "SETOPT :Not enough parameters");
            return;
        }

        String[] options = params[0].split(",");
        if (options.length == 2) {
            client.setOptions(Integer.valueOf(options[0]), Integer.valueOf(options[1]));
        }
    }

    /**
     * Called when client sends GETCODEPAGE command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onGetCodepage(ChatClient client, String[] params) {

        if (params.length < 1) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }
        
        //FIXME: Add support for more clients which are in other parms
        if (clients.containsKey(params[0])) {
            String encoding = clients.get(params[0]).getEncoding();
            if (encoding.startsWith("Cp")) {
                putReply(client, RPL_CODEPAGE, client.getNick() + "`" + encoding.substring(2));
            } else {
                // FIXME: lie if no codepage set
                putReply(client, RPL_CODEPAGE, client.getNick() + "`1252");
            }
        } else {
            putReply(client, ERR_NOSUCHNICK, params[0] + " :No such nick");
        }
    }

    /**
     * Called when client send SETCODEPAGE command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onSetCodepage(ChatClient client, String[] params) {
        try {
            client.setEncoding("Cp" + params[0]);
            putReply(client, RPL_CODEPAGESET, params[0]);
        } catch (UnsupportedEncodingException e) {
             //FIXME: unsupported codepage error reply?
        }
    }

    protected void onGetLocale(ChatClient client, String[] params) {

        if (params.length < 1) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }
        
        //FIXME: Add support for more clients which are in other parms
        if (clients.containsKey(params[0])) {
            int locale = clients.get(params[0]).getLocale();
            if (locale != 0) {
                putReply(client, RPL_LOCALE, client.getNick() + "`" + locale);
            } else {
                // FIXME: lie if no locale set
                putReply(client, RPL_LOCALE, client.getNick() + "`0");
            }
        } else {
            putReply(client, ERR_NOSUCHNICK, params[0] + " :No such nick");
        }
    }
    /**
     * Called when client send SETLOCALE command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onSetLocale(ChatClient client, String[] params) {
        try {
            client.setLocale(Integer.valueOf(params[0]));
            putReply(client, RPL_LOCALESET, params[0]);
        } catch (Exception e) {
             //FIXME: unknown locale error reply?
            System.out.println("Unexpected exception when SetLocale");
        }
    }

    /**
     * Called when client sends LIST command (channel/lobby list)
     * @param client
     * @param params 
     */
    protected void onList(ChatClient client, String[] params) {
        if (params.length < 2) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        int listType = Integer.valueOf(params[0]);
        int gameType = Integer.valueOf(params[1]);

        putReply(client, RPL_LISTSTART);

        if (listType == 0) {
            for (Iterator<ChatChannel> i = channels.values().iterator(); i.hasNext();) {
                ChatChannel channel = i.next();
                if ((channel.getFlags() & CHAN_LOBBY|CHAN_OFFICIAL|CHAN_PERMANENT) > 0 && channel.getType() == gameType)  {
                    putReply(client, RPL_LIST, channel.getName() + " " + channel.getUsers().size() + " 0 " + channel.getFlags());
                }
            }
        }
        else if (listType == gameType) {
            for (Iterator<ChatChannel> i = channels.values().iterator(); i.hasNext();) {
                ChatChannel channel = i.next();
                if ((channel.getFlags() & CHAN_LOBBY) > 0 && (channel.getFlags() & CHAN_OFFICIAL) == 0 && channel.getType() == gameType)  {
                    putReply(client, RPL_LISTGAME, String.format(
                        "%s %d %d %d %d %d %d %d::%s",
                        channel.getName(),
                        channel.getUsers().size(),
                        channel.getMaxUsers(),
                        channel.getType(),
                        channel.getTournament() ? 1 : 0,
                        channel.getReserved(),
                        channel.getLongIp(),
                        channel.getFlags(),
                        channel.getTopic()
                    ));
                }
            }
        }

        putReply(client, RPL_ENDOFLIST);
    }

    /**
     * Called when client sends JOINGAME command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onJoinGame(ChatClient client, String[] params) {

        // normal join
        if (params.length == 3 && params[0].startsWith("#Lob_")) {
            String[] newParams = new String[2];
            newParams[0] = params[0];
            newParams[1] = params[2];
            onJoin(client, newParams);
            return;
        }

        // game join
        if (params.length == 2 || params.length == 3) {
            if (channels.containsKey(params[0])) {
                ChatChannel game = channels.get(params[0]);
                try {
                    game.join(client, params.length == 3 ? params[2] : "");
                    putReplyChannel(game, client, "JOINGAME", game.getMinUsers() + " " + game.getMaxUsers() + " " + game.getType() + " " + (game.getTournament() ? 1 : 0) + " 0 " + client.getLongIp() + " 0 " + ":" + game.getName());
                    putReply(client, RPL_TOPIC, ":" + game.getTopic());
                    putChannelNames(client, game);
                    // handle buggy RA
                    client.sentGameopt(false);
                    client.discardQueue();
                } catch(UserExistsException e) {
                    putReply(client, "JOINGAME", game.getMinUsers() + " " + game.getMaxUsers() + " " + game.getType() + " " + (game.getTournament() ? 1 : 0) + " 0 " + client.getLongIp() + " 0 " + ":" + game.getName());
                } catch(UserBannedException e) {
                    putReply(client, ERR_BANNEDFROMCHAN, game.getName() + " :Cannot join channel (banned)");
                } catch(GameFullException e) {
                    putReply(client, ERR_CHANNELISFULL, game.getName() + " :Cannot join channel (game is full)");
                } catch(InvalidKeyException e) {
                    putReply(client, ERR_BADCHANNELKEY, game.getName() + " :Cannot join channel (invalid key)");
                }
            } else {
                putReply(client, ERR_NOSUCHCHANNEL, params[0] + " :No such channel");
            }
        }

        // game create
        if (params.length < 8) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        String name = params[0];
        String minUsers = params[1];
        String maxUsers = params[2];
        String gameType = params[3];
        String tournament = params[6];
        String reserved = params[7];
        String key = params.length > 8 ? params[8] : "";

        ChatChannel game = new ChatChannel(name, client, key, Integer.valueOf(gameType), Integer.valueOf(minUsers), Integer.valueOf(maxUsers), Integer.valueOf(tournament) > 0, Integer.valueOf(reserved), CHAN_LOBBY);

        try {
            game.join(client, key);
            channels.put(name, game);
            putReply(client, RPL_TOPIC, ":");
            putReply(client, "JOINGAME", minUsers + " " + maxUsers + " " + gameType + " " + tournament + " 0 " + client.getLongIp() + " 0 " + ":" + game.getName());
            putChannelNames(client, game);
            client.sentGameopt(true);
        } catch (Exception e) {
            System.out.println("Unexpected exception when joining a fresly created channel");
        }
    }

    /**
     * Called when client sends TOPIC command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onTopic(ChatClient client, String params[]) {
        if (params.length < 2) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        if (channels.containsKey(params[0])) {
            ChatChannel channel = channels.get(params[0]);
            try {
                channel.setTopic(client, params[1]);
            } catch (UserNotOperatorException e) {
                putReply(client, ERR_CHANOPRIVSNEEDED, params[0] + " :You're not channel operator");
            }
        } else {
            putReply(client, ERR_NOSUCHCHANNEL, params[0] + " :No such channel");
        }
    }

    /**
     * Called when client sends GAMEOPT command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onGameopt(ChatClient client, String params[]) {
        if (params.length < 2) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        if (params[0].startsWith("#")) {
            if (channels.containsKey(params[0])) {
                ChatChannel channel = channels.get(params[0]);
                if (channel.getUsers().contains(client)) {
                    for (Iterator<ChatClient> i = channel.getUsers().iterator(); i.hasNext();) {
                        ChatClient current = i.next();
                        // handle buggy RA
                        if (!current.sentGameopt()) {
                            current.putQueue(":" + client.getNick() + "!u@h GAMEOPT " + channel.getName() + " :" + params[1]);
                        } else {
                            putMessage(client, current, "GAMEOPT", channel.getName() + " :" + params[1]);
                        }
                    }
                } else {
                    putReply(client, ERR_NOTONCHANNEL, params[0] + " :You're not on that channel");
                }
            } else {
                putReply(client, ERR_NOSUCHCHANNEL, params[0] + " :No such channel");
            }
        } else {
            if (clients.containsKey(params[0])) {
                ChatClient current = clients.get(params[0]);
                // handle buggy RA
                if (!current.sentGameopt()) {
                    current.putQueue(":" + client.getNick() + "!u@h GAMEOPT " + current.getNick() + " :" + params[1]);
                } else {
                    putMessage(client, current, "GAMEOPT", current.getNick() + " :" + params[1]);
                }
                client.sentGameopt(true);
                client.flushQueue();
            } else {
                putReply(client, ERR_NOSUCHNICK, params[0] + " :No such nick/channel");
            }
        }
    }

    /**
     * Called when client sends JOIN command (to join a lobby or channel)
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onJoin(ChatClient client, String[] params) {

        if (params.length < 2) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        if (channels.containsKey(params[0])) {
            ChatChannel channel = channels.get(params[0]);
            try {
                channel.join(client, params.length > 1 ? params[1] : "");
                putReplyChannel(channel, client, "JOIN", ":0," + client.getLongIp() + " " + channel.getName());
                putChannelNames(client, channel);
            } catch(UserExistsException e) {
                putReply(client, "JOIN", ":0," + client.getLongIp() + " " + channel.getName());
            } catch(UserBannedException e) {
                putReply(client, ERR_BANNEDFROMCHAN, channel.getName() + " :Cannot join channel (banned)");
            } catch(GameFullException e) {
                putReply(client, ERR_CHANNELISFULL, channel.getName() + " :Cannot join channel (game is full)");
            } catch(InvalidKeyException e) {
                putReply(client, ERR_BADCHANNELKEY, channel.getName() + " :Cannot join channel (invalid key)");
            }
        } else {
            putReply(client, ERR_NOSUCHCHANNEL, params[0] + " :No such channel");
        }
    }

    /**
     * Called when client sends PRIVMSG command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onPrivmsg(ChatClient client, String[] params) {

        if (params.length < 2) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        if (params[0].startsWith("#")) {
            if (channels.containsKey(params[0])) {
                ChatChannel channel = channels.get(params[0]);
                if (channel.getUsers().contains(client)) {
                    putReplyChannel(channel, client, "PRIVMSG", params[0] + " :" + params[1], true);
                } else {
                    putReply(client, ERR_NOTONCHANNEL, params[0] + " :You're not on that channel");
                }
            } else {
                putReply(client, ERR_NOSUCHCHANNEL, params[0] + " :No such channel");
            }
        } else {
            if (clients.containsKey(params[0])) {
                putMessage(client, clients.get(params[0]), "PRIVMSG", params[0] + " :" + params[1]);
            } else {
                putReply(client, ERR_NOSUCHNICK, params[0] + " :No such nick/channel");
            }
        }
    }

    /**
     * Called when client sends PAGE command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onPage(ChatClient client, String[] params) {

        if (params.length < 2) {
            putReply(client, ERR_NEEDMOREPARAMS, "PAGE :Not enough parameters");
            return;
        }

        if (!clients.containsKey(params[0])) {
            putReply(client, RPL_PAGE, "1 :No such nick");
            return;
        }

        ChatClient target = clients.get(params[0]);

        if (!target.canPage()) {
            putReply(client, RPL_PAGE, "1 :No such nick");
            return;
        }

        putMessage(client, target, "PAGE", params[0] + " :" + params[1]);
        putReply(client, RPL_PAGE, "0 :Ok");
    }

    /**
     * Called when client sends FINDUSEREX command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onFindUserEx(ChatClient client, String[] params) {

        if (params.length < 2) {
            putReply(client, ERR_NEEDMOREPARAMS, "FINDUSEREX :Not enough parameters");
            return;
        }

        if (!clients.containsKey(params[0])) {
            putReply(client, RPL_FINDUSEREX, "1 :No such nick (not connected)");
            return;
        }

        ChatClient target = clients.get(params[0]);

        if (!target.canFind()) {
            putReply(client, RPL_FINDUSEREX, "1 :No such nick (not connected)");
            return;
        }

        for (Iterator<ChatChannel> i = channels.values().iterator(); i.hasNext();) {
            ChatChannel channel = i.next();
            if (channel.getUsers().contains(target)) {
                putReply(client, RPL_FINDUSEREX, "0 :" + channel.getName() + ",0");
                return;
            }
        }

        putReply(client, RPL_FINDUSEREX, "1 :No such nick (not in any channel)");
    }

    /**
     * Called when client sends USERIP command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onUserIp(ChatClient client, String[] params) {
        if (params.length < 1) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        // not needed for RA
    }

    /**
     * Called when client sends STARTG command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onStartG(ChatClient client, String[] params) {

        if (params.length < 2) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        String message = "";

        if (channels.containsKey(params[0])) {
            ChatChannel channel = channels.get(params[0]);
            if (channel.getOwner() == client) {
                for (String player : params[1].split(",")) {
                    try {
                        ChatClient user = channel.getUser(player);
                        message += user.getNick() + " " + user.getIp() + " ";
                    } catch(UserNotOnChannelException e) {
                        // ignore?
                    }
                }
                message+= ":1 " + (int)(System.currentTimeMillis() / 1000);

                putReplyChannel(channel, client, "STARTG", client.getNick() + " :" + message);
            } else {
                putReply(client, ERR_CHANOPRIVSNEEDED, params[0] + " :You're not channel operator");
            }
        } else {
            putReply(client, ERR_NOSUCHCHANNEL, params[0] + " :No such channel");
        }
    }

    /**
     * Called when client sends KICK command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onKick(ChatClient client, String[] params) {
        if (params.length < 2) {
            putReply(client, ERR_NEEDMOREPARAMS, "KICK :Not enough parameters");
            return;
        }

        if (channels.containsKey(params[0])) {
            ChatChannel channel = channels.get(params[0]);
            ChatClient target = clients.get(params[1]);

            try {
                channel.kick(client, target);
                putReplyChannel(channel, client, "KICK", channel.getName() + " " + target.getNick() + " :Kicked");
                putMessage(client, target, "KICK", channel.getName() + " " + target.getNick() + " :Kicked");
            } catch (UserNotOperatorException e) {
                putReply(client, ERR_CHANOPRIVSNEEDED, params[0] + " :You're not channel operator");
            } catch (UserNotOnChannelException e) {
                putReply(client, ERR_NOSUCHNICK, params[1] + " :No such nick/channel");
            }
        } else {
            putReply(client, ERR_NOSUCHCHANNEL, params[0] + " :No such channel");
        }
    }

    /**
     * Called when client sends MODE command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onMode(ChatClient client, String[] params) {
        // we're not supporting any standard IRC modes, except +b, so 3 params for now
        if (params.length < 3) {
            putReply(client, ERR_NEEDMOREPARAMS, "MODE :Not enough parameters");
            return;
        }

        if (channels.containsKey(params[0])) {
            ChatChannel channel = channels.get(params[0]);

            // just a single ban, ok?
            if (params[1].equals("+b")) {
                ChatClient target = clients.get(params[2]);

                try {
                    channel.ban(client, target);
                    putReplyChannel(channel, client, "MODE", channel.getName() + " +b " + target.getNick());
                } catch (UserNotOperatorException e) {
                    putReply(client, ERR_CHANOPRIVSNEEDED, params[0] + " :You're not channel operator");
                } catch (UserNotOnChannelException e) {
                    putReply(client, ERR_NOSUCHNICK, params[1] + " :No such nick/channel");
                }
            }
        } else {
            putReply(client, ERR_NOSUCHCHANNEL, params[0] + " :No such channel");
        }
    }
    
    /**
     * Called when client sends NAMES command
     * 
     * @param client    source client
     * @param params    params
     */
     protected void onNames(ChatClient client, String[] params) {

        if (params.length < 1) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        if (channels.containsKey(params[0])) {
            ChatChannel channel = channels.get(params[0]);
            //Fixme: Should we send list only when client is in channel?
            putChannelNames(client, channel);
        } else {
            putReply(client, ERR_NOSUCHCHANNEL, params[0] + " :No such channel");
        }
    }

        /**
     * Called when client sends SQUADINFO command
     * 
     * @param client    source client
     * @param params    params
     */
     protected void onSquadInfo(ChatClient client, String[] params) {

        if (params.length < 1) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        //FIXME: In params[0] is ID of squad, if is 0 it wants info for client
        putReply(client, ERR_SQUADIDNOEXIST, ":ID does not exist");
    }

    /**
     * Called when client sends PART command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onPart(ChatClient client, String[] params) {

        if (params.length < 1) {
            putReply(client, ERR_NEEDMOREPARAMS, ":Not enough parameters");
            return;
        }

        if (channels.containsKey(params[0])) {
            ChatChannel channel = channels.get(params[0]);
            try {
                channel.part(client);
                putReply(client, "PART", channel.getName());
                putReplyChannel(channel, client, "PART", channel.getName());
                // remove empty channel from list
                if ((channel.getFlags() & CHAN_PERMANENT) == 0 && channel.getUsers().isEmpty()) {
                    channels.remove(channel.getName());
                }
            } catch(UserNotOnChannelException e) {
                putReply(client, ERR_NOTONCHANNEL, params[0] + " :You aren't on that channel");
            }
        } else {
            putReply(client, ERR_NOSUCHCHANNEL, params[0] + " :No such channel");
        }
    }

    /**
     * Called when client sends QUIT command
     * 
     * @param client    source client
     * @param params    params
     */
    protected void onQuit(ChatClient client, String[] params) {
        putCommand(client, "ERROR", ":Quit");
        client.disconnect();
    }

    /**
     * Called when the client is considered idle and should be pinged
     * 
     * @param client    target client
     */
    public void clientIdle(ChatClient client) {
        putCommand(client, "PING", ":" + WOL.hostname);
    }

    /**
     * Called when the client is considered timed out and should be disconnected
     * 
     * @param client    target client
     */
    public void clientTimeout(ChatClient client) {
        putCommand(client, "ERROR", ":Ping timeout");
        // desparately try to send the last command out
        try {
            client.canWrite();
        } catch(Exception e) {}
        client.disconnect(true);
    }

    /**
     * Called when a client disconnect is finalized
     * 
     * @param client    source client
     */
    public void clientDisconnect(ChatClient client) {
        if (clients.containsValue(client)) {
            for (Iterator<ChatChannel> i = channels.values().iterator(); i.hasNext();) {
                ChatChannel channel = i.next();
                ArrayList<ChatClient> users = channel.getUsers();

                if (users.contains(client)) {
                    putReplyChannel(channel, client, "QUIT", channel.getName() + " :Disconnected", true);
                    users.remove(client);
                }

                if ((channel.getFlags() & CHAN_PERMANENT) == 0 && users.isEmpty()) {
                    i.remove();
                }
            }
            clients.remove(client.getNick());
        }
    }

    protected void onAccept(SocketChannel clientChannel) {
        ChatClient client = new ChatClient(clientChannel, selector, this);
        client.onConnect();
    }

}