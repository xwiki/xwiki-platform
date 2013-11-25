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
import org.xwiki.wikistream.xar.internal.XARObjectModel;
import org.xwiki.wikistream.xar.internal.XARObjectPropertyModel;
import org.xwiki.wikistream.xar.internal.XARUtils.EventParameter;
import org.xwiki.wikistream.xar.internal.input.ClassReader.WikiClass;

/**
 * @version $Id$
 * @since 5.2RC1
 */
public class WikiObjectReader extends AbstractReader
{
    public class WikiObject
    {
        public WikiClass wikiClass;

        public String name;

        public FilterEventParameters parameters = new FilterEventParameters();

        private List<WikiObjectProperty> properties = new ArrayList<WikiObjectProperty>();

        public void send(XARFilter proxyFilter) throws WikiStreamException
        {
            proxyFilter.beginWikiObject(this.name, this.parameters);

            this.wikiClass.send(proxyFilter);

            for (WikiObjectProperty property : this.properties) {
                property.send(proxyFilter);
            }

            proxyFilter.endWikiObject(this.name, this.parameters);
        }
    }

    public class WikiObjectProperty
    {
        public String name;

        public String value;

        public FilterEventParameters parameters = new FilterEventParameters();

        public void send(XARFilter proxyFilter) throws WikiStreamException
        {
            proxyFilter.onWikiObjectProperty(this.name, this.value, this.parameters);
        }
    }

    public WikiObject readObject(XMLStreamReader xmlReader, XARInputProperties properties) throws XMLStreamException,
        WikiStreamException, IOException, ParseException
    {
        WikiObject wikiObject = new WikiObject();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();
            if (elementName.equals(XARClassModel.ELEMENT_CLASS)) {
                ClassReader reader = new ClassReader();

                wikiObject.wikiClass = reader.read(xmlReader, properties);
            } else if (elementName.equals(XARObjectPropertyModel.ELEMENT_PROPERTY)) {
                wikiObject.properties.add(readObjectProperty(xmlReader, properties));
            } else {
                String value = xmlReader.getElementText();

                EventParameter parameter = XARObjectModel.OBJECT_PARAMETERS.get(elementName);

                if (parameter != null) {
                    wikiObject.parameters.put(parameter.name, convert(parameter.type, value));
                }
            }
        }

        return wikiObject;
    }

    private WikiObjectProperty readObjectProperty(XMLStreamReader xmlReader, XARInputProperties properties)
        throws XMLStreamException, WikiStreamException
    {
        xmlReader.nextTag();

        WikiObjectProperty property = new WikiObjectProperty();

        property.name = xmlReader.getLocalName();
        property.value = xmlReader.getElementText();

        xmlReader.nextTag();

        return property;
    }
}
