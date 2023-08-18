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
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
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

    private static final String SCHEME_SEPARATOR = ":";

    private static final WikiReference XWIKI_REFERENCE = new WikiReference("xwiki");

    private String reference;

    private String scheme;

    private URI uri;

    private List<String> pathSegments;

    /**
     * Create a new reference by copying the passed one.
     *
     * @param reference the reference to copy
     * @since 12.3RC1
     */
    public VfsResourceReference(VfsResourceReference reference)
    {
        this(reference.uri, reference.pathSegments);
    }

    /**
     * @param uri the URI pointing to the archive (without the path inside the archive),
     * e.g. {@code attach:space.page@attachment}
     * @param pathSegments see {@link #getPathSegments()}
     */
    public VfsResourceReference(URI uri, List<String> pathSegments)
    {
        // FIXME: we don't know the wiki of the resource yet, putting main one
        super(XWIKI_REFERENCE, EntityResourceAction.fromString(""), uri.getFragment());

        setType(TYPE);
        this.uri = uri;
        this.reference = this.uri.getSchemeSpecificPart();
        this.scheme = uri.getScheme();
        this.pathSegments = new ArrayList<>(pathSegments);
    }

    /**
     * @param uri the URI pointing to the archive (without the path inside the archive),
     * e.g. {@code attach:space.page@attachment}
     * @param pathSegments see {@link #getPathSegments()}, specified as "/"-separated string (e.g. "path/to/file")
     */
    public VfsResourceReference(URI uri, String pathSegments)
    {
        this(uri, Arrays.asList(StringUtils.split(pathSegments, RESOURCE_PATH_SEPARATOR)));
    }

    /**
     * @param fullURI the full opaque URI containing both the reference to the archive and the path to the entry inside
     * it, e.g. {@code attach:space.page@attachment/path/to/file}. Note that this constructor requires that the
     * full URL to be URL-encoded.
     * @deprecated Since 12.4RC1, this constructor shouldn't be used anymore, in particular not for internal
     * references such as {@code attach:space.page@attachment/path/to/file}, {@link #VfsResourceReference(String)}
     * should be used instead
     */
    @Deprecated
    public VfsResourceReference(URI fullURI)
    {
        // Find the first "/" and consider that everything after is the path
        this(URI.create(retrieveRootReference(fullURI.toString())), retrievePathSegment(fullURI.toString()));
    }

    /**
     * @param fullReference the full opaque reference containing both the reference to the archive and the path to the
     * entry inside it, e.g. {@code attach:space.page@attachment/path/to/file}.
     * @since 12.4RC1
     */
    public VfsResourceReference(String fullReference)
    {
        super(XWIKI_REFERENCE, EntityResourceAction.fromString(""));

        setType(TYPE);
        String referenceWithScheme = retrieveRootReference(fullReference);
        this.scheme = StringUtils.substringBefore(referenceWithScheme, SCHEME_SEPARATOR);
        this.reference = StringUtils.substringAfter(referenceWithScheme, SCHEME_SEPARATOR);
        this.pathSegments = Arrays.asList(
            StringUtils.split(retrievePathSegment(fullReference), RESOURCE_PATH_SEPARATOR));
    }

    private static String retrieveRootReference(String fullReference)
    {
        return StringUtils.substringBefore(fullReference, RESOURCE_PATH_SEPARATOR);
    }

    private static String retrievePathSegment(String fullReference)
    {
        return StringUtils.substringAfter(fullReference, RESOURCE_PATH_SEPARATOR);
    }

    /**
     * @return the URI to the VFS (e.g. {@code attach:space.page@file.zip}, {@code http://server/path/to/zip})
     * @deprecated Since 12.4RC1 this method shouldn't be used anymore, in favor of {@link #getReference}.
     */
    @Deprecated
    public URI getURI()
    {
        if (this.uri == null) {
            this.uri = URI.create(String.format("%s:%s", getScheme(), getReference()));
        }
        return this.uri;
    }

    /**
     * @return the actual reference to the VFS (e.g. {@code attach:space.page@file.zip}).
     * @since 12.4RC1
     */
    public String getReference()
    {
        return this.reference;
    }

    /**
     * @return the scheme of this reference (e.g. {@code attach}).
     * @since 12.4RC1
     */
    public String getScheme()
    {
        return this.scheme;
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
            .append(getScheme())
            .append(getReference())
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
            .append(getScheme(), rhs.getScheme())
            .append(getReference(), rhs.getReference())
            .append(getPathSegments(), rhs.getPathSegments())
            .append(getType(), rhs.getType())
            .append(getParameters(), rhs.getParameters())
            .isEquals();
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.append("scheme", getScheme());
        builder.append("reference", getReference());
        builder.append("path", getPath());
        builder.append("parameters", getParameters());
        return builder.toString();
    }

    /**
     * @return the resource reference as a URI
     */
    public URI toURI()
    {
        return URI.create(String.format("%s:%s/%s", getScheme(), getReference(), getPath()));
    }

    /**
     * @return the Content-Type to return with the response
     * @since 12.3RC1
     */
    public String getContentType()
    {
        return getParameterValue(PARAMETER_CONTENTTYPE);
    }

    /**
     * @param contentType the Content-Type to return with the response
     * @since 12.3RC1
     */
    public void setContentType(String contentType)
    {
        setParameter(PARAMETER_CONTENTTYPE, contentType);
    }
}
