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
package org.xwiki.rest.internal.resources.classes;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;

import com.xpn.xwiki.objects.classes.StaticListClass;

/**
 * Provides values for Static List properties.
 *
 * @version $Id$
 * @since 11.5RC1
 */
@Component
@Named("StaticList")
@Singleton
public class StaticListClassPropertyValuesProvider extends AbstractListClassPropertyValuesProvider<StaticListClass>
{
    @Override
    protected Class<StaticListClass> getPropertyType()
    {
        return StaticListClass.class;
    }

    @Override
    protected PropertyValues getAllowedValues(StaticListClass propertyDefinition, int limit, String filter)
        throws Exception
    {
        final PropertyValues result = new PropertyValues();
        List<String> allValues = StaticListClass.getListFromString(propertyDefinition.getValues());

        allValues.stream().filter(s -> s.toLowerCase().contains(filter.toLowerCase())).limit(limit)
            .forEach(item -> result.withPropertyValues(new PropertyValue(item)));
        return result;
    }
}
