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
package org.xwiki.wikistream.instance.internal.input;

import java.util.Iterator;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.PropertyClassFilter;
import org.xwiki.wikistream.instance.internal.PropertyClassProperties;

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class PropertyClassEventGenerator extends
    AbstractBeanEntityEventGenerator<PropertyClass, PropertyClassFilter, PropertyClassProperties>
{
    @Override
    public void write(PropertyClass xclassProperty, Object filter, PropertyClassFilter propertyFilter,
        PropertyClassProperties properties) throws WikiStreamException
    {
        // > WikiClassProperty

        FilterEventParameters propertyParameters = FilterEventParameters.EMPTY;

        String classType = xclassProperty.getClassType();
        if (xclassProperty.getClass().getSimpleName().equals(classType + "Class")) {
            // Keep exporting the full Java class name for old/default property types to avoid breaking the XAR format
            // (to allow XClasses created with the current version of XWiki to be imported in an older version).
            classType = getClass().getName();
        }

        propertyFilter.beginWikiClassProperty(xclassProperty.getName(), classType, propertyParameters);

        // * WikiClassPropertyField

        // Iterate over values sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator<BaseProperty< ? >> it = xclassProperty.getSortedIterator();
        while (it.hasNext()) {
            BaseProperty< ? > bprop = it.next();
            propertyFilter.onWikiClassPropertyField(bprop.getName(), bprop.toText(), FilterEventParameters.EMPTY);
        }

        // < WikiClassProperty

        propertyFilter.endWikiClassProperty(xclassProperty.getName(), classType, propertyParameters);
    }
}
