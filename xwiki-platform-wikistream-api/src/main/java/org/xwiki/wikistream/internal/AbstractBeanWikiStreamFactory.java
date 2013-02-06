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

import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.properties.BeanManager;
import org.xwiki.wikistream.WikiStream;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.descriptor.DefaultWikiStreamDescriptor;
import org.xwiki.wikistream.type.WikiStreamType;

public abstract class AbstractBeanWikiStreamFactory<P> extends AbstractWikiStream implements WikiStream, Initializable
{
    /**
     * The {@link BeanManager} component.
     */
    @Inject
    protected BeanManager beanManager;

    private String name;

    private String description;

    /**
     * Parameter bean class used to generate the macro descriptor.
     */
    private Class<P> parametersBeanClass;

    public AbstractBeanWikiStreamFactory(WikiStreamType type)
    {
        super(type);
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Initialise WikiStream Descriptor.
        DefaultWikiStreamDescriptor descriptor =
            new DefaultWikiStreamDescriptor(getName(), getDescription(),
                this.beanManager.getBeanDescriptor(this.parametersBeanClass));

        setDescriptor(descriptor);
    }

    protected P createParametersBean(Map<String, Object> parameters) throws WikiStreamException
    {
        P parametersBean;
        try {
            parametersBean = getParametersBeanClass().newInstance();

            this.beanManager.populate(parameters, parameters);
        } catch (Exception e) {
            throw new WikiStreamException("Failed to read parameters [" + parameters + "]", e);
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
     * @param parametersBeanClass the parametersBeanClass to set
     */
    public void setParametersBeanClass(Class<P> parametersBeanClass)
    {
        this.parametersBeanClass = parametersBeanClass;
    }

    /**
     * @return the parametersBeanClass
     */
    public Class<P> getParametersBeanClass()
    {
        return this.parametersBeanClass;
    }
}
