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

import org.xwiki.component.annotation.ComponentRole;

/**
 * Helper to do versions related operations.
 * 
 * @version $Id$
 */
@ComponentRole
public interface VersionManager
{
    /**
     * Compare to version.
     * <p>
     * Anything is supported (i.e. will return a result) but version members separators used are dot (.) and dash (-).
     * When one of the compared version member is not a number {@link String#compareTo(String)} is used.
     * <p>
     * Here are some examples:
     * <ul>
     * <li>1.1 is greater then 1.0</li>
     * <li>1.10 is greater then 1.2</li>
     * <li>1.10-sometext is greater then 1.2</li>
     * <li>1.1-sometext is greater then 1.1</li>
     * <li>1.sometext is greater then 1.10</li>
     * </ul>
     * 
     * @param version1 the first version
     * @param version2 the second version
     * @return a negative integer, zero, or a positive integer as first version is less than, equal to, or greater than
     *         the second version.
     */
    int compareVersions(String version1, String version2);
}
