/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.crypto.internal;

import java.io.UnsupportedEncodingException;

import org.bouncycastle.util.encoders.Base64;


/**
 * Utility class for various string conversions, such as base64 encoding and decoding and conversion
 * to byte array. Supports conversion of strings containing base64 encoded data. The conversion uses
 * UTF-8 because it is always available and base64 alphabet is a subset of UTF-8.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public final class Convert
{
    /** Charset used for String <-> byte[] conversion. */
    private static final String CHARSET = "UTF-8";

    /** Default line length for {@link #toChunkedBase64String(byte[])}. */
    private static final int DEFAULT_LINE_LENGTH = 64;

    /** New line separator. */
    private static final String NEWLINE = System.getProperty("line.separator", "\n");

    /**
     * Private default constructor to prevent instantiation.
     */
    private Convert()
    {
        // this class is not supposed to be instantiated
    }

    /**
     * Encode given data and return the base64 encoded result as string (no line breaks).
     * 
     * @param data the data to encode
     * @return base64 encoded data
     */
    public static String toBase64String(byte[] data)
    {
        try {
            return new String(toBase64(data), CHARSET);
        } catch (UnsupportedEncodingException exception) {
            // cannot happen
            throw new RuntimeException(exception);
        }
    }

    /**
     * Encode given data and return the base64 encoded result as string, chunking it into several lines
     * of the default length (64).
     * 
     * @param data the data to encode
     * @return base64 encoded data
     */
    public static String toChunkedBase64String(byte[] data)
    {
        return toChunkedBase64String(data, DEFAULT_LINE_LENGTH);
    }

    /**
     * Encode given data and return the base64 encoded result as string, chunking it into several lines
     * of the given length.
     * 
     * @param data the data to encode
     * @param lineLength maximal line length
     * @return base64 encoded data
     */
    public static String toChunkedBase64String(byte[] data, int lineLength)
    {
        StringBuilder result = new StringBuilder();
        String encoded = toBase64String(data);
        int begin = 0;
        int end = lineLength;
        while (end < encoded.length()) {
            result.append(encoded.substring(begin, end));
            result.append(NEWLINE);
            begin = end;
            end += lineLength;
        }
        result.append(encoded.substring(begin));
        result.append(NEWLINE);
        return result.toString();
    }

    /**
     * Encode given data and return the base64 encoded result as a byte array.
     * 
     * @param data the data to encode
     * @return base64 encoded data array
     */
    public static byte[] toBase64(byte[] data)
    {
        if (data == null) {
            return new byte[0];
        }
        return Base64.encode(data);
    }

    /**
     * Decode the base64 encoded data represented as string.
     * Ignore anything before beginningMarker or after endMarker.
     * 
     * @param withBase64EncodedContent string containing base64 encoded data.
     * @param beginningMarker anything in the string which is not after this will be ignored.
     * @param endMarker anything in the string which is not before this will be ignored.
     * @return the decoded data array
     */
    public static byte[] fromBase64String(final String withBase64EncodedContent,
                                          final String beginningMarker,
                                          final String endMarker)
    {
        return Convert.fromBase64(Convert.stringToBytes(withBase64EncodedContent,
                                                        beginningMarker,
                                                        endMarker));
    }

    /**
     * Decode the base64 encoded data represented as string.
     * 
     * @param base64Encoded base64 encoded data string
     * @return the decoded data array
     */
    public static byte[] fromBase64String(String base64Encoded)
    {
        return fromBase64(stringToBytes(base64Encoded));
    }

    /**
     * Decode the base64 encoded data array.
     * 
     * @param base64Encoded base64 encoded data
     * @return the decoded data array
     */
    public static byte[] fromBase64(byte[] base64Encoded)
    {
        if (base64Encoded == null || base64Encoded.length == 0) {
            return new byte[0];
        }
        return Base64.decode(base64Encoded);
    }

    /**
     * Get substring between beginningMarker and endMarker.
     * 
     * @param input string to get content from.
     * @param beginningMarker anything in the string which is not after this will be ignored.
     * @param endMarker anything in the string which is not before this will be ignored.
     * @return part of input between beginningMarker and endMarker.
     */
    public static String getContentBetween(final String input,
                                           final String beginningMarker,
                                           final String endMarker)
    {
        if (input == null) {
            throw new IllegalArgumentException("Given string is null");
        }
        int beginIndex = input.indexOf(beginningMarker);
        if (beginIndex < 0) {
            throw new IllegalArgumentException("No beginning marker found in string\nExpecting: "
                                               + beginningMarker);
        }
        final int endIndex = input.indexOf(endMarker, beginIndex);
        if (endIndex < 0) {
            throw new IllegalArgumentException("No end marker found in string\nExpecting: "
                                               + endMarker);
        }
        beginIndex += beginningMarker.length();
        return input.substring(beginIndex, endIndex);
    }

    /**
     * Convert string to byte array using the same encoding as for base64 conversion.
     * Ignore anything before beginningMarker or after endMarker.
     * 
     * @param withBase64EncodedContent string containing base64 encoded data.
     * @param beginningMarker anything in the string which is not after this will be ignored.
     * @param endMarker anything in the string which is not before this will be ignored.
     * @return byte array containing the characters from the string (still in Base64 format)
     */
    public static byte[] stringToBytes(final String withBase64EncodedContent,
                                       final String beginningMarker,
                                       final String endMarker)
    {
        return Convert.stringToBytes(Convert.getContentBetween(withBase64EncodedContent, beginningMarker, endMarker));
    }

    /**
     * Convert string to byte array using the same encoding as for base64 conversion.
     * 
     * @param string the string to convert
     * @return byte array containing the characters from the string
     */
    public static byte[] stringToBytes(String string)
    {
        if (string == null) {
            return new byte[0];
        }
        try {
            return string.getBytes(CHARSET);
        } catch (UnsupportedEncodingException exception) {
            // cannot happen
            throw new RuntimeException(exception);
        }
    }

    /**
     * Convert byte array into a string using UTF-8 encoding.
     * 
     * @param bytes to make into a string
     * @return new String from the given bytes.
     */
    public static String bytesToString(byte[] bytes)
    {
        if (bytes == null) {
            return "";
        }
        try {
            return new String(bytes, CHARSET);
        } catch (UnsupportedEncodingException exception) {
            // cannot happen
            throw new RuntimeException(exception);
        }
    }

    /**
     * @return a newline string, dependent on platform.
     */
    public static String getNewline()
    {
        return Convert.NEWLINE;
    }
}

