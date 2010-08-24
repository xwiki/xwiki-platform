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
package org.xwiki.properties.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.PropertyDescriptor;
import org.xwiki.properties.PropertyException;
import org.xwiki.properties.PropertyMandatoryException;
import org.xwiki.properties.RawProperties;

/**
 * Default implementation of {@link BeanManager} component.
 * <ul>
 * <li>use hibernate-validator implementation of JSR 303</li>
 * <li>add support for any Enum conversion. See {@link import org.xwiki.properties.internal.converter.EnumConverter}.</li>
 * <li>add support for {@link java.awt.Color} conversion using "r,g,b" or "#xxxxxx" format. See
 * {@link org.xwiki.properties.internal.converter.ColorConverter}.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
public class DefaultBeanManager extends AbstractLogEnabled implements BeanManager
{
    /**
     * Cache the already parsed classes.
     */
    private Map<Class< ? >, BeanDescriptor> beanDescriptorCache = Collections
        .synchronizedMap(new HashMap<Class< ? >, BeanDescriptor>());

    /**
     * The {@link ConverterManager} component.
     */
    @Requirement
    private ConverterManager converterManager;

    /**
     * The factory to use to get new JSR 303 validators.
     */
    private ValidatorFactory validatorFactory;

    /**
     * @return the factory to use to get new JSR 303 validators.
     */
    public ValidatorFactory getValidatorFactory()
    {
        if (this.validatorFactory == null) {
            try {
                this.validatorFactory = Validation.buildDefaultValidatorFactory();
            } catch (ValidationException e) {
                getLogger().warn("Unable to find default JSR 303 provider. There will be no Java bean validation.");
            }
        }

        return this.validatorFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.BeanManager#populate(java.lang.Object, java.util.Map)
     */
    public void populate(Object bean, Map<String, ? > values) throws PropertyException
    {
        Map<String, Object> remainingValues = new HashMap<String, Object>(values);

        // Populate
        populateBean(bean, remainingValues);

        // If the bean implements RawProperties, inject remaining properties
        if (!remainingValues.isEmpty() && bean instanceof RawProperties) {
            RawProperties rawProperties = (RawProperties) bean;
            for (Map.Entry<String, Object> entry : remainingValues.entrySet()) {
                rawProperties.set(entry.getKey(), entry.getValue());
            }
        }

        // Validate
        validateBean(bean);
    }

    /**
     * Populate the provided bean with provided values.
     * <p>
     * <code>values</code> is "consumed": when method executing is finished it only contains not populated properties.
     * 
     * @param bean the java bean to populate
     * @param values the values to convert and inject in the java bean
     * @throws PropertyException error when populating the bean
     */
    private void populateBean(Object bean, Map<String, Object> values) throws PropertyException
    {
        BeanDescriptor beanDescriptor = getBeanDescriptor(bean.getClass());

        // Lower case provided properties to easily ignore properties name case
        Map<String, String> lowerKeyMap = new HashMap<String, String>(values.size());
        for (Map.Entry<String, ? > entry : values.entrySet()) {
            lowerKeyMap.put(entry.getKey().toLowerCase(), entry.getKey());
        }

        for (PropertyDescriptor propertyDescriptor : beanDescriptor.getProperties()) {
            String propertyId = propertyDescriptor.getId();
            Object value = values.get(propertyId);

            if (value == null) {
                propertyId = propertyId.toLowerCase();
                value = values.get(lowerKeyMap.get(propertyId));
            }

            if (value != null) {
                try {
                    // Convert
                    Object convertedValue = this.converterManager.convert(propertyDescriptor.getPropertyClass(), value);

                    if (propertyDescriptor.getWriteMethod() != null) {
                        propertyDescriptor.getWriteMethod().invoke(bean, convertedValue);
                    } else if (propertyDescriptor.getFied() != null) {
                        propertyDescriptor.getFied().set(bean, convertedValue);
                    }
                } catch (Exception e) {
                    throw new PropertyException("Failed to populate property [" + propertyId + "]", e);
                }

                // "Tick" already populated properties
                values.remove(propertyId);
            } else if (propertyDescriptor.isMandatory()) {
                throw new PropertyMandatoryException(propertyId);
            }
        }
    }

    /**
     * Validate populated values based on JSR 303.
     * 
     * @param bean the bean to validate
     * @throws PropertyException validation error
     */
    private void validateBean(Object bean) throws PropertyException
    {
        if (getValidatorFactory() != null) {
            Validator validator = getValidatorFactory().getValidator();
            Set<ConstraintViolation<Object>> constraintViolations = validator.validate(bean);
            if (!constraintViolations.isEmpty()) {
                throw new PropertyException("Failed to validate bean: ["
                    + constraintViolations.iterator().next().getMessage() + "]");
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.BeanManager#getBeanDescriptor(java.lang.Class)
     */
    public BeanDescriptor getBeanDescriptor(Class< ? > beanClass)
    {
        BeanDescriptor beanDescriptor = null;

        if (beanClass != null) {
            // Since the bean descriptor are cached, lock based on the class to not generate twice the same bean
            // descriptor.
            synchronized (beanClass) {
                beanDescriptor = this.beanDescriptorCache.get(beanClass);
                if (beanDescriptor == null) {
                    beanDescriptor = new DefaultBeanDescriptor(beanClass);
                    this.beanDescriptorCache.put(beanClass, beanDescriptor);
                }
            }
        }

        return beanDescriptor;
    }
}
