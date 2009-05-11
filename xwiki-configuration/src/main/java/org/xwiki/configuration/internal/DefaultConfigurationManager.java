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
package org.xwiki.configuration.internal;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.beanutils.BeanUtils;
import org.xwiki.configuration.internal.commons.CommonsConfigurationAdapter;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;

import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * @version $Id$
 * @since 1.6M1
 */
@Component
public class DefaultConfigurationManager implements ConfigurationManager
{
    public void initializeConfiguration(Object configurationBean, List<ConfigurationSource> sources,
        String namespace) throws InitializationException 
    {
        // Use Jakarta Commons Configuration to merge configuration sources
        // The order is important since if a property is defined in several sources the property
        // value from the first added one will be returned.
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        for (ConfigurationSource source: sources) {
            compositeConfiguration.addConfiguration(new CommonsConfigurationAdapter(source));
        }

        // Look for all properties starting with the namespace prefix.
        Map<String, Object> properties = new HashMap<String, Object>();
        Iterator keys = compositeConfiguration.getKeys(namespace);
        
        while (keys.hasNext()) {
            String key = (String) keys.next();
            
            // List values are handled automatically (the configuration bean must simply have a setXXX(List) signature.
            // However we also want to handle properties in values. For ex:
            // some.property = prop1=value1
            // some.property = prop2=value2
            Object valueObject = compositeConfiguration.getProperty(key);

            // Verify all values are of the type key=value and if so create a Properties object.
            if (List.class.isAssignableFrom(valueObject.getClass())) {
                boolean isProperty = true;
                for (Object value : (List) valueObject) {
                    if (String.class.isAssignableFrom(value.getClass())) {
                        String valueAsString = (String) value;
                        if (valueAsString.indexOf('=') == -1) {
                            isProperty = false;
                            break;
                        }
                    } else {
                        isProperty = false;
                        break;
                    }
                }
                if (isProperty) {
                    valueObject = compositeConfiguration.getProperties(key);
                }
            }
            
            // Remove the namespace prefix for each key so that BeanUtils can call the correct
            // method on the bean.
            properties.put(key.substring(key.indexOf(namespace) + namespace.length() + 1), valueObject);
        }
        
        // For all found properties load the java bean using BeanUtils
        try {
            BeanUtils.populate(configurationBean, properties);
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize bean ["
                + configurationBean.getClass().getName() + "]", e);
        }
    }
}
