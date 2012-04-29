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

/**
 *
 * @author hifi
 */
public class ServerClient extends StringTCPClient {
    
    protected ServerClient(SocketChannel channel, Selector selector) {
        super(channel, selector);
    }

    public void putString(String message) {
        super.putString(message + "\r");
    }

    protected void onString(String message) {

        if (message.startsWith("whereto")) {
            putString(":" + WOL.hostname + " 610 UserName 1");
            putString(":" + WOL.hostname + " 605 UserName :" + WOL.hostname + " 5000 'Live chat server' 0 0.0000 0.0000");
            putString(":" + WOL.hostname + " 608 UserName :" + WOL.hostname + " 4006 'Gameres server' 0 0.0000 0.0000");
            putString(":" + WOL.hostname + " 609 UserName :" + WOL.hostname + " 4002 'Ladder server' 0 0.0000 0.0000");
            putString(":" + WOL.hostname + " 607");
            disconnect();
        }
    }

    protected void onConnect() {
        System.out.println(address + ":" + port + " connected to ServerServer");
    }

    protected void onDisconnect() {
        System.out.println(address + ":" + port + " disconnected from ServerServer");
    }

}
