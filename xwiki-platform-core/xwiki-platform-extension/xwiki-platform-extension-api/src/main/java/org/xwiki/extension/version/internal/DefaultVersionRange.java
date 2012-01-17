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
import java.text.MessageFormat;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.extension.version.InvalidVersionRangeException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionRange;

/**
 * Default implementation of {@link VersionRange}.
 * <p>
 * Based on AETHER implementation which is itself based on Maven specifications
 * <p>
 * (,1.0]
 * <p>
 * org.sonatype.aether.util.version.GenericVersionRange has been rewritten because it's impossible to extends it or even
 * access its details to properly implements {@link #isCompatible(VersionRange)} for example.
 * 
 * @see org.sonatype.aether.util.version.GenericVersionRange
 * @version $Id$
 */
public class DefaultVersionRange implements VersionRange
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
     * The minimum version.
     */
    private Version lowerBound;

    /**
     * Indicate if the minimum version is included in the range.
     */
    private boolean lowerBoundInclusive;

    /**
     * The maximum version.
     */
    private Version upperBound;

    /**
     * Indicate if the maximum version is included in the range.
     */
    private boolean upperBoundInclusive;

    /**
     * The string representation of the range.
     */
    private String value;

    /**
     * @param rawRange the version range to parse
     * @throws InvalidVersionRangeException error when parsing version range
     */
    public DefaultVersionRange(String rawRange) throws InvalidVersionRangeException
    {
        setRange(rawRange);
    }

    /**
     * @param lowerBound the minimum version
     * @param lowerBoundInclusive indicate if the minimum version is included in the range
     * @param upperBound the maximum version
     * @param upperBoundInclusive indicate if the maximum version is included in the range
     */
    public DefaultVersionRange(Version lowerBound, boolean lowerBoundInclusive, Version upperBound,
        boolean upperBoundInclusive)
    {
        this.lowerBound = lowerBound;
        this.lowerBoundInclusive = lowerBoundInclusive;
        this.upperBound = upperBound;
        this.upperBoundInclusive = upperBoundInclusive;
    }

    /**
     * @param rawRange the version range to parse
     * @throws InvalidVersionRangeException error when parsing version range
     */
    private void setRange(String rawRange) throws InvalidVersionRangeException
    {
        this.value = rawRange;

        // parse

        String range = this.value;

        this.lowerBoundInclusive = findLowerBoundInclusive(range);
        this.upperBoundInclusive = findUpperBoundInclusive(range);

        range = range.substring(1, range.length() - 1);

        int index = range.indexOf(RANGE_SEPARATOR);

        if (index < 0) {
            if (!this.lowerBoundInclusive || !this.upperBoundInclusive) {
                throw new InvalidVersionRangeException(MessageFormat.format(
                    "Invalid version range [{0}], single version must be surrounded by []", rawRange));
            }

            this.upperBound = new DefaultVersion(range.trim());
            this.lowerBound = this.upperBound;
        } else {
            String parsedLowerBound = range.substring(0, index).trim();
            String parsedUpperBound = range.substring(index + 1).trim();

            // more than two bounds, e.g. (1,2,3)
            if (StringUtils.contains(parsedUpperBound, RANGE_SEPARATOR)) {
                throw new InvalidVersionRangeException(MessageFormat.format(
                    "Invalid version range [{0}], bounds may not contain additional ','", rawRange));
            }

            this.lowerBound = parsedLowerBound.length() > 0 ? new DefaultVersion(parsedLowerBound) : null;
            this.upperBound = parsedUpperBound.length() > 0 ? new DefaultVersion(parsedUpperBound) : null;

            if (this.upperBound != null && this.lowerBound != null) {
                if (this.upperBound.compareTo(this.lowerBound) < 0) {
                    throw new InvalidVersionRangeException(MessageFormat.format(
                        "Invalid version range [{0}], lower bound must not be greater than upper bound", rawRange));
                }
            }
        }
    }

    /**
     * @param range the range to parse
     * @return true if the provided range has the minimum version included
     * @throws InvalidVersionRangeException invalid range
     */
    private boolean findLowerBoundInclusive(String range) throws InvalidVersionRangeException
    {
        if (VersionUtils.startsWith(range, '[')) {
            return true;
        } else if (VersionUtils.startsWith(range, '(')) {
            return false;
        } else {
            throw new InvalidVersionRangeException(MessageFormat.format(
                "Invalid version range [{0}], a range must start with either [ or (", range));
        }
    }

    /**
     * @param range the range to parse
     * @return true if the provided range has the maximum version included
     * @throws InvalidVersionRangeException invalid range
     */
    private boolean findUpperBoundInclusive(String range) throws InvalidVersionRangeException
    {
        if (VersionUtils.endsWith(range, ']')) {
            return true;
        } else if (VersionUtils.endsWith(range, ')')) {
            return false;
        } else {
            throw new InvalidVersionRangeException(MessageFormat.format(
                "Invalid version range [{0}], a range must end with either [ or (", range));
        }
    }

    @Override
    public boolean containsVersion(Version version)
    {
        if (version instanceof DefaultVersion) {
            return containsVersion((DefaultVersion) version);
        } else {
            return containsVersion(new DefaultVersion(version.getValue()));
        }
    }

    /**
     * @param version the version to search
     * @return true if the version is part of the range, false otherwise
     */
    public boolean containsVersion(DefaultVersion version)
    {
        if (this.lowerBound != null) {
            int comparison = this.lowerBound.compareTo(version);

            if (comparison == 0 && !this.lowerBoundInclusive) {
                return false;
            }
            if (comparison > 0) {
                return false;
            }
        }

        if (this.upperBound != null) {
            int comparison = this.upperBound.compareTo(version);

            if (comparison == 0 && !this.upperBoundInclusive) {
                return false;
            }
            if (comparison < 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getValue()
    {
        if (this.value == null) {
            StringBuilder buffer = new StringBuilder();

            buffer.append(this.lowerBoundInclusive ? '[' : '(');
            if (this.lowerBound != null) {
                buffer.append(this.lowerBound);
            }
            buffer.append(RANGE_SEPARATOR);
            if (this.upperBound != null) {
                buffer.append(this.upperBound);
            }
            buffer.append(this.upperBoundInclusive ? ']' : ')');

            this.value = buffer.toString();
        }

        return this.value;
    }

    @Override
    public boolean isCompatible(VersionRange otherRange)
    {
        boolean compatible;

        if (equals(otherRange)) {
            compatible = true;
        } else {
            if (otherRange instanceof DefaultVersionRange) {
                compatible = isCompatible((DefaultVersionRange) otherRange);
            } else {
                try {
                    compatible = isCompatible(new DefaultVersionRange(otherRange.getValue()));
                } catch (InvalidVersionRangeException e) {
                    compatible = false;
                }
            }
        }

        return compatible;
    }

    /**
     * Indicate if the provided version range is compatible with the provided version range.
     * 
     * @param otherRange the version range to compare
     * @return true if the two version ranges are compatibles, false otherwise
     */
    public boolean isCompatible(DefaultVersionRange otherRange)
    {
        int lowerCompare =
            compareTo(this.lowerBound, this.lowerBoundInclusive, otherRange.lowerBound, otherRange.lowerBoundInclusive,
                false);
        int upperCompare =
            compareTo(this.upperBound, this.upperBoundInclusive, otherRange.upperBound, otherRange.upperBoundInclusive,
                true);

        // Both ranges have one bound in common
        if (lowerCompare == 0 || upperCompare == 0) {
            return true;
        }

        // This range is included in the provided range
        if (lowerCompare > 0 && upperCompare < 0) {
            return true;
        }

        // The provided range is included in this range
        if (lowerCompare < 0 && upperCompare > 0) {
            return true;
        }

        // Validate intersections
        return lowerCompare < 0 ? isCompatible(this.upperBound, this.upperBoundInclusive, otherRange.lowerBound,
            otherRange.lowerBoundInclusive) : isCompatible(otherRange.upperBound, otherRange.upperBoundInclusive,
                this.lowerBound, this.lowerBoundInclusive);
    }

    /**
     * @param version1 the left range version
     * @param included1 indicate of the left range version is included
     * @param version2 the right range version
     * @param included2 indicate of the right range version is included
     * @param upper indicate the provided version are upper or lower bounds
     * @return a negative integer, zero, or a positive integer as the left version is less than, equal to, or greater
     *         than the right version
     */
    private int compareTo(Version version1, boolean included1, Version version2, boolean included2, boolean upper)
    {
        int compare;

        if (version1 == null) {
            compare = version2 == null ? 0 : (upper ? 1 : -1);
        } else {
            if (version2 == null) {
                compare = upper ? -1 : 1;
            } else {
                compare = compareNotNull(version1, included1, version2, included2, upper);
            }
        }

        return compare;
    }

    /**
     * @param version1 the left range version
     * @param included1 indicate of the left range version is included
     * @param version2 the right range version
     * @param included2 indicate of the right range version is included
     * @param upper indicate the provided version are upper or lower bounds
     * @return a negative integer, zero, or a positive integer as the left version is less than, equal to, or greater
     *         than the right version
     */
    private int compareNotNull(Version version1, boolean included1, Version version2, boolean included2, boolean upper)
    {
        int compare = version1.compareTo(version2);

        if (compare == 0) {
            if (included1 != included2) {
                compare = included1 ? (upper ? -1 : 1) : (upper ? 1 : -1);
            }
        }

        return compare;
    }

    /**
     * @param upper maximum version of the right range
     * @param upperInclusive indicate if maximum version is included in the range
     * @param lower minimum version of the left range
     * @param lowerInclusive indicate if the minimum version is included in the range
     * @return true if the two version ranges are compatibles, false otherwise
     */
    private boolean isCompatible(Version upper, boolean upperInclusive, Version lower, boolean lowerInclusive)
    {
        boolean compatible = true;

        if (upper != null) {
            if (lower != null) {
                int comparison = upper.compareTo(lower);

                if (comparison > 0) {
                    compatible = true;
                } else if (comparison < 0) {
                    compatible = false;
                } else {
                    compatible = upperInclusive && lowerInclusive;
                }
            }
        }

        return compatible;
    }

    // Object

    @Override
    public String toString()
    {
        return getValue();
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder(17, 31);

        builder.append(this.upperBound);
        builder.append(this.upperBoundInclusive ? 1 : 0);
        builder.append(this.lowerBound);
        builder.append(this.lowerBoundInclusive ? 1 : 0);

        return builder.toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        boolean equals;

        if (obj instanceof DefaultVersionRange) {
            equals = equals((DefaultVersionRange) obj);
        } else if (obj instanceof VersionRange) {
            try {
                equals = equals(new DefaultVersionRange(((VersionRange) obj).getValue()));
            } catch (InvalidVersionRangeException e) {
                equals = false;
            }
        } else {
            equals = false;
        }

        return equals;
    }

    /**
     * @param version the version
     * @return true if the provided version is equals to this version
     */
    public boolean equals(DefaultVersionRange version)
    {
        return this.upperBoundInclusive == version.upperBoundInclusive
            && this.lowerBoundInclusive == version.lowerBoundInclusive
            && ObjectUtils.equals(this.upperBound, version.upperBound)
            && ObjectUtils.equals(this.lowerBound, version.lowerBound);
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
     * @throws InvalidVersionRangeException error when unserializing the version
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException,
        InvalidVersionRangeException
    {
        setRange((String) in.readObject());
    }
}
