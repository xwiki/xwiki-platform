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
package org.xwiki.rendering.async.internal.service;

import java.util.List;

import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;

/**
 * The reference of an asynchronous renderer.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public class AsyncRendererResourceReference extends AbstractResourceReference
{
    private final List<String> id;

    private final String clientId;

    /**
     * Default constructor.
     * 
     * @param type see {@link #getType()}
     * @param id the id of the async renderer
     * @param clientId the id of the client associated with the async execution
     */
    public AsyncRendererResourceReference(ResourceType type, List<String> id, String clientId)
    {
        setType(type);

        this.id = id;
        this.clientId = clientId;
    }

    /**
     * @return the child path (elements after the child)
     */
    public List<String> getId()
    {
        return this.id;
    }

    /**
     * @return the clientId
     */
    public String getClientId()
    {
        return this.clientId;
    }

    @Override
    public String toString()
    {
        return getId().toString();
    }
}
