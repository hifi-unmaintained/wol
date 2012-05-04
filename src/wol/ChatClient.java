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
 * Client that is connected to ChatServer
 *
 * @author Toni Spets
 */
public class ChatClient extends StringTCPClient {

    private String nick;

    /**
     * Set when NICK, APGAR and USER command have succeeded
     */
    protected boolean registered;

    /**
     * Set when PASS command is sent and acccepted
     */
    protected boolean havePassword;

    /**
     * Timestamp when the last message was received
     */
    private long lastMessage;

    /**
     * Set when the client is considered idle
     */
    private boolean idle;

    /**
     * The ChatServer this client is tied to
     */
    private ChatServer server;

    /**
     * Red Alert self GAMEOPT sent flag (hack)
     */
    private boolean sentGameopt;

    /**
     * Message queue that can be moved to output buffer at any time (hack)
     */
    private ArrayList<String> queue;

    /**
     * SETOPT command values
     */
    private int opt1;
    private int opt2;

    /**
     * SETOPT command flags
     */
    public class UserOptions {
        final static public int OPT1_ALLOWFIND = 1;
        final static public int OPT2_ALLOWPAGE = 1;
    }
    
    /**
     * Client's LOCALE
     */
    private int locale;

    /**
     * Create a new ChatClient that is tied to given ChatServer
     * 
     * @param channel   the open channel used for I/O
     * @param selector  main selector for events
     * @param server    the ChatServer we are tied to
     */
    protected ChatClient(SocketChannel channel, Selector selector, ChatServer server) {
        this(channel, selector);
        this.server = server;
        this.queue = new ArrayList<String>();
    }

    protected ChatClient(SocketChannel channel, Selector selector) {
        super(channel, selector);
    }

    /**
     * Write a single WOL chat string to output buffer
     * 
     * @param message   non-terminated line
     */
    public void putString(String message) {
        System.out.println(address + ":" + port + " <- " + message);
        super.putString(message + "\r");
    }

    /**
     * Get nickname
     * 
     * @return          current nickname
     */
    public String getNick() {
        return nick;
    }

    /**
     * Set nickname
     * 
     * @param newNick   new nickname
     */
    public void setNick(String newNick) {
        nick = newNick;
    }

    /**
     * Get ip address as string
     * 
     * @return          ip address
     */
    public String getIp() {
        return address.getHostAddress();
    }

    /**
     * Get ip address as long
     * 
     * @return          ip address
     */
    public long getLongIp() {
        byte[] raw = address.getAddress();
        return raw[3] + (raw[2] << 8) + (raw[1] << 16) + (raw[0] << 24);
    }
    
    /**
     * Can this user be found by FIND command?
     * 
     * @return 
     */
    public boolean canFind() {
        return (opt1 & OPT1_ALLOWFIND) > 0;
    }

    /**
     * Can this user be PAGEd?
     * 
     * @return 
     */
    public boolean canPage() {
        return (opt2 & OPT2_ALLOWPAGE) > 0;
    }

    /**
     * Set private options (SETOPT command)
     * 
     * @param newOpt1   opt1 integer
     * @param newOpt2   opt2 integer
     */
    public void setOptions(int newOpt1, int newOpt2) {
        opt1 = newOpt1;
        opt2 = newOpt2;
    }
   
    /**
     * Get locale
     * 
     * @return          locale
     */
    public int getLocale() {
        return locale;
    }

    /**
     * Set locale
     * 
     * @param locale   new locale
     */
    public void setLocale(int newLocale) {
        locale = newLocale;
    }

    public void onString(String message) {

        System.out.println(address + ":" + port + " -> " + message);

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
                if (param.length() > 0)
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

            else if (command.equalsIgnoreCase("SETLOCALE")) {
                server.onSetLocale(this, params);
            }

            else if (command.equalsIgnoreCase("GETLOCALE")) {
                server.onGetLocale(this, params);
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

            else if (command.equalsIgnoreCase("NAMES")) {
                server.onNames(this, params);
            }

            else if (command.equalsIgnoreCase("SQUADINFO")) {
                server.onSquadInfo(this, params);
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

    /**
     * Called to set if this client has sent his GAMEOPT (hack)
     * 
     * @param newSentGameopt    new setting
     */
    public void sentGameopt(boolean newSentGameopt) {
        sentGameopt = newSentGameopt;
    }

    /**
     * Has this client sent his GAMEOPT? (hack)
     * 
     * @return 
     */
    public boolean sentGameopt() {
        return sentGameopt;
    }

    /**
     * Add a new message to manually flushed queue (hack)
     * 
     * @param message non-terminated message
     */
    public void putQueue(String message) {
        queue.add(message);
    }

    /**
     * Flush current output queue to output buffer, queue is cleared
     */
    public void flushQueue() {
        for (Iterator<String> i = queue.iterator(); i.hasNext();) {
            String message = i.next();
            putString(message);
            i.remove();
        }
    }

    /**
     * Discard current output queue
     */
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