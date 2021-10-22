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
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;

import com.xpn.xwiki.objects.classes.ListItem;
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
    {
        final PropertyValues result = new PropertyValues();
        List<String> allValues = StaticListClass.getListFromString(propertyDefinition.getValues());
        Map<String, ListItem> valueMap = StaticListClass.getMapFromString(propertyDefinition.getValues());

        String lowerFilter = filter.toLowerCase();

        allValues.stream().map(id -> constructPropertyValueForId(id, propertyDefinition, valueMap))
            // Filter both by key and by (translated) label (that's why we construct a PropertyValue first)
            .filter(val -> val.getValue().toString().toLowerCase().contains(lowerFilter)
                || (val.getMetaData().containsKey(META_DATA_LABEL)
                && val.getMetaData().get(META_DATA_LABEL).toString().toLowerCase().contains(lowerFilter)))
            .limit(limit).forEach((result::withPropertyValues));

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @since 13.10RC1
     */
    @Override
    protected PropertyValue getValueFromQueryResult(Object result, StaticListClass propertyDefinition)
    {
        PropertyValue value = super.getValueFromQueryResult(result, propertyDefinition);

        if (value != null && value.getValue() instanceof String) {
            String stringKey = (String) value.getValue();

            Map<String, ListItem> valueMap = StaticListClass.getMapFromString(propertyDefinition.getValues());
            return constructPropertyValueForId(stringKey, propertyDefinition, valueMap);
        }

        return value;
    }

    /**
     * Constructs a {@code PropertyValue} for the given id of the given {@code StaticListClass}.
     *
     * @param id The id/value of the item
     * @param propertyDefinition The definition of the property
     * @param map The map from value to list item with value and label
     * @return The result, may contain a label if it is different from the id
     * @since 13.10RC1
     */
    private PropertyValue constructPropertyValueForId(String id, StaticListClass propertyDefinition,
        Map<String, ListItem> map)
    {
        String displayValue = propertyDefinition.getDisplayValue(id, propertyDefinition.getName(), map,
            this.xcontextProvider.get());

        PropertyValue val = new PropertyValue(id);

        if (!id.equals(displayValue)) {
            val.getMetaData().put(META_DATA_LABEL, displayValue);
        }

        return val;
    }
}
