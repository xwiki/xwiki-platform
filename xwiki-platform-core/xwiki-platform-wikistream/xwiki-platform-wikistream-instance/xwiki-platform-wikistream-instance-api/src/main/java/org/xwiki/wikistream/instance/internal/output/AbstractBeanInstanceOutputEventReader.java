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
package org.xwiki.wikistream.instance.internal.output;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.properties.BeanManager;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.input.AbstractBeanEntityEventGenerator;
import org.xwiki.wikistream.instance.output.InstanceOutputEventReader;

public class AbstractBeanInstanceOutputEventReader<P> implements InstanceOutputEventReader, Initializable
{
    @Inject
    private BeanManager beanManager;

    private Class<P> propertiesType;

    protected P properties;

    @Override
    public void initialize() throws InitializationException
    {
        // Get the type of the properties
        ParameterizedType genericType =
            (ParameterizedType) ReflectionUtils.resolveType(AbstractBeanEntityEventGenerator.class, getClass());
        this.propertiesType = ReflectionUtils.getTypeClass(genericType.getActualTypeArguments()[2]);
    }

    @Override
    public void setProperties(Map<String, Object> properties) throws WikiStreamException
    {
        try {
            this.properties = this.propertiesType.newInstance();

            this.beanManager.populate(this.properties, properties);
        } catch (Exception e) {
            throw new WikiStreamException("Failed to convert properties to Java bean", e);
        }
    }
}
