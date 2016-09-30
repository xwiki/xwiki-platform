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
package org.xwiki.filter.instance.input;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.descriptor.FilterStreamDescriptor;

/**
 * @param <E> the type of the entity (XWikiDocument, BaseObject, BaseClass, etc.)
 * @param <F> the type of the filter declaring the events supported by this {@link EntityEventGenerator}
 * @version $Id$
 * @since 6.2M1
 */
public abstract class AbstractEntityEventGenerator<E, F> implements EntityEventGenerator<E>, Initializable
{
    @Inject
    private FilterDescriptorManager filterDescriptorManager;

    protected Class<F> filterType;

    protected FilterStreamDescriptor descriptor;

    @Override
    public void initialize() throws InitializationException
    {
        // Get the type of the internal filter
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(AbstractEntityEventGenerator.class, getClass());
        this.filterType = ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[1]);
    }

    @Override
    public FilterStreamDescriptor getDescriptor()
    {
        return this.descriptor;
    }

    protected void setDescriptor(FilterStreamDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    @Override
    public void write(E entity, Object filter, Map<String, Object> properties) throws FilterException
    {
        F internalFilter = this.filterDescriptorManager.createFilterProxy(filter, this.filterType);

        write(entity, filter, internalFilter, properties);
    }

    protected abstract void write(E entity, Object filter, F internalFilter, Map<String, Object> properties)
        throws FilterException;
}
