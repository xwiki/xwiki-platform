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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

/**
 * Utility APIs, available from Velocity/Groovy scripting.
 * 
 * @version $Id: $
 */
public class Util extends Api
{
    private com.xpn.xwiki.XWiki xwiki;

    /**
     * {@inheritDoc}
     * 
     * @see Api#Api(com.xpn.xwiki.XWikiContext)
     */
    public Util(com.xpn.xwiki.XWiki xwiki, XWikiContext context)
    {
        super(context);
        this.xwiki = xwiki;
    }

    /**
     * Protect Text from Wiki transformation.
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
     * Protect URLs from Wiki transformation.
     * 
     * @param url the url to escape
     * @return the encoded URL
     * @since 1.3 Milestone 2
     */
    public String escapeURL(String url)
    {
        return com.xpn.xwiki.util.Util.escapeURL(url);
    }

    /**
     * Creates an Array List. This is useful from Velocity since you cannot create Object from
     * Velocity with our secure uberspector.
     * 
     * @return a {@link ArrayList} object
     * @since 1.3 Milestone 2
     */
    public List getArrayList()
    {
        return new ArrayList();
    }

    /**
     * Creates a Hash Map. This is useful from Velocity since you cannot create Object from Velocity
     * with our secure uberspector.
     * 
     * @return a {@link HashMap} object
     * @since 1.3 Milestone 2
     */
    public Map getHashMap()
    {
        return new HashMap();
    }

    /**
     * Creates a Tree Map. This is useful from Velocity since you cannot create Object from Velocity
     * with our secure uberspector.
     * 
     * @return a {@link TreeMap} object
     * @since 1.3 Milestone 2
     */
    public Map getTreeMap()
    {
        return new TreeMap();
    }

    /**
     * @return the current date
     * @since 1.3 Milestone 2
     */
    public Date getDate()
    {
        return this.xwiki.getCurrentDate();
    }

    /**
     * @param time time in milliseconds since 1970, 00:00:00 GMT
     * @return Date a date from a time in milliseconds since 01/01/1970 as a Java {@link Date}
     *         Object
     * @since 1.3 Milestone 2
     */
    public Date getDate(long time)
    {
        return this.xwiki.getDate(time);
    }

    /**
     * @param time the time in milliseconds
     * @return the time delta in milliseconds between the current date and the time passed as
     *         parameter
     * @since 1.3 Milestone 2
     */
    public int getTimeDelta(long time)
    {
        return this.xwiki.getTimeDelta(time);
    }

    /**
     * Split a text to an array of texts, according to a separator.
     * 
     * @param text the original text
     * @param sep the separator characters. The separator is one or more of the separator characters
     * @return An array containing the split text
     * @since 1.3 Milestone 2
     */
    public String[] split(String text, String sep)
    {
        return this.xwiki.split(text, sep);
    }

    /**
     * Get a stack trace as a String
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
     * Sort a list using a standard comparator. Elements need to be mutally comparable and implement
     * the Comparable interface.
     * 
     * @param list the list to sort
     * @return the sorted list (as the same oject reference)
     * @see {@link java.util.Collections#sort(java.util.List)}
     * @since 1.3 Milestone 2
     */
    public List sort(List list)
    {
        Collections.sort(list);
        return list;
    }

    /**
     * Convert an Object to a number and return null if the object is not a Number.
     * 
     * @param object the object to convert
     * @return the object as a {@link Number}
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
     * Generate a random string.
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
     * Output a BufferedImage object into the response outputstream. Once this method has been
     * called, not further action is possible. Users should set $context.setFinished(true) to avoid
     * template output The image is outpout as image/jpeg.
     * 
     * @param image the BufferedImage to output
     * @throws java.io.IOException if the output fails
     * @since 1.3 Milestone 2
     */
    public void outputImage(BufferedImage image) throws IOException
    {
        JPEGImageEncoder encoder;
        OutputStream ostream = getXWikiContext().getResponse().getOutputStream();
        encoder = JPEGCodec.createJPEGEncoder(ostream);
        encoder.encode(image);
        ostream.flush();
    }

    /**
     * Get a Null object. This is useful in Velocity where there is no real null object for
     * comparaisons.
     * 
     * @return a Null Object
     * @since 1.3 Milestone 2
     */
    public Object getNull()
    {
        return null;
    }

    /**
     * Get a New Line character. This is useful in Velocity where there is no real new line
     * character for inclusion in texts.
     * 
     * @return a new line character
     * @since 1.3 Milestone 2
     */
    public String getNewline()
    {
        return "\n";
    }

    /**
     * @param str the String to convert to an integer
     * @return the parsed integer or zero in case of exception
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
     * @param str the String to convert to an Integer Object
     * @return the parsed integer or zero in case of exception
     * @since 1.3 Milestone 2
     */
    public Integer parseInteger(String str)
    {
        return new Integer(parseInt(str));
    }

    /**
     * @param str the String to convert to a long
     * @return the parsed long or zero in case of exception
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
     * @param str the String to convert to a float
     * @return the parsed float or zero in case of exception
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
     * @param str the String to convert to a double
     * @return the parsed double or zero in case of exception
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
     * Escape text so that it can be used in a like clause or in a test for equality clause. For
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
     * Replace all accents by their alpha equivalent.
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
     * Add a and b because Velocity operations are not always working.
     * 
     * @param a an integer to add
     * @param b an integer to add
     * @return the sum of a and b
     * @since 1.3 Milestone 2
     */
    public int add(int a, int b)
    {
        return a + b;
    }

    /**
     * Add a and b because Velocity operations are not working with longs.
     * 
     * @param a a long to add
     * @param b a long to add
     * @return the sum of a and b
     * @since 1.3 Milestone 2
     */
    public long add(long a, long b)
    {
        return a + b;
    }

    /**
     * Add a and b where a and b are non decimal numbers specified as Strings.
     * 
     * @param a a string representing a non decimal number
     * @param b a string representing a non decimal number
     * @return the sum of a and b as a String
     * @since 1.3 Milestone 2
     */
    public String add(String a, String b)
    {
        long c = Long.parseLong(a) + Long.parseLong(b);
        return "" + c;
    }

    /**
     * Cleans up the passed text by removing all accents and special characters to make it a valid
     * page name.
     * 
     * @param name the page name to normalize
     * @return the valid page name
     * @since 1.3 Milestone 2
     */
    public String clearName(String name)
    {
        return this.xwiki.clearName(name, getXWikiContext());
    }

    /**
     * Removes all non alpha numerical characters from the passed text. First tries to convert
     * accented chars to their alpha numeric representation.
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
