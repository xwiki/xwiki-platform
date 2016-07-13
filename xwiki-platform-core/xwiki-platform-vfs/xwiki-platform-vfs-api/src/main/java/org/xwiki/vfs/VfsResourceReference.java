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
package org.xwiki.vfs;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents a reference to a VFS resource.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Unstable
public class VfsResourceReference extends AbstractResourceReference
{
    /**
     * Represents a VFS Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType("vfs");

    private static final String RESOURCE_PATH_SEPARATOR = "/";

    private URI uri;

    private List<String> pathSegments;

    /**
     * @param uri the URI pointing to the archive (without the path inside the archive),
     *       e.g. {@code attach:space.page@attachment}
     * @param pathSegments see {@link #getPathSegments()}
     */
    public VfsResourceReference(URI uri, List<String> pathSegments)
    {
        setType(TYPE);
        this.uri = uri;
        this.pathSegments = new ArrayList<>(pathSegments);
    }

    /**
     * @param uri the URI pointing to the archive (without the path inside the archive),
     *       e.g. {@code attach:space.page@attachment}
     * @param pathSegments see {@link #getPathSegments()}, specified as "/"-separated string (e.g. "path/to/file")
     */
    public VfsResourceReference(URI uri, String pathSegments)
    {
        this(uri, Arrays.asList(StringUtils.split(pathSegments, RESOURCE_PATH_SEPARATOR)));
    }

    /**
     * @param fullURI the full opaque URI containing both the reference to the archive and the path to the entry inside
     *        it, e.g. {@code attach:space.page@attachment/path/to/file}. Note that this constructor requires that any
     *        "/" character inside the reference to the archive be URL-encoded
     */
    public VfsResourceReference(URI fullURI)
    {
        // Find the first "/" and consider that everything after is the path
        this(URI.create(StringUtils.substringBefore(fullURI.toString(), RESOURCE_PATH_SEPARATOR)),
            StringUtils.substringAfter(fullURI.toString(), RESOURCE_PATH_SEPARATOR));
    }

    /**
     * @return the URI to the VFS (e.g. {@code attach:space.page@file.zip}, {@code http://server/path/to/zip})
     */
    public URI getURI()
    {
        return this.uri;
    }

    /**
     * @return the list of segments pointing to the relative location of a resource in the VFS (e.g. {@code {"some",
     * "directory", "file.txt"}} for {@code some/directory/file.txt}
     */
    public List<String> getPathSegments()
    {
        return this.pathSegments;
    }

    /**
     * @return the String representation with "/" separating each VFS path segment, e.g. {@code some/directory/file.txt}
     */
    public String getPath()
    {
        return StringUtils.join(getPathSegments(), RESOURCE_PATH_SEPARATOR);
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(7, 7)
            .append(getURI())
            .append(getPathSegments())
            .append(getType())
            .append(getParameters())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        VfsResourceReference rhs = (VfsResourceReference) object;
        return new EqualsBuilder()
            .append(getURI(), rhs.getURI())
            .append(getPathSegments(), rhs.getPathSegments())
            .append(getType(), rhs.getType())
            .append(getParameters(), rhs.getParameters())
            .isEquals();
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.append("uri", getURI());
        builder.append("path", getPath());
        builder.append("parameters", getParameters());
        return builder.toString();
    }

    /**
     * @return the resource reference as a URI
     */
    public URI toURI()
    {
        return URI.create(String.format("%s/%s", getURI().toString(), getPath()));
    }
}
