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
package org.xwiki.filter.instance.internal.input;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.descriptor.DefaultFilterStreamBeanDescriptor;
import org.xwiki.filter.instance.input.AbstractEntityEventGenerator;
import org.xwiki.filter.instance.input.EntityEventGenerator;
import org.xwiki.properties.BeanManager;

/**
 * @param <E> the type of the entity (XWikiDocument, BaseObject, BaseClass, etc.)
 * @param <F> the type of the filter declaring the events supported by this {@link EntityEventGenerator}
 * @param <P> the type of the properties bean
 * @version $Id$
 * @since 6.2M1
 */
public abstract class AbstractBeanEntityEventGenerator<E, F, P> extends AbstractEntityEventGenerator<E, F>
{
    @Inject
    private BeanManager beanManager;

    private String name;

    private String description;

    private Class<P> propertiesType;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        // Get the type of the properties
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(AbstractBeanEntityEventGenerator.class, getClass());
        this.propertiesType = ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[2]);

        // Initialize FilterStream Descriptor.
        DefaultFilterStreamBeanDescriptor descriptor =
            new DefaultFilterStreamBeanDescriptor(getName(), getDescription(), this.beanManager
                .getBeanDescriptor(!this.propertiesType.isInterface() ? this.propertiesType : Object.class));

        setDescriptor(descriptor);
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the description
     */
    public String getDescription()
    {
        return this.description;
    }

    @Override
    protected void write(E entity, Object filter, F internalFilter, Map<String, Object> properties)
        throws FilterException
    {
        P propertiesBean;

        if (this.propertiesType.isInstance(properties)) {
            propertiesBean = (P) properties;
        } else {
            try {
                propertiesBean = this.propertiesType.newInstance();

                this.beanManager.populate(propertiesBean, properties);
            } catch (Exception e) {
                throw new FilterException("Failed to convert properties to Java bean", e);
            }
        }

        write(entity, filter, internalFilter, propertiesBean);
    }

    protected abstract void write(E entity, Object filter, F internalFilter, P properties) throws FilterException;
}
