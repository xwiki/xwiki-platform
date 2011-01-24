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
package com.xpn.xwiki.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.image.ImageProcessor;
import com.xpn.xwiki.web.Utils;

/**
 * Utility APIs, available to scripting environments under the {@code util} variable.
 * 
 * @version $Id$
 */
public class Util extends Api
{
    private com.xpn.xwiki.XWiki xwiki;

    /**
     * Simple constructor, initializes a new utility API with the current {@link com.xpn.xwiki.XWikiContext context} and
     * the current global {@link com.xpn.xwiki.XWiki XWiki} object.
     * 
     * @param xwiki the current global XWiki object
     * @param context the current context
     * @see Api#Api(com.xpn.xwiki.XWikiContext)
     */
    public Util(com.xpn.xwiki.XWiki xwiki, XWikiContext context)
    {
        super(context);
        this.xwiki = xwiki;
    }

    /**
     * Protect Text from Wiki transformation. This method is useful for preventing content generated with Velocity from
     * being interpreted in the xwiki/1.0 rendering engine, and should not be used in xwiki/2.0 code. The result is
     * valid only in HTML or XML documents.
     * 
     * @param text the text to escape
     * @return the escaped text
     * @since 1.3 Milestone 2
     */
    public String escapeText(String text)
    {
        return com.xpn.xwiki.util.Util.escapeText(text);
    }

    /**
     * Protect URLs from Wiki transformation. This method is useful for preventing content generated with Velocity from
     * being interpreted in the xwiki/1.0 rendering engine, and should not be used in xwiki/2.0 code. The result is
     * valid only in HTML or XML documents.
     * 
     * @param url the url to escape
     * @return the encoded URL, which can be used in the HTML output
     * @since 1.3 Milestone 2
     */
    public String escapeURL(String url)
    {
        return com.xpn.xwiki.util.Util.escapeURL(url);
    }

    /**
     * Translates a string into <code>application/x-www-form-urlencoded</code> format, so that it can be safely used in
     * a query string as a parameter value.
     * 
     * @param text the non encoded text
     * @return encoded text
     * @since 1.3 Milestone 2
     * @see #decodeURI(String)
     */
    public String encodeURI(String text)
    {
        return com.xpn.xwiki.util.Util.encodeURI(text, this.context);
    }

    /**
     * Decodes a <code>application/x-www-form-urlencoded</code> string, the reverse of {@link #encodeURI(String)}.
     * 
     * @param text the encoded text
     * @return decoded text
     * @since 1.3 Milestone 2
     * @see #encodeURI(String)
     */
    public String decodeURI(String text)
    {
        return com.xpn.xwiki.util.Util.decodeURI(text, this.context);
    }

    /**
     * Creates an {@link ArrayList}. This is useful from Velocity since new objects cannot be created.
     * 
     * @return an {@link ArrayList} object
     * @since 1.3 Milestone 2
     */
    public <T> List<T> getArrayList()
    {
        return new ArrayList<T>();
    }

    /**
     * Creates a {@link HashMap}, a generic map implementation optimized for performance. This is useful from Velocity
     * since new objects cannot be created.
     * 
     * @return a {@link HashMap} object
     * @since 1.3 Milestone 2
     */
    public <T, U> Map<T, U> getHashMap()
    {
        return new HashMap<T, U>();
    }

    /**
     * Creates a {@link TreeMap}, a map implementation which always maintains the natural order of the elements. This is
     * useful from Velocity since new objects cannot be created.
     * 
     * @return a {@link TreeMap} object
     * @since 1.3 Milestone 2
     */
    public <T, U> Map<T, U> getTreeMap()
    {
        return new TreeMap<T, U>();
    }

    /**
     * Creates a {@link LinkedHashMap}, a map implementation which preserves the order of insertion. This is useful from
     * Velocity since new objects cannot be created.
     * 
     * @return a {@link LinkedHashMap} object
     * @since 2.2 Milestone 1
     */
    public <T, U> Map<T, U> getLinkedHashMap()
    {
        return new LinkedHashMap<T, U>();
    }

    /**
     * Creates a new {@link Date} object corresponding to the current time. This is useful from Velocity since new
     * objects cannot be created.
     * 
     * @return the current date
     * @since 1.3 Milestone 2
     */
    public Date getDate()
    {
        return this.xwiki.getCurrentDate();
    }

