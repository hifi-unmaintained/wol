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

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import static wol.ChatClient.UserOptions.*;

/**
 *
 * @author Toni Spets
 */
public class ChatClient extends StringTCPClient {

    private String nick;

    // ChatServer modifies
    protected boolean registered;
    protected boolean havePassword;

    private long lastMessage;
    private boolean idle;
    private ChatServer server;
    private long writeDelayUntil;
    private boolean sentGameopt;
    private ArrayList<String> queue;

    private int opt1;
    private int opt2;

    public class UserOptions {
        final static public int OPT1_ALLOWFIND = 1;
        final static public int OPT2_ALLOWPAGE = 1;
    }

    protected ChatClient(SocketChannel channel, Selector selector, ChatServer server) {
        this(channel, selector);
        this.server = server;
        this.queue = new ArrayList<String>();
    }

    protected ChatClient(SocketChannel channel, Selector selector) {
        super(channel, selector);
    }

    public void putString(String message) {
        super.putString(message + "\r");
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String newNick) {
        nick = newNick;
    }

    public String getIp() {
        return address.getHostAddress();
    }

    public boolean canFind() {
        return (opt1 & OPT1_ALLOWFIND) > 0;
    }

    public boolean canPage() {
        return (opt2 & OPT2_ALLOWPAGE) > 0;
    }

    public void setOptions(int newOpt1, int newOpt2) {
        opt1 = newOpt1;
        opt2 = newOpt2;
    }

    public void onString(String message) {

        lastMessage = System.currentTimeMillis();
        idle = false;
        Matcher m = server.ircPattern.matcher(message);

        if (m.matches()) {
            MatchResult mr = m.toMatchResult();

            String prefix = mr.group(2);
            String command = mr.group(3);

            ArrayList<String> tmp = new ArrayList<String>();
            String[] parts = m.group(4).split(":", 2);

            for (String param : parts[0].split(" ")) {
                tmp.add(param);
            }

            if (parts.length > 1) {
                tmp.add(parts[1]);
            }

            String[] params = new String[tmp.size()];
            tmp.toArray(params);

            if (command.equalsIgnoreCase("CVERS")) {
                server.onCvers(this, params);
            }

            else if (command.equalsIgnoreCase("PASS")) {
                server.onPass(this, params);
            }

            else if (command.equalsIgnoreCase("NICK")) {
                server.onNick(this, params);
            }

            else if (command.equalsIgnoreCase("APGAR")) {
                server.onApgar(this, params);
            }

            else if (command.equalsIgnoreCase("SERIAL")) {
                server.onSerial(this, params);
            }

            else if (command.equalsIgnoreCase("USER")) {
                server.onUser(this, params);
            }

            else if (command.equalsIgnoreCase("VERCHK")) {
                server.onVerchk(this, params);
            }

            else if (command.equalsIgnoreCase("SETOPT")) {
                server.onSetOpt(this, params);
            }

            else if (command.equalsIgnoreCase("SETCODEPAGE")) {
                server.onSetCodepage(this, params);
            }

            else if (command.equalsIgnoreCase("LIST")) {
                server.onList(this, params);
            }

            else if (command.equalsIgnoreCase("JOIN")) {
                server.onJoin(this, params);
            }

            else if (command.equalsIgnoreCase("JOINGAME")) {
                server.onJoinGame(this, params);
            }

            else if (command.equalsIgnoreCase("TOPIC")) {
                server.onTopic(this, params);
            }

            else if (command.equalsIgnoreCase("GAMEOPT")) {
                server.onGameopt(this, params);
            }

            else if (command.equalsIgnoreCase("KICK")) {
                server.onKick(this, params);
            }

            else if (command.equalsIgnoreCase("MODE")) {
                server.onMode(this, params);
            }

            else if (command.equalsIgnoreCase("PRIVMSG")) {
                server.onPrivmsg(this, params);
            }

            else if (command.equalsIgnoreCase("PAGE")) {
                server.onPage(this, params);
            }

            else if (command.equalsIgnoreCase("GETCODEPAGE")) {
                server.onGetCodepage(this, params);
            }

            else if (command.equalsIgnoreCase("FINDUSEREX")) {
                server.onFindUserEx(this, params);
            }

            else if (command.equalsIgnoreCase("USERIP")) {
                server.onUserIp(this, params);
            }

            else if (command.equalsIgnoreCase("STARTG")) {
                server.onStartG(this, params);
            }

            else if (command.equalsIgnoreCase("PART")) {
                server.onPart(this, params);
            }

            else if (command.equalsIgnoreCase("QUIT")) {
                server.onQuit(this, params);
            }

            else if (command.equalsIgnoreCase("PONG")) {
                // ignore PONG, we don't keep track of them
            }

            else {
                System.out.println("Client sent unknown command: " + command);
            }
        }
    }

    protected void onConnect() {
        lastMessage = System.currentTimeMillis();
        System.out.println(address + ":" + port + " connected to ChatServer");
    }

    protected void onDisconnect() {
        System.out.println(address + ":" + port + " disconnected from ChatServer");
        server.clientDisconnect(this);
    }

    public void sentGameopt(boolean newSentGameopt) {
        sentGameopt = newSentGameopt;
    }

    public boolean sentGameopt() {
        return sentGameopt;
    }

    public void putQueue(String message) {
        queue.add(message);
    }

    public void flushQueue() {
        for (Iterator<String> i = queue.iterator(); i.hasNext();) {
            String message = i.next();
            putString(message);
            i.remove();
        }
    }

    public void discardQueue() {
        queue.clear();
    }

    public void think(long now) {

        if (now - lastMessage > 30000 && !idle) {
            server.clientIdle(this);
            idle = true;
        }

        if (now - lastMessage > 60000) {
            server.clientTimeout(this);
        }
    }
}