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
package org.xwiki.rest.internal;

import java.awt.Image;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Date;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.xstream.internal.SafeXStream;

import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.DomWriter;

/**
 * Adapters between JAXB and the "real world".
 * 
 * @version $Id$
 * @since 9.1RC1
 */
@Component(roles = JAXBConverter.class)
@Singleton
public class JAXBConverter implements Initializable
{
    /**
     * The list of classes supported by JAXB converters.
     */
    // TODO: find a more dynamic way to get that list
    // TODO: find a way to extend that list
    private static final Set<Class<?>> JAXB_COMPATIBLES_CLASSES = new HashSet<>(Arrays.asList(String.class,
        BigInteger.class, int.class, long.class, short.class, BigDecimal.class, float.class, double.class,
        boolean.class, byte.class, QName.class, XMLGregorianCalendar.class, byte[].class, byte[].class, Duration.class,
        Calendar.class, Date.class, URI.class, Image.class, DataHandler.class, Source.class, UUID.class));

    @Inject
    private SafeXStream xstream;

    private DocumentBuilderFactory dbFactory;

    @Override
    public void initialize() throws InitializationException
    {
        this.dbFactory = DocumentBuilderFactory.newInstance();
    }

    /**
     * @param clazz the class to test
     * @return true if the passed class is supported by JAXB
     */
    public boolean isSupported(Class<?> clazz)
    {
        return JAXB_COMPATIBLES_CLASSES.contains(clazz);
    }

    /**
     * Convert the passed POJO into something standard JAXB can serialize in a anyType element.
     * <p>
     * {@link #unserializeAny(Object)} should be used to parse the value produced by this method.
     * 
     * @param value the object to serialize
     * @return a JAXB compatible version of the passed value
     * @throws ParserConfigurationException when failing to create a document builder
     */
    public Object serializeAny(Object value) throws ParserConfigurationException
    {
        if (value == null || isSupported(value.getClass())) {
            return value;
        }

        // Convert to Element
        DocumentBuilder dBuilder = this.dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();
        // Root element is "consumed" by the JAXB marshaling
        Element rootElement = doc.createElement("rest");
        DomWriter writer = new DomWriter(rootElement);
        this.xstream.marshal(value, writer);

        return rootElement;
    }

    /**
     * Convert the value of an anyType JAXB element to expected POJO.
     * <p>
     * The passed value is expected to have been produced by {@link #serializeAny(Object)} (or something reproducing the
     * same behavior).
     * 
     * @param any the value coming from JAXB unmarshaling
     * @return the object in the "real" expected type
     */
    public Object unserializeAny(Object any)
    {
        Object value = any;

        // Parse elements
        if (any instanceof Element) {
            Element valueElement = (Element) any;
            if (valueElement.hasChildNodes()) {
                NodeList children = valueElement.getChildNodes();
                for (int i = 0; i < children.getLength(); ++i) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        DomReader reader = new DomReader((Element) child);
                        return this.xstream.unmarshal(reader);
                    }
                }
            } else {
                value = null;
            }
        }

        return value;
    }
}
