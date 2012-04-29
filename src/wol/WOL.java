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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Properties;

/**
 *
 * @author Toni Spets
 */
public class WOL {

    static String hostname;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        long lastThink = 0;

        Properties config = new Properties();
        try {
            config.load(new FileInputStream("wol.prop"));
        } catch (IOException e) {
            System.out.println("Failed to load wol.ini: " + e.getMessage());
            return;
        }

        hostname = config.getProperty("WOL.hostname");

        if (hostname == null) {
            System.out.println("No hostname defined in config.");
            return;
        }

        try {
            Selector selector = Selector.open();

            ServerServer serv = new ServerServer(InetAddress.getByName("0.0.0.0"), 4005, selector);
            ChatServer chat = new ChatServer(InetAddress.getByName("0.0.0.0"), 5000, selector);
            GameresServer gameres = new GameresServer(InetAddress.getByName("0.0.0.0"), 4006, selector);
            LadderServer ladder = new LadderServer(InetAddress.getByName("0.0.0.0"), 4002, selector);

            while (true) {

                if (selector.select(1000) > 0) {
                    for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext();) {
                        SelectionKey k = i.next();
                        SocketEvent se = (SocketEvent)k.attachment();
                        int ops = k.readyOps();

                        try {
                            if ((ops & SelectionKey.OP_ACCEPT) > 0)
                                se.canAccept();

                            if ((ops & SelectionKey.OP_CONNECT) > 0)
                                se.canConnect();

                            if ((ops & SelectionKey.OP_READ) > 0)
                                se.canRead();

                            if ((ops & SelectionKey.OP_WRITE) > 0)
                                se.canWrite();
                        } catch (IOException e) {
                            System.out.println("Unexpected IOException when passing event");
                        }

                        // remove key from selector if channel is closed
                        if (!k.channel().isOpen())
                            k.cancel();

                        i.remove();
                    }
                }

                // let everyone think once per second, approximately
                long now = System.currentTimeMillis();
                if (lastThink < now - 1000) {

                    for (Iterator <SelectionKey> i = selector.keys().iterator(); i.hasNext();) {
                        SelectionKey k = i.next();
                        SocketEvent se = (SocketEvent)k.attachment();
                        se.think(now);
                    }

                    lastThink = now;
                }
            }

        } catch (Exception e) {
            System.out.println("Unexpected exception in main:");
            e.printStackTrace();
        }
    }
}