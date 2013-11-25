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
package org.xwiki.wikistream.xar.internal.input;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.xar.input.XARInputProperties;
import org.xwiki.wikistream.xar.internal.XARClassModel;
import org.xwiki.wikistream.xar.internal.XARFilter;
import org.xwiki.wikistream.xar.internal.XARUtils.EventParameter;
import org.xwiki.wikistream.xar.internal.input.ClassPropertyReader.WikiClassProperty;

/**
 * @version $Id$
 * @since 5.2RC1
 */
public class ClassReader extends AbstractReader
{
    private ClassPropertyReader propertyReader = new ClassPropertyReader();

    public static class WikiClass
    {
        public String name;

        public FilterEventParameters parameters = new FilterEventParameters();

        private List<WikiClassProperty> properties = new ArrayList<WikiClassProperty>();

        public void send(XARFilter proxyFilter) throws WikiStreamException
        {
            proxyFilter.beginWikiClass(this.parameters);

            for (WikiClassProperty property : this.properties) {
                property.send(proxyFilter);
            }

            proxyFilter.endWikiClass(this.parameters);
        }
    }

    public WikiClass read(XMLStreamReader xmlReader, XARInputProperties properties) throws XMLStreamException,
        IOException, WikiStreamException, ParseException
    {
        WikiClass wikiClass = new WikiClass();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (XARClassModel.ELEMENT_NAME.equals(elementName)) {
                wikiClass.name = xmlReader.getElementText();
            } else if (XARClassModel.CLASS_PARAMETERS.containsKey(elementName)) {
                String value = xmlReader.getElementText();

                EventParameter parameter = XARClassModel.CLASS_PARAMETERS.get(elementName);

                if (parameter != null) {
                    wikiClass.parameters.put(parameter.name, convert(parameter.type, value));
                }
            } else {
                wikiClass.properties.add(this.propertyReader.read(xmlReader, properties));
            }
        }

        return wikiClass;
    }
}
