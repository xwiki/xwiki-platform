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
package com.xpn.xwiki.internal.filter.output;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiObjectPropertyFilter;
import org.xwiki.internal.objects.ObjectPropertyParser;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.classes.StringClass;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class BasePropertyOutputFilterStream extends AbstractElementOutputFilterStream<BaseProperty>
{
    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    private BaseClass currentXClass;

    /**
     * @param currentXClass the current class
     */
    public void setCurrentXClass(BaseClass currentXClass)
    {
        this.currentXClass = currentXClass;
    }

    /**
     * @return the current class
     * @since 12.10.4
     * @since 13.0
     */
    public BaseClass getCurrentXClass()
    {
        return this.currentXClass;
    }

    // Events

    @Override
    public void onWikiObjectProperty(String name, Object value, FilterEventParameters parameters) throws FilterException
    {
        PropertyClassInterface propertyclass = (this.currentXClass != null)
            ? (PropertyClassInterface) this.currentXClass.safeget(name) : null;
        BaseProperty property;
        try {
            if (propertyclass != null) {
                // Bulletproofing using PropertyClassInterface#fromString when a String is passed (in case it's not
                // really a String property)
                property = value instanceof String valueString ? propertyclass.fromString(valueString)
                    : propertyclass.fromValue(value);
            } else {
                property = computeMissingProperty(value, parameters);
            }
        } catch (XWikiException | ComponentLookupException e) {
            throw new FilterException(String.format("Error when handling object [%s] with value [%s]", name,
                value), e);
        }
        if (property != null) {
            if (this.entity == null) {
                this.entity = property;
            } else {
                this.entity.apply(property, true);
            }
        }
    }

    private BaseProperty<?> computeMissingProperty(Object value, FilterEventParameters parameters)
        throws XWikiException, ComponentLookupException
    {
        BaseProperty<?> result;
        ComponentManager componentManager = componentManagerProvider.get();
        Object objectPropertyType = parameters.get(WikiObjectPropertyFilter.PARAMETER_OBJECTPROPERTY_TYPE);
        if (objectPropertyType instanceof String objectPropertyTypeName
            && componentManager.hasComponent(ObjectPropertyParser.class, objectPropertyTypeName)) {
            ObjectPropertyParser objectPropertyParser =
                componentManager.getInstance(ObjectPropertyParser.class, objectPropertyTypeName);
            result = objectPropertyParser.fromValue(value);
        } else {
            StringClass stringClass = new StringClass();
            result = stringClass.fromString(String.valueOf(value));
        }
        return result;
    }

    @Override
    public void disable()
    {
        super.disable();
        this.currentXClass = null;
    }
}
