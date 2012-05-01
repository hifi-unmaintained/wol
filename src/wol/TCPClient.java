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
 * Implements a buffered TCP client that can be used for both server and client
 * connections.
 *
 * @author Toni Spets
 */
public class TCPClient implements SocketEvent {

    public static final int INBUF_SIZE = 8192;
    public static final int OUTBUF_SIZE = 2048;

    protected Selector selector;
    protected SocketChannel channel;
    protected InetAddress address;
    protected int port;
    protected ByteBuffer inbuf;
    protected ByteBuffer outbuf;
    protected boolean disconnecting;

    /**
     * Creates a new TCPClient instance
     * 
     * @param channel   pre-created channel for communication
     * @param selector  the main selector that is used to request events
     */
    protected TCPClient(SocketChannel channel, Selector selector) {
        inbuf = ByteBuffer.allocate(INBUF_SIZE);
        outbuf = ByteBuffer.allocate(OUTBUF_SIZE);
        this.channel = channel;
        this.selector = selector;
        address = channel.socket().getInetAddress();
        port = channel.socket().getPort();
        setOps();
    }

    /**
     * Requests for events from the selector
     */
    protected void setOps() {
        int ops = SelectionKey.OP_READ;

        if (!channel.isConnected())
            ops |= SelectionKey.OP_CONNECT;

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

    /**
     * Disconnects the client gracefully by flushing the output buffer
     */
    protected void disconnect() {
        disconnect(false);
    }

    /**
     * Disconnects the client
     * 
     * @param force do not flush the output buffer
     */
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

    public void canAccept() throws IOException {}

    public void canConnect() throws IOException {
        onConnect();
    }

    public void canRead() throws IOException {

        if (inbuf.remaining() == 0) {
            System.out.println(address + ":" + port + " read buffer full, disconnecting");
            disconnect(true);
            return;
        }

        if (channel.read(inbuf) == -1) {
            disconnect();
            return;
        }

        inbuf.flip();
        onRead();
        inbuf.compact();
        setOps();
    }

    public void canWrite() throws IOException {
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

    /**
     * Write to output buffer
     * @param data the bytes to send
     */
    protected void write(byte[] data) {
        try {
            outbuf.put(data);
            setOps();
        } catch (BufferOverflowException e) {
            System.out.println(address + ":" + port + " write buffer full, disconnecting");
            disconnect(true);
        }
    }

    public void close() throws IOException {

        onDisconnect();

        if (channel.isOpen())
            channel.close();
    }

    public void think(long now) {};

    /**
     * Called when this instance is connected to the other end
     */
    protected void onConnect() {};

    /**
     * Called when the input buffer has data
     */
    protected void onRead() {};

    /**
     * Called before writing the output buffer, this is usually not needed
     */
    protected void onWrite() {}

    /**
     * Called when the channel is disconnected
     */
    protected void onDisconnect() {};
}
