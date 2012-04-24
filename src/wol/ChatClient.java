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
import java.util.regex.MatchResult;
import java.util.regex.Matcher;

/**
 *
 * @author Toni Spets
 */
public class ChatClient extends StringTCPClient {

    String nick;

    boolean registered;
    boolean havePassword;

    long lastMessage;
    boolean idle;
    ChatServer server;

    protected ChatClient(SocketChannel channel, Selector selector, ChatServer server) {
        this(channel, selector);
        this.server = server;
    }

    protected ChatClient(SocketChannel channel, Selector selector) {
        super(channel, selector);
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

            else if (command.equalsIgnoreCase("PRIVMSG")) {
                server.onPrivmsg(this, params);
            }

            else if (command.equalsIgnoreCase("GETCODEPAGE")) {
                server.onGetCodepage(this, params);
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