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
package org.xwiki.extension.version;

import java.io.Serializable;

/**
 * A range of versions.
 * 
 * @version $Id$
 */
public interface VersionRange extends Serializable
{
    /**
     * Indicate if the provided version is part of this range.
     * 
     * @param version the version to search
     * @return true if the version is part of the range, false otherwise
     */
    boolean containsVersion(Version version);

    /**
     * @return the string representation of this version range
     */
    String getValue();

    /**
     * Indicate if the provided version range is compatible with the provided version range.
     * 
     * @param otherRange the version range to compare
     * @return true if the two version ranges are compatibles, false otherwise
     */
    boolean isCompatible(VersionRange otherRange);
}
