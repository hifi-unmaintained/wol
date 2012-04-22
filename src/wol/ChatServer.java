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
import java.net.InetAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Toni Spets
 */
public class ChatServer extends TCPServer {

    protected ChatServer(InetAddress address, int port, Selector selector) throws IOException {
        super(address, port, selector);
        System.out.println("ChatServer listening on " + address + ":" + port);
    }

    protected void onAccept(SocketChannel clientChannel) {
        ChatClient user = new ChatClient(clientChannel, selector);
    }

}