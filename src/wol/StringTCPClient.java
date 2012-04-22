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

import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Toni Spets
 */
abstract public class StringTCPClient extends TCPClient {

    protected StringTCPClient(SocketChannel channel, Selector selector) {
        super(channel, selector);
    }

    public void putString(String message) {
        try {
            System.out.println(address + ":" + port + " <- " + message);
            outbuf.put(new String(message + "\r\n").getBytes("US-ASCII"));
            setOps();
        } catch (BufferOverflowException e) {
            System.out.println(address + ":" + port + " SENDQ full, disconnecting");
            disconnect(true);
        } catch (UnsupportedEncodingException e) {
            System.out.println(address + ":" + port + " is using unsupported encoding: " + e.getMessage());
        }
    }

    abstract protected void onString(String message);

    protected void onRead() {

        byte[] buf = inbuf.array();

        int offset = 0, end = inbuf.limit();
        for (int i = 0; i < end - 1; i++) {
            if (buf[i] == '\r' && buf[i+1] == '\n') {
                try {
                    String message = new String(buf, offset, i - offset, "US-ASCII");
                    System.out.println(address + ":" + port + " -> " + message);
                    onString(message);
                } catch (Exception e) {
                    System.out.println("Unexpected exception when converting bytes to string");
                }
                offset = i + 2;
            }
        }

        inbuf.position(offset);

    }
}
