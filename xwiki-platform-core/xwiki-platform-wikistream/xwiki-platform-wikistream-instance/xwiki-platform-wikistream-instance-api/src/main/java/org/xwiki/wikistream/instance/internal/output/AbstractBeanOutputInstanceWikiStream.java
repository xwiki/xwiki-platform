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
package org.xwiki.wikistream.instance.internal.output;

import java.lang.reflect.ParameterizedType;

import javax.inject.Inject;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.properties.BeanManager;
import org.xwiki.wikistream.WikiStreamException;

/**
 * @param <P>
 * @version $Id$
 * @since 5.2
 */
public abstract class AbstractBeanOutputInstanceWikiStream<P> implements BeanOutputInstanceWikiStream<P>, Initializable
{
    @Inject
    private BeanManager beanManager;

    private Class<P> propertiesType;

    protected P properties;

    @Override
    public void initialize() throws InitializationException
    {
        // Get the type of the properties
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(AbstractBeanOutputInstanceWikiStream.class, getClass());
        this.propertiesType = ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[0]);
    }

    @Override
    public void setProperties(P properties) throws WikiStreamException
    {
        this.properties = properties;
    }
    
    @Override
    public Object getFilter() throws WikiStreamException
    {
        return this;
    }
}
