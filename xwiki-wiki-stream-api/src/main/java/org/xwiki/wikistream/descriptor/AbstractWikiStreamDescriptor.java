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
package org.xwiki.wikistream.descriptor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.PropertyDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultParameterDescriptor;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * @version $Id$
 */
public class AbstractWikiStreamDescriptor implements WikiStreamDescriptor
{

    private WikiStreamType type;

    /**
     * @see #getName()
     */
    private String name;

    /**
     * The description of the macro.
     */
    private String description;

    /**
     * The description of the parameters bean.
     */
    private BeanDescriptor parametersBeanDescriptor;

    /**
     * A map containing the {@link WikiStreamParameterDescriptor} for each parameters supported for this wiki stream.
     * <p>
     * The {@link Map} keys are lower cased for easier case insensitive search, to get the "real" name of the property
     * use {@link WikiStreamParameterDescriptor#getName()}.
     */
    private Map<String, WikiStreamParameterDescriptor> parameterDescriptorMap =
        new LinkedHashMap<String, WikiStreamParameterDescriptor>();

    /**
     * //TODO - Documentation
     * @param type
     * @param name
     * @param description
     * @param parametersBeanDescriptor
     */
    public AbstractWikiStreamDescriptor(WikiStreamType type, String name, String description,
        BeanDescriptor parametersBeanDescriptor)
    {
        super();
        this.type = type;
        this.name = name;
        this.description = description;
        this.parametersBeanDescriptor = parametersBeanDescriptor;
    }
    
    protected void extractParameters(){
        for (PropertyDescriptor propertyDescriptor : parametersBeanDescriptor.getProperties()) {
            DefaultWikiStreamParameterDescriptor desc = new DefaultWikiStreamParameterDescriptor(propertyDescriptor);
            this.parameterDescriptorMap.put(desc.getId().toLowerCase(), desc);
        }
    }
    
    public WikiStreamType getType(){
        return type;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.descriptor.WikiStreamDescriptor#getName()
     */
    public String getName()
    {
        // TODO Auto-generated method stub
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.descriptor.WikiStreamDescriptor#getDescription()
     */
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return description;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.descriptor.WikiStreamDescriptor#getParametersBeanClass()
     */
    public Class< ? > getParametersBeanClass()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.descriptor.WikiStreamDescriptor#getParameterDescriptorMap()
     */
    public Map<String, WikiStreamParameterDescriptor> getParameterDescriptorMap()
    {
        return parameterDescriptorMap;
    }

}
