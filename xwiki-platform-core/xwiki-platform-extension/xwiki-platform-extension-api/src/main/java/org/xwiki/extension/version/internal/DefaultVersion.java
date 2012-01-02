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
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Used to parse the version.
     */
    private static final GenericVersionScheme AETHERVERSIONSCHEME = new GenericVersionScheme();

    /**
     * The parsed version.
     */
    protected org.sonatype.aether.version.Version aetherVersion;

    /**
     * @param rawVersion the original string representation of the version
     */
    public DefaultVersion(String rawVersion)
    {
        setVersion(rawVersion);
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

    /**
     * @param rawVersion the version string representation to parse
     */
    private void setVersion(String rawVersion)
    {
        try {
            this.aetherVersion = AETHERVERSIONSCHEME.parseVersion(rawVersion);
        } catch (InvalidVersionSpecificationException e) {
            // Should never happen since org.sonatype.aether.util.version.GenericVersion does not really produce any
            // exception but can't use it directly since it's package protected
        }
    }

    /**
     * @return the real Version implementation.
     */
    public org.sonatype.aether.version.Version getAetherVersion()
    {
        return this.aetherVersion;
    }

    // Version

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

    // Object

    @Override
    public String toString()
    {
        return getValue();
    }

    @Override
    public int hashCode()
    {
        return this.aetherVersion.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        boolean equals;

        if (obj instanceof DefaultVersion) {
            equals = equals((DefaultVersion) obj);
        } else if (obj instanceof Version) {
            equals = equals(new DefaultVersion(((Version) obj).getValue()));
        } else {
            equals = false;
        }

        return equals;
    }

    /**
     * @param version the version
     * @return true if the provided version is equals to this version
     */
    public boolean equals(DefaultVersion version)
    {
        return this.aetherVersion.equals(version.aetherVersion);
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
        setVersion((String) in.readObject());
    }
}
