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
package org.xwiki.extension.version.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.extension.version.VersionRangeCollection;
import org.xwiki.extension.version.IncompatibleVersionConstraintException;
import org.xwiki.extension.version.InvalidVersionConstraintException;
import org.xwiki.extension.version.InvalidVersionRangeException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.version.VersionRange;

/**
 * Default implementation of {@link VersionConstraint}.
 * <p>
 * Mostly based on AETHER implementation which is itself based on Maven specifications. The difference is that it can
 * contains a list of ranges OR collection instead of just a OR collection to allow combining several constraints in
 * one.
 * <p>
 * {(,1.0],[2.0,)},{[3.0)}
 * 
 * @see org.sonatype.aether.util.version.GenericVersionConstraint
 * @version $Id$
 */
public class DefaultVersionConstraint implements VersionConstraint
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The character used to separated version ranges.
     */
    private static final char RANGE_SEPARATOR = ',';

    /**
     * @see #getRanges()
     */
    private List<VersionRangeCollection> ranges;

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
     */
    public DefaultVersionConstraint(String rawConstraint)
    {
        setConstraint(rawConstraint);
    }

    /**
     * Created a new {@link DefaultVersionConstraint} by cloning the provided version constraint.
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
    public DefaultVersionConstraint(Collection< ? extends VersionRangeCollection> ranges, Version version)
    {
        if (ranges != null && !ranges.isEmpty()) {
            this.ranges = new ArrayList<VersionRangeCollection>(ranges);
        } else {
            this.ranges = Collections.emptyList();
        }
        this.version = version;
    }

    /**
     * @param rawConstraint the constraint to parse
     */
    private void setConstraint(String rawConstraint)
    {
        this.value = rawConstraint;

        // Parse

        List<VersionRangeCollection> newRanges = null;
        try {
            newRanges = parseRanges(rawConstraint);
        } catch (InvalidVersionConstraintException e) {
            // Invalid range syntax, lets use it as version
        }

        // Version

        if (newRanges == null || newRanges.isEmpty()) {
            this.version = new DefaultVersion(rawConstraint);
            this.ranges = Collections.emptyList();
        } else {
            this.ranges = newRanges;
        }
    }

    /**
     * @param rawConstraint the constraint to parse
     * @return the list of version ranges
     * @throws InvalidVersionConstraintException invalid constraint range syntax
     */
    private List<VersionRangeCollection> parseRanges(String rawConstraint) throws InvalidVersionConstraintException
    {
        String constraint = this.value;

        List<VersionRangeCollection> newRanges = new ArrayList<VersionRangeCollection>();

        while (VersionUtils.startsWith(constraint, '{')) {
            int index = constraint.indexOf('}');

            if (index < 0) {
                throw new InvalidVersionConstraintException("Unbounded version range [" + rawConstraint + "]");
            }

            String range = constraint.substring(0, index + 1);
            try {
                newRanges.add(new DefaultCollectionVersionRange(range));
            } catch (InvalidVersionRangeException e) {
                throw new InvalidVersionConstraintException("Failed to parse version range [" + range
                    + "] in constraint [" + rawConstraint + "]");
            }

            constraint = constraint.substring(index + 1).trim();

            if (VersionUtils.startsWith(constraint, RANGE_SEPARATOR)) {
                constraint = constraint.substring(1).trim();
            }
        }

        if (!constraint.isEmpty() && !newRanges.isEmpty()) {
            throw new InvalidVersionConstraintException("Invalid version range [" + rawConstraint
                + "], expected [ or ( but got " + constraint);
        }

        return newRanges;
    }

    @Override
    public Collection<VersionRangeCollection> getRanges()
    {
        return (Collection) this.ranges;
    }

    @Override
    public Version getVersion()
    {
        return this.version;
    }

    @Override
    public boolean containsVersion(Version version)
    {
        boolean contains;

        if (this.ranges.isEmpty()) {
            contains = this.version != null && this.version.equals(version);
        } else {
            contains = true;

            for (VersionRange range : this.ranges) {
                contains &= range.containsVersion(version);
            }
        }

        return contains;
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
    public DefaultVersionConstraint merge(Collection<VersionRangeCollection> otherRanges, Version otherVersion)
        throws IncompatibleVersionConstraintException
    {
        // Validate
        validateCompatibility(otherRanges, otherVersion);

        Collection<VersionRangeCollection> newRanges =
            new ArrayList<VersionRangeCollection>(this.ranges.size() + otherRanges.size());
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
    private void validateCompatibility(Collection<VersionRangeCollection> otherRanges, Version otherVersion)
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

            if (getVersion() != null) {
                buffer.append(getVersion());
            } else {
                if (this.ranges.size() == 1) {
                    buffer.append('{');
                    buffer.append(this.ranges.get(0).getValue());
                    buffer.append('}');
                } else {
                    for (VersionRange range : getRanges()) {
                        if (buffer.length() > 0) {
                            buffer.append(RANGE_SEPARATOR);
                        }
                        buffer.append('{');
                        buffer.append(range.getValue());
                        buffer.append('}');
                    }
                }
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

    // Serializable

    /**
     * @param out the stream
     * @throws IOException error when serializing the version
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.writeObject(getValue());
    }

    /**
     * @param in the stream
     * @throws IOException error when unserializing the version
     * @throws ClassNotFoundException error when unserializing the version
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        setConstraint((String) in.readObject());
    }
}
