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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.extension.version.InvalidVersionRangeException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionRange;
import org.xwiki.extension.version.VersionRangeCollection;

/**
 * A collection of {@link VersionRange} linked with OR logic operation.
 * <p>
 * Mostly based on AETHER implementation which is itself based on Maven specifications.
 * <p>
 * (,1.0],[2.0,)
 * 
 * @see org.sonatype.aether.util.version.GenericVersionScheme#parseVersionConstraint(String)
 * @version $Id$
 */
public class DefaultVersionRangeCollection implements VersionRangeCollection
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
    private List<VersionRange> ranges = new ArrayList<VersionRange>();

    /**
     * String representation of this range.
     */
    private String value;

    /**
     * @param rawRanges the version ranges to parse
     * @throws InvalidVersionRangeException error when parsing version range
     */
    public DefaultVersionRangeCollection(String rawRanges) throws InvalidVersionRangeException
    {
        setRanges(rawRanges);
    }

    /**
     * @param ranges the ranges
     */
    public DefaultVersionRangeCollection(Collection< ? extends VersionRange> ranges)
    {
        for (VersionRange range : ranges) {
            this.ranges.add(range);
        }
    }

    /**
     * @param rawRanges the range collection string representation
     * @throws InvalidVersionRangeException error when parsing version range
     */
    private void setRanges(String rawRanges) throws InvalidVersionRangeException
    {
        this.value = rawRanges;

        // Parse

        if (StringUtils.isEmpty(rawRanges)) {
            throw new InvalidVersionRangeException("Range can't be empty");
        }

        parseRanges(this.value);
    }

    /**
     * @param rawRanges the ranges to parse
     * @throws InvalidVersionRangeException invalid ranges syntax
     */
    private void parseRanges(String rawRanges) throws InvalidVersionRangeException
    {
        String currentRanges = rawRanges;

        while (VersionUtils.startsWith(currentRanges, '[') || VersionUtils.startsWith(currentRanges, '(')) {
            int index1 = currentRanges.indexOf(')');
            int index2 = currentRanges.indexOf(']');

            int index = index2;
            if (index2 < 0 || (index1 >= 0 && index1 < index2)) {
                index = index1;
            }

            if (index < 0) {
                throw new InvalidVersionRangeException("Unbounded version range [" + rawRanges + "]");
            }

            currentRanges = parseRange(currentRanges, index, rawRanges);

            if (VersionUtils.startsWith(currentRanges, RANGE_SEPARATOR)) {
                currentRanges = currentRanges.substring(1).trim();
            }
        }

        if (!currentRanges.isEmpty()) {
            throw new InvalidVersionRangeException("Invalid version range [" + rawRanges
                + "], expected [ or ( but got " + currentRanges);
        }
    }

    /**
     * @param currentRanges the current ranges string representation
     * @param index the index of the end of the range in currentRanges
     * @param rawRanges the full string representation
     * @return the new currentRanges
     * @throws InvalidVersionRangeException syntax error in the range
     */
    private String parseRange(String currentRanges, int index, String rawRanges) throws InvalidVersionRangeException
    {
        String range = currentRanges.substring(0, index + 1);
        try {
            this.ranges.add(new DefaultVersionRange(range));
        } catch (InvalidVersionRangeException e) {
            throw new InvalidVersionRangeException("Failed to parse version range [" + range + "] in constraint ["
                + rawRanges + "]");
        }

        return currentRanges.substring(index + 1).trim();
    }

    @Override
    public Collection<VersionRange> getRanges()
    {
        return this.ranges;
    }

    @Override
    public boolean containsVersion(Version version)
    {
        for (VersionRange range : getRanges()) {
            if (range.containsVersion(version)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getValue()
    {
        if (this.value == null) {
            StringBuilder buffer = new StringBuilder();

            for (VersionRange range : this.ranges) {
                if (buffer.length() > 0) {
                    buffer.append(RANGE_SEPARATOR);
                }
                buffer.append(range);
            }

            this.value = buffer.toString();
        }

        return this.value;
    }

    @Override
    public boolean isCompatible(VersionRange otherRange)
    {
        if (equals(otherRange)) {
            return true;
        }

        for (VersionRange versionRange : this.ranges) {
            boolean compatible;

            if (otherRange instanceof VersionRangeCollection) {
                compatible = ((VersionRangeCollection) otherRange).isCompatible(versionRange);
            } else {
                compatible = versionRange.isCompatible(otherRange);
            }

            if (compatible) {
                return true;
            }
        }

        return false;
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

        if (obj == null || !(obj instanceof DefaultVersionRangeCollection)) {
            return false;
        }

        DefaultVersionRangeCollection versionConstraint = (DefaultVersionRangeCollection) obj;

        return this.ranges.equals(versionConstraint.getRanges());
    }

    @Override
    public int hashCode()
    {
        return this.ranges.hashCode();
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
     * @throws IOException error when unserializing the version range collection
     * @throws ClassNotFoundException error when unserializing the version range collection
     * @throws InvalidVersionRangeException error when unserializing the version range collection
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException,
        InvalidVersionRangeException
    {
        setRanges((String) in.readObject());
    }
}