    /**
     * Creates a new {@link Date} object corresponding to the specified time. This is useful from Velocity since new
     * objects cannot be created.
     * 
     * @param time time in milliseconds since 1970, 00:00:00 GMT
     * @return Date a date from a time in milliseconds since 01/01/1970 as a Java {@link Date} Object
     * @since 1.3 Milestone 2
     */
    public Date getDate(long time)
    {
        return this.xwiki.getDate(time);
    }

    /**
     * Compute the elapsed time, in milliseconds, since the specified unix-epoch timestamp. This is useful from Velocity
     * since new objects cannot be created.
     * 
     * @param time the time in milliseconds
     * @return the time delta in milliseconds between the current date and the time passed as parameter
     * @since 1.3 Milestone 2
     */
    public int getTimeDelta(long time)
    {
        return this.xwiki.getTimeDelta(time);
    }

    /**
     * Split a text into an array of texts, according to a list of separator characters.
     * 
     * @param text the original text
     * @param separator the separator characters
     * @return an array containing the resulting text fragments
     * @since 1.3 Milestone 2
     */
    public String[] split(String text, String separator)
    {
        return this.xwiki.split(text, separator);
    }

    /**
     * Reverse the order of the elements within a list, so that the last element is moved to the beginning of the list,
     * the next-to-last element to the second position, and so on. This is useful from Velocity since classes and their
     * static methods cannot be accessed.
     * 
     * @param list the list to reverse
     * @return the reversed list
     * @since 1.4 Milestone 1
     */
    public <T> List<T> reverseList(List<T> list)
    {
        Collections.reverse(list);
        return list;
    }

    /**
     * Get a stack trace as a String.
     * 
     * @param e the exception to convert to a String
     * @return the exception stack trace as a String
     * @since 1.3 Milestone 2
     */
    public String printStrackTrace(Throwable e)
    {
        return this.xwiki.printStrackTrace(e);
    }

    /**
     * Sort a list using a standard comparator. Elements need to be mutually comparable and implement the Comparable
     * interface. This is useful from Velocity since classes and their static methods cannot be accessed.
     * 
     * @param list the list to sort
     * @return the sorted list (as the same object reference)
     * @see {@link java.util.Collections#sort(java.util.List)}
     * @since 1.3 Milestone 2
     */
    public <T extends Comparable<T>> List<T> sort(List<T> list)
    {
        Collections.sort(list);
        return list;
    }

    /**
     * Generate a random string, containing only alpha-numeric characters.
     * 
     * @param size the desired size of the string
     * @return the randomly generated string
     * @since 1.3 Milestone 2
     */
    public String generateRandomString(int size)
    {
        return this.xwiki.generateRandomString(size);
    }

    /**
     * Output a BufferedImage object into the response outputstream. Once this method has been called, no further action
     * is possible. Users should set {@code $context.setFinished(true)} to avoid template output. The image is served as
     * image/jpeg.
     * 
     * @param image the BufferedImage to output
     * @throws java.io.IOException if the output fails
     * @since 1.3 Milestone 2
     */
    public void outputImage(BufferedImage image) throws IOException
    {
        OutputStream ostream = getXWikiContext().getResponse().getOutputStream();
        Utils.getComponent(ImageProcessor.class).writeImage(image, "image/jpeg", (float) 0.8, ostream);
        ostream.flush();
    }

    /**
     * Get a Null value. This is useful in Velocity where there is no real {@code null} object for comparisons.
     * 
     * @return a {@code null} Object
     * @since 1.3 Milestone 2
     */
    public Object getNull()
    {
        return null;
    }

    /**
     * Get a New Line character. This is useful in Velocity where there is no real new line character for inclusion in
     * texts.
     * 
     * @return a new line character
     * @since 1.3 Milestone 2
     */
    public String getNewline()
    {
        return "\n";
    }

