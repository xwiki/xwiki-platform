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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents a reference to a VFS resource.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class VfsResourceReference extends EntityResourceReference
{
    /**
     * Represents a VFS Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType("vfs");

    private static final String PARAMETER_CONTENTTYPE = "content-type";

    private static final String RESOURCE_PATH_SEPARATOR = "/";

    private static final String DECODER_LOCALE = "UTF-8";

    private URI uri;

    private List<String> pathSegments;

    /**
     * Create a new reference by copying the passed one.
     * 
     * @param reference the reference to copy
     * @since 12.3RC1
     */
    @Unstable
    public VfsResourceReference(VfsResourceReference reference)
    {
        this(reference.uri, reference.pathSegments);
    }

    /**
     * @param uri the URI pointing to the archive (without the path inside the archive),
     *       e.g. {@code attach:space.page@attachment}
     * @param pathSegments see {@link #getPathSegments()}
     */
    public VfsResourceReference(URI uri, List<String> pathSegments)
    {
        // FIXME: we don't know the wiki of the resource yet, putting main one
        super(new WikiReference("xwiki"), EntityResourceAction.fromString(""));

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
     *        it, e.g. {@code attach:space.page@attachment/path/to/file}. Note that this constructor requires that the
     *        full URL to be URL-encoded.
     */
    public VfsResourceReference(URI fullURI)
    {
        // Find the first "/" and consider that everything after is the path
        this(retrieveRootURI(fullURI), retrievePathSegment(fullURI));
    }

    private static URI retrieveRootURI(URI fullURI)
    {
        try {
            return URI.create(StringUtils.substringBefore(
                URLDecoder.decode(fullURI.toString(), DECODER_LOCALE), RESOURCE_PATH_SEPARATOR));
        } catch (UnsupportedEncodingException e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    private static String retrievePathSegment(URI fullURI)
    {
        try {
            return StringUtils.substringAfter(
                URLDecoder.decode(fullURI.toString(), DECODER_LOCALE), RESOURCE_PATH_SEPARATOR);
        } catch (UnsupportedEncodingException e) {
            // should never happen
            throw new RuntimeException(e);
        }
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

    /**
     * @return the Content-Type to return with the response
     * @since 12.3RC1
     */
    @Unstable
    public String getContentType()
    {
        return getParameterValue(PARAMETER_CONTENTTYPE);
    }

    /**
     * @param contentType the Content-Type to return with the response
     * @since 12.3RC1
     */
    @Unstable
    public void setContentType(String contentType)
    {
        setParameter(PARAMETER_CONTENTTYPE, contentType);
    }
}
