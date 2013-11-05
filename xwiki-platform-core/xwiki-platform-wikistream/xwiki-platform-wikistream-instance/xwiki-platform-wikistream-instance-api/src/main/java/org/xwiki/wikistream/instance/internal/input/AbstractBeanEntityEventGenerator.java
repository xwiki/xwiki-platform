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
package org.xwiki.wikistream.instance.internal.input;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.properties.BeanManager;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.input.AbstractEntityEventGenerator;

public abstract class AbstractBeanEntityEventGenerator<E, F, P> extends AbstractEntityEventGenerator<E, F>
{
    @Inject
    private BeanManager beanManager;

    private Class<P> propertiesType;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        // Get the type of the properties
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(AbstractBeanEntityEventGenerator.class, getClass());
        this.propertiesType = ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[2]);
    }

    @Override
    protected void write(E entity, Object filter, F internalFilter, Map<String, Object> properties)
        throws WikiStreamException
    {
        P propertiesBean;

        if (this.propertiesType.isInstance(properties)) {
            propertiesBean = (P) properties;
        } else {
            try {
                propertiesBean = this.propertiesType.newInstance();

                this.beanManager.populate(propertiesBean, properties);
            } catch (Exception e) {
                throw new WikiStreamException("Failed to convert properties to Java bean", e);
            }
        }

        write(entity, filter, internalFilter, propertiesBean);
    }

    protected abstract void write(E entity, Object filter, F internalFilter, P properties) throws WikiStreamException;
}
