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
import java.util.Vector;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Toni Spets
 */
public class ChatClient extends StringTCPClient {

    final public int ERR_NEEDMOREPARAMS     = 461;
    final public int ERR_ALREADYREGISTERED  = 462;
    final public int ERR_PASSWDMISMATCH     = 464;

    String nick;

    boolean registered;
    boolean havePassword;

    Pattern ircPattern;

    protected ChatClient(SocketChannel channel, Selector selector) {
        super(channel, selector);
        ircPattern = Pattern.compile("^(:([^ ]+) )?([^ ]+) (.+)");
    }

    protected void onCvers(String[] params) { }

    protected void onPass(String[] params) {

        if (params.length < 1) {
            putReply(ERR_NEEDMOREPARAMS, "Not enough parameters");
            return;
        }

        if (params[0] != "supersecret") {
            putReply(ERR_PASSWDMISMATCH, "Password incorrect")
            disconnect();
            return;
        }

        havePassword = true;
    }

    protected void onNick(String[] params) {

        if (params.length < 1) {
            putReply(ERR_NEEDMOREPARAMS, "Not enough parameters");
            return;
        }

        nick = params[0];
    }

    protected void onApgar(String[] params) { }
    protected void onSerial(String[] params) { }

    protected void onUser(String[] params) {

        if (params.length < 4) {
            putError(ERR_NEEDMOREPARAMS, "Not enough parameters");
            return;
        }

        if (user != null) {
            putError(ERR_ALREADYREGISTERED, "Not enough parameters");
            return;
        }

        if (!havePassword) {
            putReply(ERR_PASSWDMISMATCH, "Password incorrect")
            disconnect();
            return;
        }

        if (nick != null) {
            registered = true;
        }
    }

    protected void onVerchk(String[] params) { }
    protected void onSetOpt(String[] params) { }
    protected void onSetCodepage(String[] params) { }
    protected void onList(String[] params) { }
    protected void onJoinGame(String[] params) { }
    protected void onJoin(String[] params) { }
    protected void onGetCodepage(String[] params) { }
    protected void onPart(String[] params) { }

    protected void onQuit(String[] params) {
        // FIXME: do a dirty quit for now
        disconnect(true);
    }

    public void onString(String message) {

        Matcher m = ircPattern.matcher(message);

        if (m.matches()) {
            MatchResult mr = m.toMatchResult();

            // parse params
            Vector<String> tmp = new Vector<String>();
            String[] parts = m.group(4).split(":", 2);

            for (String param : parts[0].split(" ")) {
                tmp.add(param);
            }

            if (parts.length > 1) {
                tmp.add(parts[1]);
            }

            String prefix = mr.group(2);
            String command = mr.group(3);
            String[] params = new String[tmp.size()];
            tmp.toArray(params);

            if (command.equalsIgnoreCase("CVERS")) {
                onCvers(params);
            }

            else if (command.equalsIgnoreCase("PASS")) {
                onPass(params);
            }

            else if (command.equalsIgnoreCase("NICK")) {
                onNick(params);
            }

            else if (command.equalsIgnoreCase("APGAR")) {
                onApgar(params);
            }

            else if (command.equalsIgnoreCase("SERIAL")) {
                onSerial(params);
            }

            else if (command.equalsIgnoreCase("USER")) {
                onUser(params);
            }

            else if (command.equalsIgnoreCase("VERCHK")) {
                onVerchk(params);
            }

            else if (command.equalsIgnoreCase("SETOPT")) {
                onSetOpt(params);
            }

            else if (command.equalsIgnoreCase("SETCODEPAGE")) {
                onSetCodepage(params);
            }

            else if (command.equalsIgnoreCase("LIST")) {
                onList(params);
            }

            else if (command.equalsIgnoreCase("JOIN")) {
                onJoin(params);
            }

            else if (command.equalsIgnoreCase("JOINGAME")) {
                onJoinGame(params);
            }

            else if (command.equalsIgnoreCase("GETCODEPAGE")) {
                onGetCodepage(params);
            }

            else if (command.equalsIgnoreCase("PART")) {
                onPart(params);
            }

            else if (command.equalsIgnoreCase("QUIT")) {
                onQuit(params);
            }
        }
    }

    protected void putReply(int code, String message) {
        putString(":irc.westwood.com " + code + " UserName :" + message);
    }

    protected void onConnect() {
        System.out.println(address + ":" + port + " connected to ChatServer");
    }

    protected void onDisconnect() {
        System.out.println(address + ":" + port + " disconnected from ChatServer");
    }
}