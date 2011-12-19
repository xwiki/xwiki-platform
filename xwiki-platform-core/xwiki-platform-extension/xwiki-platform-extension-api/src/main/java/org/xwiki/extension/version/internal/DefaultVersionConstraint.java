package org.xwiki.extension.version.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.extension.version.IncompatibleVersionConstraintException;
import org.xwiki.extension.version.InvalidVersionConstraintException;
import org.xwiki.extension.version.InvalidVersionRangeException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.version.VersionRange;

/**
 * Default implementation of {@link VersionConstraint}.
 * <p>
 * Mostly based on AETHER implementation which is itself based on Maven specifications.
 * 
 * @see org.sonatype.aether.util.version.GenericVersionConstraint
 * @version $Id$
 */
public class DefaultVersionConstraint implements VersionConstraint
{
    /**
     * The character used to separated version ranges.
     */
    private static final char RANGE_SEPARATOR = ',';

    /**
     * @see #getRanges()
     */
    private List<VersionRange> ranges = new ArrayList<VersionRange>();

    /**
     * @see #getVersion()
     */
    private Version version;

    /**
     * @see #getValue()
     */
    private String value;

    /**
     * @param rawConstraint the version range to parse
     * @throws InvalidVersionConstraintException error when parsing version constraint
     */
    public DefaultVersionConstraint(String rawConstraint) throws InvalidVersionConstraintException
    {
        this.value = rawConstraint;

        // Parse

        String constraint = this.value;

        while (startsWith(constraint, '[') || startsWith(constraint, '(')) {
            int index1 = constraint.indexOf(')');
            int index2 = constraint.indexOf(']');

            int index = index2;
            if (index2 < 0 || (index1 >= 0 && index1 < index2)) {
                index = index1;
            }

            if (index < 0) {
                throw new InvalidVersionConstraintException("Unbounded version range [" + rawConstraint + "]");
            }

            String range = constraint.substring(0, index + 1);
            try {
                this.ranges.add(new DefaultVersionRange(range));
            } catch (InvalidVersionRangeException e) {
                throw new InvalidVersionConstraintException("Failed to parse version range [" + range
                    + "] in constraint [" + rawConstraint + "]");
            }

            constraint = constraint.substring(index + 1).trim();

            if (startsWith(constraint, RANGE_SEPARATOR)) {
                constraint = constraint.substring(1).trim();
            }
        }

        if (constraint.length() > 0 && !this.ranges.isEmpty()) {
            throw new InvalidVersionConstraintException("Invalid version range [" + rawConstraint
                + "], expected [ or ( but got " + constraint);
        }

        if (this.ranges.isEmpty()) {
            this.version = new DefaultVersion(constraint);
        }
    }

    /**
     * CReated a new {@link DefaultVersionConstraint} by cloning the provided version constraint.
     * 
     * @param versionConstraint the version constrain to copy
     */
    public DefaultVersionConstraint(VersionConstraint versionConstraint)
    {
        this(versionConstraint.getRanges(), versionConstraint.getVersion());
    }

    /**
     * @param ranges the ranges of versions
     * @param version the recommended version
     */
    public DefaultVersionConstraint(Iterable< ? extends VersionRange> ranges, Version version)
    {
        if (ranges != null) {
            for (VersionRange range : ranges) {
                this.ranges.add(range);
            }
        }
        this.version = version;
    }

    /**
     * @param str the string to look at
     * @param c the character to search in the provided string
     * @return true of the provided character is the first character of the provided string
     */
    private boolean startsWith(String str, char c)
    {
        return str.length() > 0 && str.charAt(0) == c;
    }

    @Override
    public Collection<VersionRange> getRanges()
    {
        return this.ranges;
    }

    @Override
    public Version getVersion()
    {
        return this.version;
    }

    @Override
    public boolean containsVersion(Version version)
    {
        if (version instanceof DefaultVersion) {
            return containsVersion((DefaultVersion) version);
        } else {
            return containsVersion(new DefaultVersion(version));
        }
    }

    /**
     * @param version the version to test, null is invalid.
     * @return true if the provided version satisfies this constraint, false otherwise.
     */
    public boolean containsVersion(DefaultVersion version)
    {
        for (VersionRange range : this.ranges) {
            if (range.containsVersion(version)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public DefaultVersionConstraint merge(VersionConstraint versionConstraint)
        throws IncompatibleVersionConstraintException
    {
        return merge(versionConstraint.getRanges(), versionConstraint.getVersion());
    }

    /**
     * Create a new {@link DefaultVersionConstraint} instance which is the combination of the provided version and
     * version ranges and this version constraint.
     * 
     * @param otherRanges the version ranges to merge with this version ranges
     * @param otherVersion the version to merge with this version
     * @return the new {@link DefaultVersionConstraint}
     * @throws IncompatibleVersionConstraintException the provided version and version ranges are not compatible with
     *             this version constraint
     */
    public DefaultVersionConstraint merge(Collection<VersionRange> otherRanges, Version otherVersion)
        throws IncompatibleVersionConstraintException
    {
        // Validate
        validateCompatibility(otherRanges, otherVersion);

        Collection<VersionRange> newRanges = new ArrayList<VersionRange>(this.ranges.size() + otherRanges.size());
        newRanges.addAll(this.ranges);
        newRanges.addAll(otherRanges);

        Version newVersion = this.version.compareTo(otherVersion) >= 0 ? this.version : otherVersion;

        return new DefaultVersionConstraint(newRanges, newVersion);
    }

    /**
     * @param otherRanges the ranges to validate with this ranges and version
     * @param otherVersion the version to validate with this ranges
     * @throws IncompatibleVersionConstraintException the provided ranges and recommended version and not compatible
     *             with this ranges and recommended version
     */
    private void validateCompatibility(Collection<VersionRange> otherRanges, Version otherVersion)
        throws IncompatibleVersionConstraintException
    {
        for (VersionRange otherRange : otherRanges) {
            validateVersionWithRange(this.version, otherRange);

            for (VersionRange range : this.ranges) {
                validateVersionWithRange(otherVersion, range);

                if (!range.isCompatible(otherRange)) {
                    throw new IncompatibleVersionConstraintException("Ranges [" + range + "] and [" + otherRange
                        + "] are incompatibles");
                }
            }
        }
    }

    /**
     * @param version the version to validate with provided version range
     * @param versionRange the version range
     * @throws IncompatibleVersionConstraintException the provided version is not part of provided version range
     */
    private void validateVersionWithRange(Version version, VersionRange versionRange)
        throws IncompatibleVersionConstraintException
    {
        if (version != null && !versionRange.containsVersion(version)) {
            throw new IncompatibleVersionConstraintException("Version [" + this.version
                + "] is not part of version range [" + versionRange + "]");
        }
    }

    @Override
    public String getValue()
    {
        if (this.value == null) {
            StringBuilder buffer = new StringBuilder();

            for (VersionRange range : getRanges()) {
                if (buffer.length() > 0) {
                    buffer.append(RANGE_SEPARATOR);
                }
                buffer.append(range);
            }

            if (buffer.length() <= 0) {
                buffer.append(getVersion());
            }

            this.value = buffer.toString();
        }

        return this.value;
    }

    // Object

    @Override
    public String toString()
    {
        return getValue();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof VersionConstraint)) {
            return false;
        }

        VersionConstraint versionConstraint = (VersionConstraint) obj;

        return this.ranges.equals(versionConstraint.getRanges())
            && ObjectUtils.equals(this.version, versionConstraint.getVersion());
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder(17, 31);
        builder.append(getRanges());
        builder.append(getVersion());

        return builder.toHashCode();
    }
}
