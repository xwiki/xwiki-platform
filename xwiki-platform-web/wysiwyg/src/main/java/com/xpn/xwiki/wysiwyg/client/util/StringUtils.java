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
package com.xpn.xwiki.wysiwyg.client.util;

/**
 * Operations on Strings.
 * 
 * @version $Id$
 */
public class StringUtils
{
    /**
     * Constructor.
     */
    protected StringUtils()
    {
    }

    /**
     * Check is a String is empty ("") or null.
     * 
     * @param str String to inspect.
     * @return true if the string is empty or null, false otherwise.
     */
    public static boolean isEmpty(String str)
    {
        return str == null || str.length() == 0;
    }

    /**
     * @param str the string to search in
     * @param pattern the pattern to search in {@code str}
     * @return the substring in {@code str} after the last occurrence of {@code pattern}, or the full {@code str} if
     *         {@code pattern} does not appear
     */
    public static String substringAfterLast(String str, String pattern)
    {
        return str.substring(str.lastIndexOf(pattern) + 1);
    }
}
