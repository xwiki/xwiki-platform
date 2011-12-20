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

import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.xwiki.extension.version.InvalidVersionRangeException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionRange;

/**
 * Default implementation of {@link VersionRange}.
 * <p>
 * Based on AETHER implementation which is itself based on Maven specifications
 * <p>
 * (,1.0]
 * 
 * @see org.sonatype.aether.util.version.GenericVersionRange
 * @version $Id$
 */
public class DefaultVersionRange implements VersionRange
{
    /**
     * Used to parse the version range.
     */
    private static final GenericVersionScheme VERSIONSCHEME = new GenericVersionScheme();

    /**
     * The parsed version range.
     */
    private org.sonatype.aether.version.VersionRange aetherVersionRange;

    /**
     * @param rawRange the version range to parse
     * @throws InvalidVersionRangeException error when parsing version range
     */
    public DefaultVersionRange(String rawRange) throws InvalidVersionRangeException
    {
        try {
            this.aetherVersionRange = VERSIONSCHEME.parseVersionRange(rawRange);
        } catch (InvalidVersionSpecificationException e) {
            throw new InvalidVersionRangeException("Failed to parse version range [" + rawRange + "]", e);
        }
    }

    /**
     * @param versionRange the AETHER version range
     */
    public DefaultVersionRange(org.sonatype.aether.version.VersionRange versionRange)
    {
        this.aetherVersionRange = versionRange;
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
        return this.aetherVersionRange.containsVersion(version.aetherVersion);
    }

    @Override
    public String getValue()
    {
        return this.aetherVersionRange.toString();
    }

    @Override
    public boolean isCompatible(VersionRange otherRange)
    {
        // TODO Auto-generated method stub
        return false;
    }
}