    /**
     * Convert an Object to a number and return {@code null} if the object is not a Number.
     * 
     * @param object the object to convert
     * @return the object as a {@link Number}, or {@code null}
     * @since 1.3 Milestone 2
     */
    public Number toNumber(java.lang.Object object)
    {
        try {
            return new Long(object.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert a {@code String} to a {@code Boolean} object.
     * 
     * @param str the String containing the boolean representation to be parsed
     * @return the boolean represented by the string argument, {@code false} if the string is not representing a boolean
     * @since 1.8 Milestone 2
     */
    public Boolean parseBoolean(String str)
    {
        return Boolean.parseBoolean(str);
    }

    /**
     * Convert a {@code String} to a primitive {@code int}.
     * 
     * @param str the String to convert to an integer
     * @return the parsed integer or zero if the string is not a valid integer number
     * @since 1.3 Milestone 2
     */
    public int parseInt(String str)
    {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Convert a {@code String} to an {@code Integer} object.
     * 
     * @param str the String to convert to an Integer Object
     * @return the parsed integer or zero if the string is not a valid integer number
     * @since 1.3 Milestone 2
     */
    public Integer parseInteger(String str)
    {
        return new Integer(parseInt(str));
    }

    /**
     * Convert a {@code String} to a primitive {@code long}.
     * 
     * @param str the String to convert to a long
     * @return the parsed long or zero if the string is not a valid long number
     * @since 1.3 Milestone 2
     */
    public long parseLong(String str)
    {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Convert a {@code String} to a primitive {@code float}.
     * 
     * @param str the String to convert to a float
     * @return the parsed float or zero if the string is not a valid float number
     * @since 1.3 Milestone 2
     */
    public float parseFloat(String str)
    {
        try {
            return Float.parseFloat(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Convert a {@code String} to a primitive {@code double}.
     * 
     * @param str the String to convert to a double
     * @return the parsed double or zero if the string is not a valid double number
     * @since 1.3 Milestone 2
     */
    public double parseDouble(String str)
    {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Escape text so that it can be used in a HQL/SQL query, in a like clause or in a test for equality clause. For
     * example it escapes single quote characters.
     * 
     * @param text the text to escape
     * @return filtered text
     * @since 1.3 Milestone 2
     */
    public String escapeSQL(String text)
    {
        return Utils.SQLFilter(text);
    }

    /**
     * Replace all accented characters by their ASCII equivalent.
     * 
     * @param text the text to parse
     * @return a string with accents replaced with their alpha equivalent
     * @since 1.3 Milestone 2
     */
    public String clearAccents(String text)
    {
        return com.xpn.xwiki.util.Util.noaccents(text);
    }

    /**
     * Add two integer numbers. Useful in Velocity, since arithmetical operations are not always working.
     * 
     * @param a the first number to add
     * @param b the second number to add
     * @return the sum of the two parameters
     * @since 1.3 Milestone 2
     */
    public int add(int a, int b)
    {
        return a + b;
    }

    /**
     * Add two long numbers. Useful in Velocity, since arithmetical operations are not always working.
     * 
     * @param a the first number to add
     * @param b the second number to add
     * @return the sum of the two parameters
     * @since 1.3 Milestone 2
     */
    public long add(long a, long b)
    {
        return a + b;
    }

    /**
     * Add two numbers, specified as strings. Useful in Velocity, since arithmetical operations are not always working.
     * 
     * @param a a string representing a number to add
     * @param b a string representing a number to add
     * @return the sum of the two parameters, as a String
     * @since 1.3 Milestone 2
     */
    public String add(String a, String b)
    {
        long c = Long.parseLong(a) + Long.parseLong(b);
        return "" + c;
    }

    /**
     * Cleans up the passed text by removing all accents and special characters to make it a valid page name.
     * 
     * @param documentName the document name to normalize
     * @return the equivalent valid document name
     * @since 1.3 Milestone 2
     */
    public String clearName(String documentName)
    {
        return this.xwiki.clearName(documentName, getXWikiContext());
    }

    /**
     * Removes all non alpha numerical characters from the passed text. First tries to convert accented chars to their
     * ASCII representation. Then it removes all the remaining non-alphanumeric non-ASCII characters.
     * 
     * @param text the text to convert
     * @return the alpha numeric equivalent
     * @since 1.3 Milestone 2
     */
    public String convertToAlphaNumeric(String text)
    {
        return com.xpn.xwiki.util.Util.convertToAlphaNumeric(text);
    }
}
