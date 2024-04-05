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
package com.xpn.xwiki.internal.skin;

import org.xwiki.filter.input.InputSource;
import org.xwiki.skin.Resource;
import org.xwiki.skin.ResourceRepository;

/**
 * @param <I> the type of the {@link InputSource}
 * @version $Id$
 * @since 6.4M1
 */
public abstract class AbstractResource<I extends InputSource> implements Resource<I>
{
    protected ResourceRepository repository;

    protected String id;

    protected String path;

    protected String resourceName;

    protected AbstractResource(String id, String path, String resourceName, ResourceRepository repository)
    {
        this.id = id;
        this.path = path;
        this.resourceName = resourceName;
        this.repository = repository;
    }

    @Override
    public ResourceRepository getRepository()
    {
        return this.repository;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getPath()
    {
        return this.path;
    }

    @Override
    public String getResourceName()
    {
        return this.resourceName;
    }

    @Override
    public String toString()
    {
        return getId();
    }
}
