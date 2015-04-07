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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.xar.internal.XARClassModel;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.filter.xar.internal.input.ClassPropertyReader.WikiClassProperty;
import org.xwiki.filter.FilterException;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class ClassReader extends AbstractReader implements XARXMLReader<ClassReader.WikiClass>
{
    @Inject
    private XARXMLReader<ClassPropertyReader.WikiClassProperty> propertyReader;

    public static class WikiClass
    {
        public String name;

        public FilterEventParameters parameters = new FilterEventParameters();

        public Map<String, WikiClassProperty> properties = new LinkedHashMap<String, WikiClassProperty>();

        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            proxyFilter.beginWikiClass(this.parameters);

            for (WikiClassProperty property : this.properties.values()) {
                property.send(proxyFilter);
            }

            proxyFilter.endWikiClass(this.parameters);
        }

        public boolean isEmpty()
        {
            return this.properties.isEmpty();
        }

        public void addProperty(WikiClassProperty property)
        {
            this.properties.put(property.name, property);
        }
    }

    @Override
    public WikiClass read(XMLStreamReader xmlReader) throws XMLStreamException, FilterException
    {
        WikiClass wikiClass = new WikiClass();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (wikiClass.name == null && XARClassModel.ELEMENT_NAME.equals(elementName)) {
                wikiClass.name = xmlReader.getElementText();
            } else if (XARClassModel.CLASS_PARAMETERS.containsKey(elementName)) {
                String value = xmlReader.getElementText();

                EventParameter parameter = XARClassModel.CLASS_PARAMETERS.get(elementName);

                if (parameter != null) {
                    Object wsValue = convert(parameter.type, value);
                    if (wsValue != null) {
                        wikiClass.parameters.put(parameter.name, wsValue);
                    }
                }
            } else {
                wikiClass.addProperty(this.propertyReader.read(xmlReader));
            }
        }

        return wikiClass;
    }
}
