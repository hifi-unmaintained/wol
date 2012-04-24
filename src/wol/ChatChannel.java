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

/**
 *
 * @author Toni Spets
 */
public class ChatChannel {

    // standard channel properties
    protected String name;
    protected String topic;
    protected String key;
    protected int gameType;
    protected boolean permanent;
    ArrayList<ChatClient> users;
    ArrayList<String> bans;

    // game properties
    protected int type;
    protected int minUsers;
    protected int maxUsers;
    protected int official;
    protected int tournament;
    protected int ingame;
    protected int flags;
    protected long reserved;
    protected long ipaddr;
    protected int latency;
    protected int hidden;
    protected String location;
    protected String exInfo;

    public class UserBannedException extends Exception {}
    public class UserExistsException extends Exception {}
    public class NoSuchUserException extends Exception {}
    public class InvalidKeyException extends Exception {}
    public class GameFullException extends Exception {}
    public class GameClosedException extends Exception {}

    public ChatChannel(String name, String key, int gameType, boolean permanent) {
        this.name = name;
        this.key = key;
        this.gameType = gameType;
        this.permanent = permanent;
        users = new ArrayList<ChatClient>();
        bans = new ArrayList<String>();
    }

    public boolean isGameType(int gameType) {
        return (this.gameType == gameType);
    }

    public boolean isPermanent() {
        return permanent;
    }

    public String getName() {
        return name;
    }

    public ArrayList<ChatClient> getUsers() {
        return users;
    }

    public int userCount() {
        return users.size();
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

        if (bans.contains(client.nick))
            throw new UserBannedException();

        users.add(client);
    }

    public void part(ChatClient client) throws NoSuchUserException {

        if (!users.contains(client))
            throw new NoSuchUserException();

        users.remove(client);
    }
}
