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
import java.util.Collection;

/**
 * An extension version constraint. Generally on a dependency. A constraint can either consist of one or more version
 * ranges or a single version. In the first case, the constraint expresses a hard requirement on a version matching one
 * of its ranges. In the second case, the constraint expresses a soft requirement on a specific version (i.e. a
 * recommendation).
 * 
 * @version $Id$
 */
public interface VersionConstraint extends Serializable
{
    /**
     * Gets the version ranges of this constraint.
     * 
     * @return the version ranges, never null.
     */
    Collection<VersionRangeCollection> getRanges();

    /**
     * Gets the version recommended by this constraint.
     * 
     * @return the recommended version or null if none.
     */
    Version getVersion();

    /**
     * @return a String representation of the version constraint
     */
    String getValue();

    /**
     * Indicate if the provided {@link Version} satisfies the constraint.
     * 
     * @param version the version to test, null is invalid.
     * @return true if the provided version satisfies this constraint, false otherwise.
     */
    boolean containsVersion(Version version);

    /**
     * Merge too version constraints in one.
     * 
     * @param versionConstraint the version constraint to merge with this version constraint
     * @return the merged version constraint
     * @throws IncompatibleVersionConstraintException the provided version constraint is compatible with the provided
     *             version constraint
     */
    VersionConstraint merge(VersionConstraint versionConstraint) throws IncompatibleVersionConstraintException;
}
