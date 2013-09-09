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
import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.WikiClassPropertyFilter;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.instance.internal.PropertyClassProperties;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class PropertyClassInputWikiStream implements InputWikiStream
{
    private PropertyClass xclassProperty;

    private PropertyClassProperties properties;

    public PropertyClassInputWikiStream(PropertyClass xclassProperty, PropertyClassProperties properties)
    {
        this.xclassProperty = xclassProperty;
        this.properties = properties;
    }

    @Override
    public void read(Object filter) throws WikiStreamException
    {
        XWikiDocumentFilter documentFilter = (XWikiDocumentFilter) filter;

        // > WikiClassProperty

        FilterEventParameters propertyParameters = new FilterEventParameters();

        Map<String, String> fields = new LinkedHashMap<String, String>();

        // Iterate over values sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator<BaseProperty< ? >> it = this.xclassProperty.getSortedIterator();
        while (it.hasNext()) {
            BaseProperty< ? > bprop = it.next();
            fields.put(bprop.getName(), bprop.toText());
        }

        propertyParameters.put(WikiClassPropertyFilter.PARAMETER_FIELDS, fields);

        String classType = this.xclassProperty.getClassType();
        if (this.xclassProperty.getClass().getSimpleName().equals(classType + "Class")) {
            // Keep exporting the full Java class name for old/default property types to avoid breaking the XAR format
            // (to allow XClasses created with the current version of XWiki to be imported in an older version).
            classType = getClass().getName();
        }

        documentFilter.beginWikiClassProperty(this.xclassProperty.getName(), classType, propertyParameters);

        // < WikiClassProperty

        documentFilter.endWikiClassProperty(this.xclassProperty.getName(), classType, propertyParameters);
    }
}
