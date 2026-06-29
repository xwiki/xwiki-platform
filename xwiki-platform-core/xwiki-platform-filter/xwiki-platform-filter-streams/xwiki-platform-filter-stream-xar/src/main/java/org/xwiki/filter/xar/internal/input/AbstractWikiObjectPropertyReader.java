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
package org.xwiki.filter.xar.internal.input;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiObjectPropertyFilter;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.input.ClassPropertyReader.WikiClassProperty;
import org.xwiki.filter.xar.internal.input.ClassReader.WikiClass;
import org.xwiki.xar.internal.XarObjectPropertySerializerManager;
import org.xwiki.xar.internal.model.XarObjectPropertyModel;

/**
 * @version $Id$
 * @since 9.0RC1
 */
public abstract class AbstractWikiObjectPropertyReader extends AbstractReader
{
    @Inject
    private XarObjectPropertySerializerManager propertySerializerManager;

    /**
     * Class holding information about wiki object property.
     */
    public static class WikiObjectProperty
    {
        /**
         * The name of the object property.
         */
        public String name;

        /**
         * The value of the property.
         */
        public Object value;

        /**
         * The parameter of the property.
         */
        public FilterEventParameters parameters = new FilterEventParameters();

        /**
         * Send events related to the object property to the proxy filter.
         *
         * @param proxyFilter the proxy filter where to send the events.
         * @throws FilterException in case of problem when sending events.
         */
        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            proxyFilter.onWikiObjectProperty(this.name, this.value, this.parameters);
        }
    }

    protected WikiObjectProperty readObjectProperty(XMLStreamReader xmlReader, XARInputProperties properties,
        WikiClass wikiClass) throws XMLStreamException, FilterException
    {
        String typeAttribute = xmlReader.getAttributeValue(null, XarObjectPropertyModel.ATTRIBUTE_TYPE);
        xmlReader.nextTag();

        WikiObjectProperty property = new WikiObjectProperty();

        property.name = xmlReader.getLocalName();

        String type;
        if (wikiClass != null) {
            WikiClassProperty classProperty = wikiClass.properties.get(property.name);
            type = classProperty != null ? classProperty.type : null;
        } else {
            type = properties.getObjectPropertyType();
        }
        boolean useTypeAttribute = false;
        if (type == null && !StringUtils.isEmpty(typeAttribute)) {
            type = typeAttribute;
            useTypeAttribute = true;
        }

        try {
            property.value = this.propertySerializerManager.getPropertySerializer(type).read(xmlReader);
        } catch (ComponentLookupException e) {
            throw new FilterException("Failed to get a property parser", e);
        }

        // only use and serialize the object property type when needed (i.e. when the type is missing).
        if (useTypeAttribute) {
            property.parameters.put(WikiObjectPropertyFilter.PARAMETER_OBJECTPROPERTY_TYPE, typeAttribute);
        }
        property.parameters.put(WikiObjectPropertyFilter.PARAMETER_TYPE, type);

        xmlReader.nextTag();

        return property;
    }
}
