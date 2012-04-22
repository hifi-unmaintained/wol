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
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Toni Spets
 */
public class TCPClient implements SocketEvent {

    public static final int INBUF_SIZE = 4096;
    public static final int OUTBUF_SIZE = 4096;

    protected Selector selector;
    protected SocketChannel channel;
    protected InetAddress address;
    protected int port;
    protected ByteBuffer inbuf;
    protected ByteBuffer outbuf;
    protected boolean disconnecting;

    protected TCPClient(SocketChannel channel, Selector selector) {
        inbuf = ByteBuffer.allocate(INBUF_SIZE);
        outbuf = ByteBuffer.allocate(OUTBUF_SIZE);
        this.channel = channel;
        this.selector = selector;
        address = channel.socket().getInetAddress();
        port = channel.socket().getPort();

        onConnect();
        setOps();
    }

    protected void setOps() {
        int ops = SelectionKey.OP_READ;

        // if out buffer has data, request write
        if (outbuf.position() > 0)
            ops |= SelectionKey.OP_WRITE;

        try {
            if (channel.isOpen())
                channel.register(selector, ops, this);
        } catch (IOException e) {
            System.out.println("TCPClient: Unexpected exception " + e + " while registering: " + e.getMessage());
        }
    }

    protected void disconnect() {
        disconnect(false);
    }

    protected void disconnect(boolean force) {

        // allow graceful disconnect
        if (!force && outbuf.position() > 0) {
            disconnecting = true;
            return;
        }

        onDisconnect();

        try {
            if (channel.isOpen())
                channel.close();
        } catch (IOException e) {
            // should never reach
        }
    }

    protected void read() throws IOException {

        // handle disconnect and buffer overflow
        try {
            if (channel.read(inbuf) == -1) {
                disconnect();
                return;
            }
        } catch (BufferOverflowException e) {
            disconnect(true);
        }

        inbuf.flip();
        onRead();
        inbuf.compact();
        setOps();
    }

    protected void write() throws IOException {
        // TODO: push outbuf in chunks rather than everything at once!
        outbuf.flip();
        onWrite();
        channel.write(outbuf);
        outbuf.clear();
        setOps();

        // finalize graceful disconnect
        if (disconnecting)
            disconnect(true);
    }

    protected void onConnect() {};
    protected void onRead() {};
    protected void onWrite() {}
    protected void onDisconnect() {};

    public void event(int ops) throws IOException {
        if ((ops & SelectionKey.OP_WRITE) > 0)
            write();
        if ((ops & SelectionKey.OP_READ) > 0)
            read();
    }
}
