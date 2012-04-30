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

/**
 *
 * @author Toni Spets
 */
public class GameresValue {

    public final static int TYPE_BYTE       = 1;
    public final static int TYPE_BOOLEAN    = 2;
    public final static int TYPE_TIME       = 5;
    public final static int TYPE_INT        = 6;
    public final static int TYPE_STRING     = 7;
    public final static int TYPE_RAW        = 20;

    private int type;

    private String tag;
    private int intValue;
    private String strValue;
    private byte[] rawValue;

    public class InvalidPacketTypeException extends Exception {}

    GameresValue(String tag, int type, int length, ByteBuffer data) throws BufferUnderflowException, InvalidPacketTypeException {

        this.tag = tag;
        this.type = type;

        switch (type) {
            case TYPE_RAW:
                rawValue = new byte[length];
                data.get(rawValue);
                break;
            case TYPE_BOOLEAN:
                data.order(ByteOrder.LITTLE_ENDIAN);
                intValue = data.getInt();
                break;
            case TYPE_STRING:
                rawValue = new byte[length];
                data.get(rawValue);
                try {
                    strValue = new String(rawValue, "US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    strValue = "";
                }
                break;
            case TYPE_BYTE:
            case TYPE_TIME:
            case TYPE_INT:
                data.order(ByteOrder.BIG_ENDIAN);
                intValue = data.getInt();
            default:
        }
    }

    public String getTag() {
        return tag;
    }

    public int getType() {
        return type;
    }

    public byte[] getRaw() {
        return rawValue;
    }

    public boolean getBoolean() {
        return intValue > 0;
    }

    public int getInt() {
        return intValue;
    }

    public String getString() {
        return strValue;
    }

    public String toString() {
        if (type == TYPE_BOOLEAN) {
            return intValue > 0 ? "true" : "false";
        }
        if (type == TYPE_RAW) {
            return "[ RAW " + rawValue.length + " BYTES ]";
        }
        if (type == TYPE_STRING) {
            return strValue;
        } else {
            return String.valueOf(intValue);
        }
    }
}