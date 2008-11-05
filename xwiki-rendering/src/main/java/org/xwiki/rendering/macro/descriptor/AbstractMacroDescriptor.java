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
package org.xwiki.rendering.macro.descriptor;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describe a macro.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public abstract class AbstractMacroDescriptor implements MacroDescriptor
{
    /**
     * The description of the macro.
     */
    private String description;

    /**
     * The class of the JAVA bean containing macro parameters.
     */
    private Class< ? > parametersBeanClass;

    /**
     * A map containing the {@link ParameterDescriptor} for each parameters supported for this macro.
     * <p>
     * The {@link Map} keys are lower cased for easier case insensitive search, to get the "real" name of the property
     * use {@link ParameterDescriptor#getName()}.
     */
    private Map<String, ParameterDescriptor> parameterDescriptorMap = new LinkedHashMap<String, ParameterDescriptor>();

    public AbstractMacroDescriptor(String description, Class< ? > parametersBeanClass)
    {
        this.description = description;
        this.parametersBeanClass = parametersBeanClass;
    }

    /**
     * Extract parameters informations from {@link #parametersBeanClass} and insert it in {@link #parameterDescriptorMap}.
     * @since 1.7M2
     */
    protected void extractParameterDescriptorMap()
    {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(this.parametersBeanClass);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            if (propertyDescriptors != null) {
                for (int i = 0; i < propertyDescriptors.length; i++) {
                    PropertyDescriptor propertyDescriptor = propertyDescriptors[i];
                    if (propertyDescriptor != null) {
                        extractParameterDescriptor(propertyDescriptor);
                    }
                }
            }
        } catch (IntrospectionException e) {
            // TODO: add error log here
        }
    }

    /**
     * Extract provided parameters informations and insert it in {@link #parameterDescriptorMap}.
     * 
     * @param propertyDescriptor the JAVA bean property descriptor.
     * @since 1.7M2
     */
    protected void extractParameterDescriptor(PropertyDescriptor propertyDescriptor)
    {
        DefaultParameterDescriptor desc = new DefaultParameterDescriptor();
        desc.setName(propertyDescriptor.getName());
        desc.setType(propertyDescriptor.getPropertyType());

        Method writeMethod = propertyDescriptor.getWriteMethod();

        if (writeMethod != null) {
            Method readMethod = propertyDescriptor.getReadMethod();

            String description;

            ParameterDescription parameterDescription = writeMethod.getAnnotation(ParameterDescription.class);

            if (parameterDescription == null && readMethod != null) {
                parameterDescription = readMethod.getAnnotation(ParameterDescription.class);
            }

            if (parameterDescription != null) {
                description = parameterDescription.value();
            } else {
                description = propertyDescriptor.getShortDescription();
            }

            desc.setDescription(description);

            this.parameterDescriptorMap.put(desc.getName().toLowerCase(), desc);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.MacroDescriptor#getDescription()
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.MacroDescriptor#getParametersBeanClass()
     */
    public Class< ? > getParametersBeanClass()
    {
        return this.parametersBeanClass;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.MacroDescriptor#getParameterDescriptorMap()
     */
    public Map<String, ParameterDescriptor> getParameterDescriptorMap()
    {
        return Collections.unmodifiableMap(this.parameterDescriptorMap);
    }
}
