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
package org.xwiki.wikistream.internal;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.properties.BeanManager;
import org.xwiki.wikistream.WikiStreamFactory;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.descriptor.DefaultWikiStreamBeanDescriptor;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * @param <P>
 * @version $Id$
 * @since 5.2M2
 */
public abstract class AbstractBeanWikiStreamFactory<P> extends AbstractWikiStream implements WikiStreamFactory, Initializable
{
    /**
     * The {@link BeanManager} component.
     */
    @Inject
    protected BeanManager beanManager;

    private String name;

    private String description;

    /**
     * Properties bean class used to generate the macro descriptor.
     */
    private Class<P> propertiesBeanClass;

    public AbstractBeanWikiStreamFactory(WikiStreamType type)
    {
        super(type);
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Get bean properties type
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(AbstractBeanWikiStreamFactory.class, getClass());
        this.propertiesBeanClass = ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[0]);

        // Initialize WikiStream Descriptor.
        DefaultWikiStreamBeanDescriptor descriptor =
            new DefaultWikiStreamBeanDescriptor(getName(), getDescription(),
                this.beanManager.getBeanDescriptor(this.propertiesBeanClass));

        setDescriptor(descriptor);
    }

    protected P createPropertiesBean(Map<String, Object> properties) throws WikiStreamException
    {
        Class<P> beanClass = getPropertiesBeanClass();

        if (beanClass.isInstance(properties)) {
            return (P) properties;
        }

        P parametersBean;
        try {
            parametersBean = beanClass.newInstance();

            this.beanManager.populate(parametersBean, properties);
        } catch (Exception e) {
            throw new WikiStreamException(String.format("Failed to read parameters [%s]", properties), e);
        }

        return parametersBean;
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

    /**
     * @param propertiesBeanClass the parametersBeanClass to set
     */
    public void setPropertiesBeanClass(Class<P> propertiesBeanClass)
    {
        this.propertiesBeanClass = propertiesBeanClass;
    }

    /**
     * @return the properties bean class
     */
    public Class<P> getPropertiesBeanClass()
    {
        return this.propertiesBeanClass;
    }
}
