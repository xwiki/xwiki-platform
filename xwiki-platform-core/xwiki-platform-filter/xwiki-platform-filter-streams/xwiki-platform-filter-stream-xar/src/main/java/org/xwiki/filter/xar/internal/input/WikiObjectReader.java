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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiObjectFilter;
import org.xwiki.filter.xar.internal.XARClassModel;
import org.xwiki.filter.xar.internal.XARObjectModel;
import org.xwiki.filter.xar.internal.XARObjectPropertyModel;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.filter.xar.internal.input.ClassPropertyReader.WikiClassProperty;
import org.xwiki.filter.xar.internal.input.ClassReader.WikiClass;
import org.xwiki.xar.internal.XarObjectPropertySerializerManager;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class WikiObjectReader extends AbstractReader implements XARXMLReader<WikiObjectReader.WikiObject>
{
    @Inject
    private XarObjectPropertySerializerManager propertySerializerManager;

    @Inject
    private XARXMLReader<ClassReader.WikiClass> classReader;

    public static class WikiObject
    {
        public WikiClass wikiClass;

        public FilterEventParameters parameters = new FilterEventParameters();

        private List<WikiObjectProperty> properties = new ArrayList<WikiObjectProperty>();

        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            String name = null;

            if (this.parameters.containsKey(WikiObjectFilter.PARAMETER_CLASS_REFERENCE)) {
                StringBuilder nameBuilder =
                    new StringBuilder(this.parameters.get(WikiObjectFilter.PARAMETER_CLASS_REFERENCE).toString());

                if (this.parameters.containsKey(WikiObjectFilter.PARAMETER_NUMBER)) {
                    nameBuilder.append('[');
                    nameBuilder.append(this.parameters.get(WikiObjectFilter.PARAMETER_NUMBER));
                    nameBuilder.append(']');
                }

                name = nameBuilder.toString();
            }

            proxyFilter.beginWikiObject(name, this.parameters);

            if (this.wikiClass != null) {
                this.wikiClass.send(proxyFilter);
            }

            for (WikiObjectProperty property : this.properties) {
                property.send(proxyFilter);
            }

            proxyFilter.endWikiObject(name, this.parameters);
        }
    }

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

    @Override
    public WikiObject read(XMLStreamReader xmlReader) throws XMLStreamException, FilterException
    {
        WikiObject wikiObject = new WikiObject();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();
            if (elementName.equals(XARClassModel.ELEMENT_CLASS)) {
                wikiObject.wikiClass = this.classReader.read(xmlReader);
            } else if (elementName.equals(XARObjectPropertyModel.ELEMENT_PROPERTY)) {
                wikiObject.properties.add(readObjectProperty(xmlReader, wikiObject.wikiClass));
            } else {
                String value = xmlReader.getElementText();

                EventParameter parameter = XARObjectModel.OBJECT_PARAMETERS.get(elementName);

                if (parameter != null) {
                    Object wsValue = convert(parameter.type, value);
                    if (wsValue != null) {
                        wikiObject.parameters.put(parameter.name, wsValue);
                    }
                }
            }
        }

        return wikiObject;
    }

    private WikiObjectProperty readObjectProperty(XMLStreamReader xmlReader, WikiClass wikiClass)
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
            type = null;
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
