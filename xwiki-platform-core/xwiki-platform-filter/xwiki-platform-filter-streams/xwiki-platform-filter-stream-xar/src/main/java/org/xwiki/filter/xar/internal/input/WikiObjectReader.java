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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiObjectFilter;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.filter.xar.internal.input.ClassReader.WikiClass;
import org.xwiki.xar.internal.model.XarClassModel;
import org.xwiki.xar.internal.model.XarObjectModel;
import org.xwiki.xar.internal.model.XarObjectPropertyModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class WikiObjectReader extends AbstractWikiObjectPropertyReader
    implements XARXMLReader<WikiObjectReader.WikiObject>
{
    /**
     * The list of parameters to be used when reading an object.
     */
    private static final Map<String, EventParameter> OBJECT_PARAMETERS = Map.of(
        XarObjectModel.ELEMENT_NAME, new EventParameter(WikiObjectFilter.PARAMETER_NAME),
        XarObjectModel.ELEMENT_CLASSNAME, new EventParameter(WikiObjectFilter.PARAMETER_CLASS_REFERENCE),
        XarObjectModel.ELEMENT_GUID, new EventParameter(WikiObjectFilter.PARAMETER_GUID),
        XarObjectModel.ELEMENT_NUMBER, new EventParameter(WikiObjectFilter.PARAMETER_NUMBER, Integer.class)
    );

    @Inject
    private XARXMLReader<ClassReader.WikiClass> classReader;

    /**
     * Class holding information about xwiki objects.
     */
    public static class WikiObject
    {
        /**
         * The xclass the object is type of.
         */
        public WikiClass wikiClass;

        /**
         * The parameters of the xobject.
         */
        public FilterEventParameters parameters = new FilterEventParameters();

        /**
         * The properties of the xobjects.
         */
        private List<WikiObjectProperty> properties = new ArrayList<>();

        /**
         * Send events related to the xobject to the proxy filter.
         *
         * @param proxyFilter the proxy filter where to send the events.
         * @throws FilterException in case of problem when sending events.
         */
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

    @Override
    public WikiObject read(XMLStreamReader xmlReader, XARInputProperties properties)
        throws XMLStreamException, FilterException
    {
        WikiObject wikiObject = new WikiObject();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();
            if (elementName.equals(XarClassModel.ELEMENT_CLASS)) {
                wikiObject.wikiClass = this.classReader.read(xmlReader, properties);
            } else if (elementName.equals(XarObjectPropertyModel.ELEMENT_PROPERTY)) {
                String classReference =
                    (String) wikiObject.parameters.get(WikiObjectFilter.PARAMETER_CLASS_REFERENCE);
                wikiObject.properties.add(readObjectProperty(xmlReader, properties, wikiObject.wikiClass,
                    classReference));
            } else {
                EventParameter parameter = OBJECT_PARAMETERS.get(elementName);

                if (parameter != null) {
                    Object wsValue = convert(parameter.type, xmlReader.getElementText());
                    if (wsValue != null) {
                        wikiObject.parameters.put(parameter.name, wsValue);
                    }
                } else {
                    unknownElement(xmlReader);
                }
            }
        }

        return wikiObject;
    }
}
