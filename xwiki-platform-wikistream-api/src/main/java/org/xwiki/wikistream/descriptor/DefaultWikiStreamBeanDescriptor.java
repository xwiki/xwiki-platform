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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.PropertyDescriptor;

/**
 * @version $Id$
 */
public class DefaultWikiStreamBeanDescriptor implements WikiStreamBeanDescriptor
{
    /**
     * @see #getName()
     */
    private String name;

    /**
     * The description of the macro.
     */
    private String description;

    /**
     * The description of the properties bean.
     */
    private BeanDescriptor propertiesBeanDescriptor;

    /**
     * A map containing the {@link WikiStreamPropertyDescriptor} for each parameters supported for this wiki stream.
     * <p>
     * The {@link Map} keys are lower cased for easier case insensitive search, to get the "real" name of the property
     * use {@link WikiStreamPropertyDescriptor#getName()}.
     */
    private Map<String, WikiStreamPropertyDescriptor< ? >> parameterDescriptorMap =
        new LinkedHashMap<String, WikiStreamPropertyDescriptor< ? >>();

    /**
     * @param name
     * @param description
     * @param parametersBeanDescriptor
     */
    public DefaultWikiStreamBeanDescriptor(String name, String description, BeanDescriptor parametersBeanDescriptor)
    {
        this.name = name;
        this.description = description;
        this.propertiesBeanDescriptor = parametersBeanDescriptor;
    }

    protected void extractParameters()
    {
        for (PropertyDescriptor propertyDescriptor : this.propertiesBeanDescriptor.getProperties()) {
            DefaultWikiStreamBeanParameterDescriptor< ? > desc =
                new DefaultWikiStreamBeanParameterDescriptor<Object>(propertyDescriptor);
            this.parameterDescriptorMap.put(desc.getId().toLowerCase(), desc);
        }
    }

    // WikiStreamDescriptor

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public <T> WikiStreamPropertyDescriptor<T> getPropertyDescriptor(String propertyName)
    {
        return (WikiStreamPropertyDescriptor<T>) this.parameterDescriptorMap.get(propertyName);
    }

    @Override
    public Collection<WikiStreamPropertyDescriptor< ? >> getProperties()
    {
        return Collections.<WikiStreamPropertyDescriptor< ? >> unmodifiableCollection(this.parameterDescriptorMap
            .values());
    }

    // WikiStreamBeanDescriptor

    @Override
    public Class< ? > getBeanClass()
    {
        return this.propertiesBeanDescriptor.getBeanClass();
    }
}
