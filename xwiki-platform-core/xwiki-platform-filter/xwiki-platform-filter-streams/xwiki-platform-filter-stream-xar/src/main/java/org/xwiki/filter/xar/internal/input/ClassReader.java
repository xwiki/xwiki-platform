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
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiClassFilter;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.filter.xar.internal.input.ClassPropertyReader.WikiClassProperty;
import org.xwiki.xar.internal.model.XarClassModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class ClassReader extends AbstractReader implements XARXMLReader<ClassReader.WikiClass>
{
    /**
     * Parameters to be used when reading a class.
     */
    private static final Map<String, EventParameter> CLASS_PARAMETERS =
        Map.of(XarClassModel.ELEMENT_CUSTOMCLASS, new EventParameter(WikiClassFilter.PARAMETER_CUSTOMCLASS),
            XarClassModel.ELEMENT_CUSTOMMAPPING, new EventParameter(WikiClassFilter.PARAMETER_CUSTOMMAPPING),
            XarClassModel.ELEMENT_SHEET_DEFAULTVIEW, new EventParameter(WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW),
            XarClassModel.ELEMENT_SHEET_DEFAULTEDIT, new EventParameter(WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT),
            XarClassModel.ELEMENT_DEFAULTSPACE, new EventParameter(WikiClassFilter.PARAMETER_DEFAULTSPACE),
            XarClassModel.ELEMENT_NAMEFIELD, new EventParameter(WikiClassFilter.PARAMETER_NAMEFIELD),
            XarClassModel.ELEMENT_VALIDATIONSCRIPT, new EventParameter(WikiClassFilter.PARAMETER_VALIDATIONSCRIPT));

    @Inject
    private XARXMLReader<ClassPropertyReader.WikiClassProperty> propertyReader;

    /**
     * Dedicated class to hold information about a read XWiki class.
     */
    public static class WikiClass
    {
        /**
         * The name of the xclass.
         */
        public String name;

        /**
         * The parameters of the xclass.
         */
        public FilterEventParameters parameters = new FilterEventParameters();

        /**
         * The properties of the xclass.
         */
        public Map<String, WikiClassProperty> properties = new LinkedHashMap<>();

        /**
         * Send events related to the xclass to the proxy filter.
         *
         * @param proxyFilter the proxy filter where to send the events.
         * @throws FilterException in case of problem when sending events.
         */
        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            proxyFilter.beginWikiClass(this.parameters);

            for (WikiClassProperty property : this.properties.values()) {
                property.send(proxyFilter);
            }

            proxyFilter.endWikiClass(this.parameters);
        }

        /**
         * @return {@code true} if the properties are empty.
         */
        public boolean isEmpty()
        {
            return this.properties.isEmpty();
        }

        /**
         * Put a new property indexed by its name.
         * 
         * @param property the property to be added.
         */
        public void addProperty(WikiClassProperty property)
        {
            this.properties.put(property.name, property);
        }
    }

    @Override
    public WikiClass read(XMLStreamReader xmlReader, XARInputProperties properties)
        throws XMLStreamException, FilterException
    {
        WikiClass wikiClass = new WikiClass();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (wikiClass.name == null && XarClassModel.ELEMENT_NAME.equals(elementName)) {
                wikiClass.name = xmlReader.getElementText();
                wikiClass.parameters.put(WikiClassFilter.PARAMETER_NAME, wikiClass.name);
            } else if (CLASS_PARAMETERS.containsKey(elementName)) {
                String value = xmlReader.getElementText();

                EventParameter parameter = CLASS_PARAMETERS.get(elementName);

                if (parameter != null) {
                    Object wsValue = convert(parameter.type, value);
                    if (wsValue != null) {
                        wikiClass.parameters.put(parameter.name, wsValue);
                    }
                }
            } else {
                WikiClassProperty property = this.propertyReader.read(xmlReader, properties);
                if (property != null) {
                    wikiClass.addProperty(property);
                }
            }
        }

        return wikiClass;
    }
}
