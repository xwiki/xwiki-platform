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
import org.xwiki.extension.version.Version;

/**
 * Default implementation of {@link Version}. Note each repositories generally provide their own implementation based on
 * their own version standard.
 * <p>
 * Based on AETHER rules which is itself based on Maven specifications
 * 
 * @see org.sonatype.aether.util.version.GenericVersion
 * @version $Id$
 */
public class DefaultVersion implements Version
{
    /**
     * Used to parse the version.
     */
    private static final GenericVersionScheme VERSIONSCHEME = new GenericVersionScheme();

    /**
     * The parsed version.
     */
    protected org.sonatype.aether.version.Version aetherVersion;

    /**
     * @param rawVersion the original string representation of the version
     */
    public DefaultVersion(String rawVersion)
    {
        try {
            this.aetherVersion = VERSIONSCHEME.parseVersion(rawVersion);
        } catch (InvalidVersionSpecificationException e) {
            // Should never happen since org.sonatype.aether.util.version.GenericVersion does not really produce any
            // exception but can't use it directly since it's package protected
        }
    }

    /**
     * @param version the AETHER version
     */
    public DefaultVersion(org.sonatype.aether.version.Version version)
    {
        this.aetherVersion = version;
    }

    /**
     * Create a new {@link DefaultVersion} by cloning the provided version.
     * 
     * @param version the version to copy
     */
    public DefaultVersion(Version version)
    {
        this(version.getValue());
    }

    @Override
    public int compareTo(Version version)
    {
        if (version instanceof DefaultVersion) {
            return compareTo((DefaultVersion) version);
        } else {
            return compareTo(new DefaultVersion(version.getValue()));
        }
    }

    /**
     * @param version the version to compare
     * @return a negative integer, zero, or a positive integer as this version is less than, equal to, or greater than
     *         the specified version
     */
    public int compareTo(DefaultVersion version)
    {
        return this.aetherVersion.compareTo(version.aetherVersion);
    }

    @Override
    public String getValue()
    {
        return this.aetherVersion.toString();
    }
}
