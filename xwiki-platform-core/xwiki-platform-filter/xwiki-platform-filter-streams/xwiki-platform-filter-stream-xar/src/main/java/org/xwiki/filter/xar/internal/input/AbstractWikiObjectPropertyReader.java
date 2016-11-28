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
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.input.ClassPropertyReader.WikiClassProperty;
import org.xwiki.filter.xar.internal.input.ClassReader.WikiClass;
import org.xwiki.xar.internal.XarObjectPropertySerializerManager;

/**
 * @version $Id$
 * @since 9.0RC1
 */
public class AbstractWikiObjectPropertyReader extends AbstractReader
{
    @Inject
    private XarObjectPropertySerializerManager propertySerializerManager;

    public class WikiObjectProperty
    {
        public String name;

        public Object value;

        public FilterEventParameters parameters = new FilterEventParameters();

        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            proxyFilter.onWikiObjectProperty(this.name, this.value, this.parameters);
        }
    }

    public WikiObjectProperty readObjectProperty(XMLStreamReader xmlReader, XARInputProperties properties, WikiClass wikiClass)
        throws XMLStreamException, FilterException
    {
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

        try {
            property.value = this.propertySerializerManager.getPropertySerializer(type).read(xmlReader);
        } catch (ComponentLookupException e) {
            throw new FilterException("Failed to get a property parser", e);
        }

        xmlReader.nextTag();

        return property;
    }
}
