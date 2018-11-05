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
package org.xwiki.rendering.async.internal;

import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Request;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;

/**
 * The request of the asynchronous renderer job.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public class AsyncRendererJobRequest extends AbstractRequest
{
    private static final String PROPERTY_RIGHT = "right";

    private static final String PROPERTY_RIGHT_ENTITY = "right.entity";

    private transient AsyncRenderer renderer;

    /**
     * The default constructor.
     */
    public AsyncRendererJobRequest()
    {
        setVerbose(false);
    }

    /**
     * @param request the request to copy
     */
    public AsyncRendererJobRequest(Request request)
    {
        super(request);

        setVerbose(false);
    }

    /**
     * @return the renderer to execute
     */
    public AsyncRenderer getRenderer()
    {
        return this.renderer;
    }

    /**
     * @param renderer the renderer to execute
     */
    public void setRenderer(AsyncRenderer renderer)
    {
        this.renderer = renderer;
    }

    /**
     * @param right the right required to access the result
     * @param rightEntity the reference on which the right is required to access the result
     */
    public void setRight(Right right, EntityReference rightEntity)
    {
        setProperty(PROPERTY_RIGHT, right);
        setProperty(PROPERTY_RIGHT_ENTITY, rightEntity);
    }

    /**
     * @return the right required to access the result
     */
    public Right getRight()
    {
        return getProperty(PROPERTY_RIGHT);
    }

    /**
     * @return the reference on which the right is required to access the result
     */
    public EntityReference getRightEntity()
    {
        return getProperty(PROPERTY_RIGHT_ENTITY);
    }
}
