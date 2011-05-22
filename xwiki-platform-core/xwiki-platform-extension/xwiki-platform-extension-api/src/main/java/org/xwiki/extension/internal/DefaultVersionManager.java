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
package org.xwiki.extension.internal;

import java.util.regex.Pattern;

/**
 * Default implementation of {@link VersionManager}.
 * 
 * @version $Id$
 */
public class DefaultVersionManager implements VersionManager
{
    /**
     * The separator used to cut in peaces the versions strings.
     */
    private static final Pattern SEPARATORS = Pattern.compile("[\\.-]");

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.internal.VersionManager#compareVersions(java.lang.String, java.lang.String)
     */
    public int compareVersions(String version1, String version2)
    {
        String[] elements1 = SEPARATORS.split(version1);
        String[] elements2 = SEPARATORS.split(version2);

        for (int i = 0; i < elements1.length; ++i) {
            int result;
            if (elements2.length == i) {
                result = 1;
            } else {
                result = compareElement(elements1[i], elements2[i]);
            }

            if (result != 0) {
                return result;
            }
        }

        return elements2.length > elements1.length ? -1 : 0;
    }

    /**
     * @param element1 the first element
     * @param element2 the second element
     * @return a negative integer, zero, or a positive integer as first version element is less than, equal to, or
     *         greater than the second version element.
     */
    private int compareElement(String element1, String element2)
    {
        Integer value1 = convertElement(element1);
        Integer value2 = convertElement(element2);

        if (value1 != null && value2 != null) {
            return value1 - value2;
        } else {
            return element1.compareTo(element2);
        }
    }

    /**
     * @param element the version element to convert in integer
     * @return the integer version of the provided string, null if invalid
     */
    private Integer convertElement(String element)
    {
        try {
            return Integer.valueOf(element);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
