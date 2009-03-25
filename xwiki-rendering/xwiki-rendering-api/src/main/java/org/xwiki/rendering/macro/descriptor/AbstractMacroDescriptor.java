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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.rendering.macro.descriptor.annotation.ParameterDescription;
import org.xwiki.rendering.macro.descriptor.annotation.ParameterHidden;
import org.xwiki.rendering.macro.descriptor.annotation.ParameterMandatory;

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
     * Define a macro content.
     */
    private ContentDescriptor contentDescriptor;

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

    /**
     * @param description the description of the macro.
     * @param contentDescriptor the description of the macro content. null indicate macro does not support content.
     * @param parametersBeanClass the class of the JAVA bean containing macro parameters.
     */
    public AbstractMacroDescriptor(String description, ContentDescriptor contentDescriptor,
        Class< ? > parametersBeanClass)
    {
        this.description = description;
        this.parametersBeanClass = parametersBeanClass;
        this.contentDescriptor = contentDescriptor;
    }

    /**
     * Extract parameters informations from {@link #parametersBeanClass} and insert it in
     * {@link #parameterDescriptorMap}.
     * 
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

            ParameterHidden parameterHidden =
                extractParameterAnnotation(writeMethod, readMethod, ParameterHidden.class);

            if (parameterHidden == null) {
                ParameterDescription parameterDescription =
                    extractParameterAnnotation(writeMethod, readMethod, ParameterDescription.class);

                desc.setDescription(parameterDescription != null ? parameterDescription.value() : propertyDescriptor
                    .getShortDescription());

                ParameterMandatory parameterMandatory =
                    extractParameterAnnotation(writeMethod, readMethod, ParameterMandatory.class);

                desc.setMandatory(parameterMandatory != null);

                this.parameterDescriptorMap.put(desc.getName().toLowerCase(), desc);
            }
        }
    }

    /**
     * Get the parameter annotation. Try first on the setter then on the getter if no annotation has been found.
     * 
     * @param <T> the Class object corresponding to the annotation type.
     * @param writeMethod the method that should be used to write the property value.
     * @param readMethod the method that should be used to read the property value.
     * @param annotationClass the Class object corresponding to the annotation type.
     * @return this element's annotation for the specified annotation type if present on this element, else null.
     * @since 1.7
     */
    protected <T extends Annotation> T extractParameterAnnotation(Method writeMethod, Method readMethod,
        Class<T> annotationClass)
    {
        T parameterDescription = writeMethod.getAnnotation(annotationClass);

        if (parameterDescription == null && readMethod != null) {
            parameterDescription = readMethod.getAnnotation(annotationClass);
        }

        return parameterDescription;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.MacroDescriptor#getContentDescriptor()
     */
    public ContentDescriptor getContentDescriptor()
    {
        return this.contentDescriptor;
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
