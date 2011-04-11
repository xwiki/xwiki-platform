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
package org.xwiki.wikistream;

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.properties.BeanManager;
import org.xwiki.wikistream.descriptor.WikiStreamDescriptor;

/**
 * @version $Id$
 */
public abstract class AbstractOutputWikiStream<P> extends AbstractLogEnabled implements WikiStream<P>, Initializable
{

    /**
     * The {@link BeanManager} component.
     */
    @Requirement
    protected BeanManager beanManager;

    private String name;

    private String description;

    private WikiStreamDescriptor descriptor;

    /**
     * Parameter bean class used to generate the macro descriptor.
     */
    private Class< ? > parametersBeanClass;

    public AbstractOutputWikiStream(String name, String description, WikiStreamDescriptor descriptor)
    {
        this.setName(name);
        this.setDescription(description);
        this.setDescriptor(descriptor);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // TODO Auto-generated method stub

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
        return name;
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
        return description;
    }

    /**
     * @param parametersBeanClass the parametersBeanClass to set
     */
    public void setParametersBeanClass(Class< ? > parametersBeanClass)
    {
        this.parametersBeanClass = parametersBeanClass;
    }

    /**
     * @return the parametersBeanClass
     */
    public Class< ? > getParametersBeanClass()
    {
        return parametersBeanClass;
    }

    /**
     * @param descriptor the descriptor to set
     */
    public void setDescriptor(WikiStreamDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    /**
     * @return the descriptor
     */
    public WikiStreamDescriptor getDescriptor()
    {
        return descriptor;
    }

}
