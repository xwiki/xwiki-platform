package org.xwiki.extension.version;

import java.util.Collection;

/**
 * An extension version constraint. Generally on a dependency. A constraint can either consist of one or more version
 * ranges or a single version. In the first case, the constraint expresses a hard requirement on a version matching one
 * of its ranges. In the second case, the constraint expresses a soft requirement on a specific version (i.e. a
 * recommendation).
 * 
 * @version $Id$
 */
public interface VersionConstraint
{
    /**
     * Gets the version ranges of this constraint.
     * 
     * @return the version ranges, never null.
     */
    Collection<CollectionVersionRange> getRanges();

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
     * Merge too version constraint and create a new one.
     * 
     * @param versionConstraint the version constraint to merge with this version constraint
     * @return the new version constraint
     * @throws IncompatibleVersionConstraintException the provided version constraint is compatible with the provided
     *             version constraint
     */
    VersionConstraint merge(VersionConstraint versionConstraint) throws IncompatibleVersionConstraintException;
}
