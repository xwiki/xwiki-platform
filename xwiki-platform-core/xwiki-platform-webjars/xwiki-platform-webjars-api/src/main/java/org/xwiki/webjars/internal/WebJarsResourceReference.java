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
package org.xwiki.webjars.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;

/**
 * Represents a reference to a WebJar resource.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class WebJarsResourceReference extends AbstractResourceReference
{
    /**
     * Represents a WebJars Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType("webjars");

    private static final char RESOURCE_PATH_SEPARATOR = '/';

    private String namespace;

    private List<String> resourceSegments;

    /**
     * @param namespace see {@link #getNamespace()}
     * @param resourceSegments see {@link #getResourceSegments()}
     */
    public WebJarsResourceReference(String namespace, List<String> resourceSegments)
    {
        setType(TYPE);
        this.namespace = namespace;
        this.resourceSegments = new ArrayList<>(resourceSegments);
    }

    /**
     * @return the list of segments making up the WebJar Resource (e.g. {@code ["angular", "2.1.11", "angular.js"]} for
     *         a Resource located in a {@code angular/2.1.11/angular.js} directory in a WebJar JAR
     */
    public List<String> getResourceSegments()
    {
        return this.resourceSegments;
    }

    /**
     * @return the String representation with "/" separating each resource segment, e.g.
     *         {@code angular/2.1.11/angular.js}
     */
    public String getResourceName()
    {
        return StringUtils.join(getResourceSegments(), RESOURCE_PATH_SEPARATOR);
    }

    /**
     * @return the namespace under which the webjars resource can be found. A name can be for example a wiki (e.g.
     *         {@code wiki:<wikiId>})
     */
    public String getNamespace()
    {
        return this.namespace;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(5, 5)
            .append(getResourceSegments())
            .append(getType())
            .append(getNamespace())
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
        WebJarsResourceReference rhs = (WebJarsResourceReference) object;
        return new EqualsBuilder()
            .append(getResourceSegments(), rhs.getResourceSegments())
            .append(getType(), rhs.getType())
            .append(getNamespace(), rhs.getNamespace())
            .append(getParameters(), rhs.getParameters())
            .isEquals();
    }
}
