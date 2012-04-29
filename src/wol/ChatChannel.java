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
 *
 * @author Toni Spets
 */
public class ChatChannel {

    // standard channel properties
    protected String name;
    protected String topic = "";
    protected String key;
    protected int gameType;
    protected boolean permanent;
    ChatClient owner;
    ArrayList<ChatClient> users;
    ArrayList<String> bans;

    // game properties
    protected int type;
    protected int minUsers;
    protected int maxUsers;
    protected int official;
    protected boolean tournament;
    protected int ingame;
    protected int flags;
    protected int reserved;
    protected int ipaddr;
    protected int latency;
    protected int hidden;
    protected String location;
    protected String exInfo;

    public class UserNotOperatorException extends Exception {}
    public class UserNotOnChannelException extends Exception {}
    public class UserBannedException extends Exception {}
    public class UserExistsException extends Exception {}
    public class InvalidKeyException extends Exception {}
    public class GameFullException extends Exception {}
    public class GameClosedException extends Exception {}

    public class ChannelFlags {
        final static public int CHAN_PERMANENT  = 4;
        final static public int CHAN_LOBBY      = 128;
        final static public int CHAN_OFFICIAL   = 256;
    }

    public ChatChannel(String name, ChatClient owner, String key, int gameType, int minUsers, int maxUsers, boolean tournament, int reserved, int flags) {
        this.name = name;
        this.owner = owner;
        this.key = key;
        this.gameType = gameType;
        this.minUsers = minUsers;
        this.maxUsers = maxUsers;
        this.tournament = tournament;
        this.reserved = reserved;
        this.permanent = permanent;
        this.flags = flags;
        users = new ArrayList<ChatClient>();
        bans = new ArrayList<String>();
    }

    public String getName() {
        return name;
    }

    public ChatClient getOwner() {
        return owner;
    }

    public int getMinUsers() {
        return minUsers;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public int getType() {
        return gameType;
    }

    public boolean getTournament() {
        return tournament;
    }

    public int getReserved() {
        return reserved;
    }

    public int getIp() {
        return ipaddr;
    }

    public int getFlags() {
        return flags;
    }

    public String getTopic() {
        return topic;
    }

    public ArrayList<ChatClient> getUsers() {
        return users;
    }

    public void setTopic(ChatClient client, String newTopic) throws UserNotOperatorException {
        if (getOwner() != client)
            throw new UserNotOperatorException();

        topic = newTopic;
    }

    public ChatClient getUser(String nick) throws UserNotOnChannelException {

        for (Iterator<ChatClient> i = users.iterator(); i.hasNext();) {
            ChatClient client = i.next();
            if (client.getNick().equals(nick)) {
                return client;
            }
        }

        throw new UserNotOnChannelException();
    }

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

    public void kick(ChatClient client, ChatClient target) throws UserNotOperatorException, UserNotOnChannelException {

        if (!users.contains(client))
            throw new UserNotOnChannelException();

        if (!users.contains(target))
            throw new UserNotOnChannelException();

        if (getOwner() != client)
            throw new UserNotOperatorException();

        users.remove(target);
    }

    public void ban(ChatClient client, ChatClient target) throws UserNotOperatorException, UserNotOnChannelException {

        if (!users.contains(client))
            throw new UserNotOnChannelException();

        if (getOwner() != client)
            throw new UserNotOperatorException();

        bans.add(target.getNick());
    }

    public void part(ChatClient client) throws UserNotOnChannelException {

        if (!users.contains(client))
            throw new UserNotOnChannelException();

        users.remove(client);
    }
}
