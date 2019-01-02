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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.XARClassPropertyModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class ClassPropertyReader extends AbstractReader implements XARXMLReader<ClassPropertyReader.WikiClassProperty>
{
    public static class WikiClassProperty
    {
        public String name;

        public String type;

        public FilterEventParameters parameters = new FilterEventParameters();

        public Map<String, String> fields = new LinkedHashMap<>();

        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            proxyFilter.beginWikiClassProperty(this.name, this.type, this.parameters);

            for (Map.Entry<String, String> entry : this.fields.entrySet()) {
                proxyFilter.onWikiClassPropertyField(entry.getKey(), entry.getValue(), FilterEventParameters.EMPTY);
            }

            proxyFilter.endWikiClassProperty(this.name, this.type, this.parameters);

        }
    }

    @Override
    public WikiClassProperty read(XMLStreamReader xmlReader, XARInputProperties properties)
        throws XMLStreamException, FilterException
    {
        WikiClassProperty wikiClassProperty = new WikiClassProperty();

        wikiClassProperty.name = xmlReader.getLocalName();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();
            String value = xmlReader.getElementText();

            if (elementName.equals(XARClassPropertyModel.ELEMENT_CLASSTYPE)) {
                wikiClassProperty.type = value;
            } else {
                wikiClassProperty.fields.put(elementName, value);

                // If a <name> is defined it has priority over parent element local name
                if (elementName.equals(XARClassPropertyModel.ELEMENT_NAME)) {
                    wikiClassProperty.name = value;
                }
            }
        }

        if (wikiClassProperty.type == null) {
            throw new FilterException(
                String.format("No <classType> element found for property [%s]", wikiClassProperty.name));
        }

        return wikiClassProperty;
    }
}
