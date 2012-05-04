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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Acts as any channel in WOL: chat, lobby and game
 *
 * @author Toni Spets
 */
public class ChatChannel {

    /**
     * Channel name, like #foo
     */
    protected String name;

    /**
     * Current channel topic
     */
    protected String topic = "";

    /**
     * Current channel key
     */
    protected String key;

    /**
     * Channel gameType
     */
    protected int gameType;

    /**
     * ChatClient who created the channel, null for permanent channels
     */
    ChatClient owner;

    /**
     * Current user list
     */
    ArrayList<ChatClient> users;

    /**
     * Current ban list, just nicknames
     */
    ArrayList<String> bans;

    /**
     * Game type
     */
    protected int type;

    /**
     * Minimum amount of users for a game to start
     */
    protected int minUsers;

    /**
     * Maximum amount of users this game can bare
     */
    protected int maxUsers;

    /*
     * Official channel flag?
     */
    protected int official;

    /*
     * Tournament flag
     */
    protected boolean tournament;

    /*
     * Game in-progress flag?
     */
    protected int ingame;

    /**
     * Channel flags
     * @see ChannelFlags
     */
    protected long flags;

    /**
     * Game specific reserved settings
     */
    protected long reserved;

    /**
     * Host ip address in integer format
     */
    protected int ipaddr; //PELISH: Useless - we should get IP from owner

    /**
     * Latency (relative to what?)
     */
    protected int latency;

    /**
     * Hidden flag?
     */
    protected int hidden;

    /**
     * Physical location?
     */
    protected String location;

    /**
     * Game specific extra information in string format
     */
    protected String exInfo;

    /**
     * Thrown when the requested action requires operator privileges
     */
    public class UserNotOperatorException extends Exception {}

    /**
     * Thrown when the requested action targeted unknown user
     */
    public class UserNotOnChannelException extends Exception {}

    /**
     * Thrown when joining a channel where the user is banned from
     */
    public class UserBannedException extends Exception {}

    /**
     * Thrown when joining a channel you are already in
     */
    public class UserExistsException extends Exception {}

    /**
     * Thrown when the channel key is incorrect
     */
    public class InvalidKeyException extends Exception {}

    /**
     * Thrown when the channel is full
     */
    public class GameFullException extends Exception {}

    /**
     * Thrown when the game is closed
     */
    public class GameClosedException extends Exception {}

    /**
     * Flags the channel can have
     */
    public class ChannelFlags {
        final static public int CHAN_PERMANENT  = 4;
        final static public int CHAN_LOBBY      = 128;
        final static public int CHAN_OFFICIAL   = 256;
    }

    /**
     * Creates a new Channel of any type
     * 
     * @param name          channel name, including #
     * @param owner         client who created this channel, can be null
     * @param key           channel key, can be empty string
     * @param gameType      game type
     * @param minUsers      minimum amount of users required to play
     * @param maxUsers      maximum amount of users that can join
     * @param tournament    tournament flag
     * @param reserved      game specific reserved settings
     * @param flags         channel flags
     */
    public ChatChannel(String name, ChatClient owner, String key, int gameType, int minUsers, int maxUsers, boolean tournament, long reserved, long flags) {
        this.name = name;
        this.owner = owner;
        this.key = key;
        this.gameType = gameType;
        this.minUsers = minUsers;
        this.maxUsers = maxUsers;
        this.tournament = tournament;
        this.reserved = reserved;
        this.flags = flags;
        users = new ArrayList<ChatClient>();
        bans = new ArrayList<String>();
    }

    /**
     * Get channel name
     * 
     * @return 
     */
    public String getName() {
        return name;
    }

    /**
     * Get channel owner
     * 
     * @return 
     */
    public ChatClient getOwner() {
        return owner;
    }

    /**
     * Get minimum user amount
     * 
     * @return 
     */
    public int getMinUsers() {
        return minUsers;
    }

    /**
     * Get maximum user amount
     * 
     * @return 
     */
    public int getMaxUsers() {
        return maxUsers;
    }

