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
package org.xwiki.url.internal;

import java.util.List;

import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceType;

/**
 * The reference of a {@link ResourceReferenceHandler} leading to other sub-{@link ResourceReferenceHandler}s.
 * 
 * @version $Id$
 * @since 10.2
 */
public class ParentResourceReference extends AbstractResourceReference
{
    private String rootPath;

    private String child;

    private List<String> childSegments;

    /**
     * Default constructor.
     * 
     * @param type see {@link #getType()}
     * @param rootPath the path starting with the child
     * @param child the child handler hint
     * @param pathSegments the rest of the path
     */
    public ParentResourceReference(ResourceType type, String rootPath, String child, List<String> pathSegments)
    {
        setType(type);

        this.rootPath = rootPath;
        this.child = child;
        this.childSegments = pathSegments;
    }

    /**
     * @return the path starting with the child
     */
    public String getRootPath()
    {
        return this.rootPath;
    }

    /**
     * @return the child hint
     */
    public String getChild()
    {
        return this.child;
    }

    /**
     * @return the child path (elements after the child)
     */
    public List<String> getPathSegments()
    {
        return this.childSegments;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("path = ");
        builder.append(getRootPath());
        builder.append(", child = ");
        builder.append(getChild());
        builder.append(", pathSegments = ");
        builder.append(getPathSegments());

        return builder.toString();
    }
}
