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
package org.xwiki.activeinstalls2.internal.data;

import java.util.Collection;

/**
 * Represents extension-related Ping data.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class ExtensionPing
{
    private String id;

    private String version;

    private Collection<String> features;

    /**
     * @return the Extension's unique id (format {@code groupId:artifactId})
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id see {@link #getId()}
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the Extension's version
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version see {@link #getVersion()}
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * @return the list of features (i.e. aliases) for the Extension
     */
    public Collection<String> getFeatures()
    {
        return this.features;
    }

    /**
     * @param features see {@link #getFeatures()}
     */
    public void setFeatures(Collection<String> features)
    {
        this.features = features;
    }
}

