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
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;

/**
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class BasePropertyOutputFilterStream extends AbstractEntityOutputFilterStream<BaseProperty>
{
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
        if (this.currentXClass != null) {
            PropertyClassInterface propertyclass = (PropertyClassInterface) this.currentXClass.safeget(name);

            if (propertyclass != null) {
                // Bulletproofing using PropertyClassInterface#fromString when a String is passed (in case it's not
                // really a String property)
                BaseProperty property =
                    value instanceof String ? propertyclass.fromString((String) value) : propertyclass.fromValue(value);

                if (this.entity == null) {
                    this.entity = property;
                } else {
                    this.entity.apply(property, true);
                }
            } else {
                // TODO: Log something ?
            }
        } else {
            // TODO: Log something ?
        }
    }
}
