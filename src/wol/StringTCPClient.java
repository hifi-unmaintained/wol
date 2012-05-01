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
import java.nio.charset.Charset;

/**
 * Implements a line buffered TCP client where lines are terminated with a
 * single NL.
 * 
 * This implementation also handles write buffer overflows by disconnecting the client
 * 
 * @author Toni Spets
 */
abstract public class StringTCPClient extends TCPClient {

    String encoding = "US-ASCII";

    protected StringTCPClient(SocketChannel channel, Selector selector) {
        super(channel, selector);
    }

    /**
     * Get the current encoding name
     * @return encoding name
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Set new encoding for I/O
     * @param newEncoding
     * @throws UnsupportedEncodingException 
     */
    public void setEncoding(String newEncoding) throws UnsupportedEncodingException {
        if (!Charset.isSupported(newEncoding))
            throw new UnsupportedEncodingException(newEncoding);

        encoding = newEncoding;
    }

    /**
     * Queue a new line to be sent to the client
     * 
     * @param message a line without NL
     */
    public void putString(String message) {
        try {
            String messageNl = new String(message + "\n");
            write(messageNl.getBytes(encoding));
        } catch (UnsupportedEncodingException e) {
            System.out.println(address + ":" + port + " is using unsupported encoding, disconnecting: " + e.getMessage());
            disconnect(true);
        }
    }

    /**
     * Called when a new line arrives
     * @param message a line without NL or CRNL
     */
    abstract protected void onString(String message);

    protected void onRead() {

        byte[] buf = inbuf.array();

        int offset = 0, end = inbuf.limit();
        for (int i = 0; i < end; i++) {
            if (buf[i] == '\n') {
                String message = null;

                try {
                    message = new String(buf, offset, i - offset - (i > 0 && buf[i-1] == '\r' ? 1 : 0), encoding);
                } catch (Exception e) {
                    System.out.println("Unexpected exception when converting bytes to string");
                }

                if (message != null && message.length() > 0) {
                    onString(message);
                }

                offset = i + 1;

                // ignore rest of the buffer if something already triggered a disconnect
                if (disconnecting) {
                    break;
                }
            }
        }

        inbuf.position(offset);

    }
}
