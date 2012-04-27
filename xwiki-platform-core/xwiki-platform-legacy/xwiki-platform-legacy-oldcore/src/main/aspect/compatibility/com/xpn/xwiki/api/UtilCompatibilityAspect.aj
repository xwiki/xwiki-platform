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
package compatibility.com.xpn.xwiki.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.xpn.xwiki.api.Util;
import com.xpn.xwiki.web.Utils;

/**
 * Add a backward compatibility layer to the {@link Util} class.
 * 
 * @version $Id$
 */
public privileged aspect UtilCompatibilityAspect
{
    /**
     * Creates a {@link HashMap}, a generic map implementation optimized for performance. This is useful from Velocity
     * since new objects cannot be created.
     *
     * @param <T> the type of keys maintained by this map
     * @param <U> the type of mapped values
     * @return a {@link HashMap} object
     * @since 1.3 Milestone 2
     * @deprecated use {@code $collectionstool.map} ({@link org.xwiki.velocity.tools.CollectionsTool#getMap()})
     */
    @Deprecated
    public <T, U> Map<T, U> Util.getHashMap()
    {
        return new HashMap<T, U>();
    }

    /**
     * Creates a {@link TreeMap}, a map implementation which always maintains the natural order of the elements. This is
     * useful from Velocity since new objects cannot be created.
     *
     * @param <T> the type of keys maintained by this map
     * @param <U> the type of mapped values
     * @return a {@link TreeMap} object
     * @since 1.3 Milestone 2
     * @deprecated use {@code $collectionstool.sortedMap}
     *             ({@link org.xwiki.velocity.tools.CollectionsTool#getSortedMap()})
     */
    @Deprecated
    public <T, U> Map<T, U> Util.getTreeMap()
    {
        return new TreeMap<T, U>();
    }

    /**
     * Creates a {@link LinkedHashMap}, a map implementation which preserves the order of insertion. This is useful from
     * Velocity since new objects cannot be created.
     *
     * @param <T> the type of keys maintained by this map
     * @param <U> the type of mapped values
     * @return a {@link LinkedHashMap} object
     * @since 2.2 Milestone 1
     * @deprecated use {@code $collectionstool.orderedMap}
     *             ({@link org.xwiki.velocity.tools.CollectionsTool#getOrderedMap()})
     */
    @Deprecated
    public <T, U> Map<T, U> Util.getLinkedHashMap()
    {
        return new LinkedHashMap<T, U>();
    }

    /**
     * Creates an {@link ArrayList}. This is useful from Velocity since new objects cannot be created.
     *
     * @param T the type of the elements in the list
     * @return an {@link ArrayList} object
     * @since 1.3 Milestone 2
     * @deprecated use {@code $collectionstool.arrayList}
     *             ({@link org.xwiki.velocity.tools.CollectionsTool#getArrayList()})
     */
    @Deprecated
    public <T> List<T> Util.getArrayList()
    {
        return new ArrayList<T>();
    }

    /**
     * Reverse the order of the elements within a list, so that the last element is moved to the beginning of the list,
     * the next-to-last element to the second position, and so on. This is useful from Velocity since classes and their
     * static methods cannot be accessed.
     *
     * @param list the list to reverse
     * @return the reversed list
     * @since 1.4 Milestone 1
     * @deprecated use {@code $collectionstool.reverse($list}
     *             ({@link org.xwiki.velocity.tools.CollectionsTool#reverse(List)})
     */
    @Deprecated
    public <T> List<T> Util.reverseList(List<T> list)
    {
        Collections.reverse(list);
        return list;
    }


    /**
     * Sort a list using a standard comparator. Elements need to be mutually comparable and implement the Comparable
     * interface. This is useful from Velocity since classes and their static methods cannot be accessed.
     *
     * @param list the list to sort
     * @return the sorted list (as the same object reference)
     * @see {@link java.util.Collections#sort(java.util.List)}
     * @since 1.3 Milestone 2
     * @deprecated use {@code $collectionstool.sort($list} ({@link org.xwiki.velocity.tools.CollectionsTool#sort(List)})
     */
    @Deprecated
    public <T extends Comparable<T>> List<T> Util.sort(List<T> list)
    {
        Collections.sort(list);
        return list;
    }

    /**
     * Split a text into an array of texts, according to a list of separator characters.
     *
     * @param text the original text
     * @param separator the separator characters
     * @return an array containing the resulting text fragments
     * @since 1.3 Milestone 2
     * @deprecated use {@code $stringtool.split($string, $separators)}
     *             ({@link org.apache.commons.lang3.StringUtils#split(String, String)})
     */
    @Deprecated
    public String[] Util.split(String text, String separator)
    {
        return this.xwiki.split(text, separator);
    }

    /**
     * Convert an Object to a number and return {@code null} if the object is not a Number.
     *
     * @param object the object to convert
     * @return the object as a {@link Number}, or {@code null}
     * @since 1.3 Milestone 2
     * @deprecated use {@code $mathtool.toNumber($string)}
     *             ({@link org.apache.velocity.tools.generic.MathTool#toNumber(Object)})
     */
    @Deprecated
    public Number Util.toNumber(java.lang.Object object)
    {
        try {
            return new Long(object.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert a {@code String} to a primitive {@code long}.
     *
     * @param str the String to convert to a long
     * @return the parsed long or zero if the string is not a valid long number
     * @since 1.3 Milestone 2
     * @deprecated use {@code $mathtool.toNumber($string).longValue()}
     *             ({@link org.apache.velocity.tools.generic.MathTool#toNumber(Object)})
     */
    @Deprecated
    public long Util.parseLong(String str)
    {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Convert a {@code String} to a primitive {@code int}.
     *
     * @param str the String to convert to an integer
     * @return the parsed integer or zero if the string is not a valid integer number
     * @since 1.3 Milestone 2
     * @deprecated use {@code $mathtool.toInteger($string}
     *             ({@link org.apache.velocity.tools.generic.MathTool#toInteger(Object)})
     */
    @Deprecated
    public int Util.parseInt(String str)
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
     * @deprecated use {@code $mathtool.toInteger($string}
     *             ({@link org.apache.velocity.tools.generic.MathTool#toInteger(Object)})
     */
    @Deprecated
    public Integer Util.parseInteger(String str)
    {
        return Integer.valueOf(parseInt(str));
    }

    /**
     * Convert a {@code String} to a primitive {@code float}.
     *
     * @param str the String to convert to a float
     * @return the parsed float or zero if the string is not a valid float number
     * @since 1.3 Milestone 2
     * @deprecated use {@code $mathtool.toDouble($string).floatValue()}
     *             ({@link org.apache.velocity.tools.generic.MathTool#toDouble(Object)})
     */
    @Deprecated
    public float Util.parseFloat(String str)
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
     * @deprecated use {@code $mathtool.toDouble($string}
     *             ({@link org.apache.velocity.tools.generic.MathTool#toDouble(Object)})
     */
    @Deprecated
    public double Util.parseDouble(String str)
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
     * @deprecated values should be passed as positional parameters, not concatenated in the query; see
     *             {@link XWiki#searchDocuments(String, List)}; if escaping SQL is really needed, use {@code
     *             $escapetool.sql($string} ({@link org.xwiki.velocity.tools.EscapeTool#sql(Object)})
     */
    @Deprecated
    public String Util.escapeSQL(String text)
    {
        return Utils.SQLFilter(text);
    }

    /**
     * Add two integer numbers. Useful in Velocity, since arithmetical operations are not always working.
     *
     * @param a the first number to add
     * @param b the second number to add
     * @return the sum of the two parameters
     * @since 1.3 Milestone 2
     * @deprecated use {@code $mathtool.add($n1, $n2}
     *             ({@link org.apache.velocity.tools.generic.MathTool#add(Object, Object)})
     */
    @Deprecated
    public int Util.add(int a, int b)
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
     * @deprecated use {@code $mathtool.add($n1, $n2}
     *             ({@link org.apache.velocity.tools.generic.MathTool#add(Object, Object)})
     */
    @Deprecated
    public long Util.add(long a, long b)
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
     * @deprecated use {@code $mathtool.add} and {@code $mathtool.toNumber} to get the same result
     */
    @Deprecated
    public String Util.add(String a, String b)
    {
        long c = Long.parseLong(a) + Long.parseLong(b);
        return "" + c;
    }

    /**
     * Protect Text from Wiki transformation. This method is useful for preventing content generated with Velocity from
     * being interpreted in the xwiki/1.0 rendering engine, and should not be used in xwiki/2.0 code. The result is
     * valid only in HTML or XML documents.
     *
     * @param text the text to escape
     * @return the escaped text
     * @since 1.3 Milestone 2
     * @deprecated this method only works for {@code xwiki/1.0} wiki syntax, and it doesn't even escape all the syntax
     */
    @Deprecated
    public String Util.escapeText(String text)
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
     * @deprecated this method only works for {@code xwiki/1.0} wiki syntax when outputting HTML
     */
    @Deprecated
    public String Util.escapeURL(String url)
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
     * @deprecated use {@code $escapetool.url($string)} ({@link org.xwiki.velocity.tools.EscapeTool#url(Object)})
     */
    @Deprecated
    public String Util.encodeURI(String text)
    {
        return com.xpn.xwiki.util.Util.encodeURI(text, this.context);
    }
}
