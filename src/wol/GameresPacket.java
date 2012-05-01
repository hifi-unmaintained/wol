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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import wol.GameresValue.InvalidPacketTypeException;

/**
 * Parses a gameres packet into String/GameresValue pairs
 *
 * @author Toni Spets
 */
public class GameresPacket {

    static class InvalidGameresException extends Exception {
        public InvalidGameresException(String message) {
            super(message);
        }
    };

    /**
     * Parse a gameres packet
     * 
     * @param data      complete gameres packet
     * @return
     * @throws wol.GameresPacket.InvalidGameresException 
     */
    static HashMap<String, GameresValue> parse(ByteBuffer data) throws InvalidGameresException {

        HashMap<String, GameresValue> values = new HashMap<String, GameresValue>();

        data.order(ByteOrder.BIG_ENDIAN);

        try {
            if (data.limit() != data.getShort())
                throw new InvalidGameresException("Invalid packet length");
            data.getShort();
        } catch (BufferUnderflowException e) {
            throw new InvalidGameresException("Packet way too short");
        }

        while (data.position() < data.limit()) {
            String tag = "";
            short type;
            int length;

            data.order(ByteOrder.BIG_ENDIAN);

            try {
                byte[] tagData = new byte[4];
                data.get(tagData);
                tag = new String(tagData, "US-ASCII");
            } catch (BufferUnderflowException e){
                throw new InvalidGameresException("Tag missing");
            } catch (UnsupportedEncodingException e) {
                // never reached
            }

            try {
                type = data.getShort();
            } catch (BufferUnderflowException e){
                throw new InvalidGameresException("Type missing");
            }

            try {
                length = data.getShort();
            } catch (BufferUnderflowException e){
                throw new InvalidGameresException("Length missing");
            }

            if (length % 4 > 0)
                length = length - (length % 4) + 4;

            try {
                GameresValue val = new GameresValue(tag, type, length, data);
                values.put(tag, val);
            } catch (InvalidPacketTypeException e){
                throw new InvalidGameresException("Invalid type");
            } catch (BufferUnderflowException e){
                throw new InvalidGameresException("Data missing");
            }
        }

        return values;
    }
}
