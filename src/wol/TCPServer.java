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
import java.net.InetSocketAddress;
import java.nio.channels.*;

/**
 *
 * @author Toni Spets
 */
abstract public class TCPServer implements SocketEvent {

    protected ServerSocketChannel channel;
    protected Selector selector;

    protected TCPServer(InetAddress address, int port, Selector selector) throws IOException {
        this.selector = selector;

        channel = ServerSocketChannel.open();
        channel.socket().bind(new InetSocketAddress(address, port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT, this);
    }

    public void canAccept() {
        try {
            SocketChannel clientChannel = channel.accept();
            clientChannel.configureBlocking(false);
            onAccept(clientChannel);
        } catch(IOException e) {
            System.out.println("TCPServer: Unexpected exception " + e + ": " + e.getMessage());
        }
    }

    public void canConnect() {}
    public void canRead() {}
    public void canWrite() {}
    public void think(long now) {}

    abstract protected void onAccept(SocketChannel clientChannel);

}