    /**
     * Get game type
     * 
     * @return 
     */
    public int getType() {
        return gameType;
    }

    /**
     * Get tournament flag
     * 
     * @return 
     */
    public boolean getTournament() {
        return tournament;
    }

    /**
     * Get reserved settings
     * 
     * @return 
     */
    public long getReserved() {
        return reserved;
    }

    /**
     * Get host ip
     * 
     * @return 
     */
    public int getIp() {
        return ipaddr; //Pelish: Useless - we should get it from owner structure
    }
    
    /**
     * Get host ip
     * 
     * @return          Ip address
     */
    public long getLongIp() {
        return this.owner.getLongIp();
    }

    /**
     * Get channel flags
     * 
     * @return 
     */
    public long getFlags() {
        return flags;
    }

    /**
     * Get channel topic
     * 
     * @return 
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Get channel users
     * 
     * @return 
     */
    public ArrayList<ChatClient> getUsers() {
        return users;
    }

    /**
     * Set channel topic
     * 
     * @param client    source client
     * @param newTopic  new topic
     * @throws wol.ChatChannel.UserNotOperatorException 
     */
    public void setTopic(ChatClient client, String newTopic) throws UserNotOperatorException {
        if (getOwner() != client)
            throw new UserNotOperatorException();

        topic = newTopic;
    }

    /**
     * Find user by nick
     * 
     * @param nick      nickname of target user
     * @return
     * @throws wol.ChatChannel.UserNotOnChannelException 
     */
    public ChatClient getUser(String nick) throws UserNotOnChannelException {

        for (Iterator<ChatClient> i = users.iterator(); i.hasNext();) {
            ChatClient client = i.next();
            if (client.getNick().equals(nick)) {
                return client;
            }
        }

        throw new UserNotOnChannelException();
    }

    /**
     * Client joins a channel
     * 
     * @param client    source client
     * @param joinKey   client channel key
     * @throws wol.ChatChannel.UserExistsException
     * @throws wol.ChatChannel.UserBannedException
     * @throws wol.ChatChannel.GameFullException
     * @throws wol.ChatChannel.InvalidKeyException 
     */
    public void join(ChatClient client, String joinKey) throws UserExistsException, UserBannedException, GameFullException, InvalidKeyException {
        // check if user is already in the channel
        if (users.contains(client))
            throw new UserExistsException();

        // check if channel is full
        if (maxUsers > 0 && users.size() == maxUsers)
            throw new GameFullException();

        if (!joinKey.equals(key))
            throw new InvalidKeyException();

        if (bans.contains(client.getNick()))
            throw new UserBannedException();

        users.add(client);
    }

    /**
     * Client is kicked from a channel
     * 
     * @param client        source client
     * @param target        target client
     * @throws wol.ChatChannel.UserNotOperatorException
     * @throws wol.ChatChannel.UserNotOnChannelException 
     */
    public void kick(ChatClient client, ChatClient target) throws UserNotOperatorException, UserNotOnChannelException {

        if (!users.contains(client))
            throw new UserNotOnChannelException();

        if (!users.contains(target))
            throw new UserNotOnChannelException();

        if (getOwner() != client)
            throw new UserNotOperatorException();

        users.remove(target);
    }

    /**
     * Client is banned from a channel
     * 
     * @param client        source client
     * @param target        target client
     * @throws wol.ChatChannel.UserNotOperatorException
     * @throws wol.ChatChannel.UserNotOnChannelException 
     */
    public void ban(ChatClient client, ChatClient target) throws UserNotOperatorException, UserNotOnChannelException {

        if (!users.contains(client))
            throw new UserNotOnChannelException();

        if (getOwner() != client)
            throw new UserNotOperatorException();

        bans.add(target.getNick());
    }

    /**
     * Client leaves a channel
     * 
     * @param client        source client
     * @throws wol.ChatChannel.UserNotOnChannelException 
     */
    public void part(ChatClient client) throws UserNotOnChannelException {

        if (!users.contains(client))
            throw new UserNotOnChannelException();

        users.remove(client);
    }
}
