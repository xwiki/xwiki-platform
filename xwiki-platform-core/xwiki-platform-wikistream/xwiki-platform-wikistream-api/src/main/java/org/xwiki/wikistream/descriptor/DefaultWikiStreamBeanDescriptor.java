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

import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.PropertyDescriptor;
import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 * @since 5.2M2
 */
@Unstable
public class DefaultWikiStreamBeanDescriptor extends AbstractWikiStreamDescriptor implements WikiStreamBeanDescriptor
{
    /**
     * The description of the properties bean.
     */
    private BeanDescriptor propertiesBeanDescriptor;

    /**
     * @param name
     * @param description
     * @param parametersBeanDescriptor
     */
    public DefaultWikiStreamBeanDescriptor(String name, String description, BeanDescriptor parametersBeanDescriptor)
    {
        super(name, description);

        this.propertiesBeanDescriptor = parametersBeanDescriptor;

        extractParameters();
    }

    protected void extractParameters()
    {
        for (PropertyDescriptor propertyDescriptor : this.propertiesBeanDescriptor.getProperties()) {
            DefaultWikiStreamBeanParameterDescriptor< ? > desc =
                new DefaultWikiStreamBeanParameterDescriptor<Object>(propertyDescriptor);
            this.parameterDescriptorMap.put(desc.getId().toLowerCase(), desc);
        }
    }

    // WikiStreamBeanDescriptor

    @Override
    public Class< ? > getBeanClass()
    {
        return this.propertiesBeanDescriptor.getBeanClass();
    }
}
