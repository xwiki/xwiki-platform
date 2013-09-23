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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.xar.internal.XARAttachmentModel;
import org.xwiki.wikistream.xar.internal.XARClassModel;
import org.xwiki.wikistream.xar.internal.XARClassPropertyModel;
import org.xwiki.wikistream.xar.internal.XARDocumentModel;
import org.xwiki.wikistream.xar.internal.XARFilter;
import org.xwiki.wikistream.xar.internal.XARObjectModel;
import org.xwiki.wikistream.xar.internal.XARObjectPropertyModel;
import org.xwiki.wikistream.xml.internal.input.XMLInputWikiStreamUtils;

/**
 * @version $Id$
 * @since 5.2RC1
 */
public class DocumentLocaleReader
{
    private String currentSpace;

    private boolean sentBeginDocument;

    private boolean sentBeginDocumentLocale;

    private boolean sentBeginDocumentRevision;

    private class WikiClass
    {
        public FilterEventParameters parameters = new FilterEventParameters();
    }

    private class WikiObject
    {
        public String name;

        public FilterEventParameters parameters = new FilterEventParameters();
    }

    private WikiClass currentClass = new WikiClass();

    private List<WikiObject> currentObjects;

    public void read(Object filter, XARFilter proxyFilter, XARInputProperties properties) throws XMLStreamException,
        IOException, WikiStreamException
    {
        XMLStreamReader xmlReader = XMLInputWikiStreamUtils.createXMLStreamReader(properties);

        // <xwikidoc>

        xmlReader.nextTag();

        xmlReader.require(XMLStreamReader.START_ELEMENT, null, XARDocumentModel.ELEMENT_DOCUMENT);

        readDocument(xmlReader, filter, proxyFilter, properties);
    }

    private void readDocument(XMLStreamReader xmlReader, Object filter, XARFilter proxyFilter,
        XARInputProperties properties) throws XMLStreamException, WikiStreamException
    {

        FilterEventParameters documentParameters = new FilterEventParameters();
        FilterEventParameters documentLocaleParameters = new FilterEventParameters();
        FilterEventParameters documentRevisionParameters = new FilterEventParameters();

        String documentName = null;
        Locale documentLocale = Locale.ROOT;
        String documentVersion = null;

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (elementName.equals(XARAttachmentModel.ELEMENT_ATTACHMENT)) {
                readAttachment(xmlReader, filter, proxyFilter, properties);
            } else if (elementName.equals(XARObjectModel.ELEMENT_OBJECT)) {
                readObject(xmlReader, filter, proxyFilter, properties);
            } else if (elementName.equals(XARClassModel.ELEMENT_CLASS)) {
                readClass(xmlReader, filter, proxyFilter, properties);
            } else {
                String value = xmlReader.getElementText();

                if (XARDocumentModel.ELEMENT_SPACE.equals(elementName)) {
                    if (!value.equals(this.currentSpace)) {
                        if (this.currentSpace != null) {
                            proxyFilter.endWikiSpace(this.currentSpace, FilterEventParameters.EMPTY);
                        }
                        this.currentSpace = value;
                        proxyFilter.beginWikiSpace(this.currentSpace, FilterEventParameters.EMPTY);
                    }
                } else if (XARDocumentModel.ELEMENT_NAME.equals(elementName)) {
                    documentName = value;
                    if (documentParameters.size() == XARDocumentModel.DOCUMENTPARAMETERS.size()) {
                        proxyFilter.beginWikiDocument(documentName, documentParameters);
                    }
                } else {
                    String parameterName = XARDocumentModel.DOCUMENTPARAMETERS.get(elementName);

                    if (parameterName != null) {
                        // TODO: convert into proper values
                        documentParameters.put(parameterName, value);
                    } else {
                        parameterName = XARDocumentModel.DOCUMENTPARAMETERS.get(elementName);

                        if (parameterName != null) {
                            // TODO: convert into proper values
                            documentParameters.put(parameterName, value);
                        } else {
                            parameterName = XARDocumentModel.DOCUMENTPARAMETERS.get(elementName);

                            if (parameterName != null) {
                                // TODO: convert into proper values
                                documentParameters.put(parameterName, value);
                            } else {
                                // Unknown property
                                // TODO: log something ?
                            }
                        }
                    }
                }
            }
        }

        proxyFilter.endWikiDocumentRevision(documentVersion, documentRevisionParameters);
        proxyFilter.endWikiDocumentLocale(documentLocale, documentLocaleParameters);
        proxyFilter.endWikiDocument(documentName, documentParameters);
    }

    private void readObject(XMLStreamReader xmlReader, Object filter, XARFilter proxyFilter,
        XARInputProperties properties) throws XMLStreamException, WikiStreamException
    {
        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();
            if (elementName.equals(XARClassModel.ELEMENT_CLASS)) {
                readClass(xmlReader, filter, proxyFilter, properties);
            } else if (elementName.equals(XARObjectPropertyModel.ELEMENT_PROPERTY)) {
                readObjectProperty(xmlReader, filter, proxyFilter, properties);
            } else {
                String value = xmlReader.getElementText();

                String parameterName = XARDocumentModel.DOCUMENTPARAMETERS.get(elementName);

                if (parameterName != null) {

                } else {
                    if (XARDocumentModel.ELEMENT_SPACE.equals(elementName)) {

                    }
                }
            }
        }
    }

    private void readObjectProperty(XMLStreamReader xmlReader, Object filter, XARFilter proxyFilter,
        XARInputProperties properties) throws XMLStreamException, WikiStreamException
    {
        xmlReader.nextTag();

        String propertyName = xmlReader.getLocalName();
        String value = xmlReader.getElementText();

        proxyFilter.onWikiObjectProperty(propertyName, value, FilterEventParameters.EMPTY);
    }

    private void readClass(XMLStreamReader xmlReader, Object filter, XARFilter proxyFilter,
        XARInputProperties properties) throws XMLStreamException, WikiStreamException
    {
        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (XARClassModel.XARTOEVENTPARAMETERS.containsKey(elementName)) {
                String value = xmlReader.getElementText();

                String parameterName = XARDocumentModel.DOCUMENTPARAMETERS.get(elementName);

                if (parameterName != null) {

                } else {
                    if (XARClassModel.ELEMENT_NAME.equals(elementName)) {

                    }
                }
            } else {
                readClassProperty(xmlReader, filter, proxyFilter, properties);
            }
        }
    }

    private void readClassProperty(XMLStreamReader xmlReader, Object filter, XARFilter proxyFilter,
        XARInputProperties properties) throws XMLStreamException, WikiStreamException
    {
        String propertyName = xmlReader.getLocalName();

        String propertyType = null;

        Map<String, String> fields = new HashMap<String, String>();
        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();
            String value = xmlReader.getElementText();

            fields.put(elementName, value);

            if (elementName.equals(XARClassPropertyModel.ELEMENT_CLASSTYPE)) {
                propertyType = value;
            }
        }

        if (propertyType == null) {
            throw new WikiStreamException(String.format("No <classType> element found for property [%s]", propertyName));
        }

        // > WikiClassProperty

        proxyFilter.endWikiClassProperty(propertyName, propertyType, FilterEventParameters.EMPTY);

        // * WikiClassPropertyField

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            proxyFilter.onWikiClassPropertyField(entry.getKey(), entry.getValue(), FilterEventParameters.EMPTY);
        }

        // > WikiClassProperty

        proxyFilter.endWikiClassProperty(propertyName, propertyType, FilterEventParameters.EMPTY);
    }

    private void readAttachment(XMLStreamReader xmlReader, Object filter, XARFilter proxyFilter,
        XARInputProperties properties) throws XMLStreamException
    {
        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            String value = xmlReader.getElementText();

            String parameterName = XARDocumentModel.DOCUMENTPARAMETERS.get(elementName);

            if (parameterName != null) {

            } else {
                if (XARDocumentModel.ELEMENT_SPACE.equals(elementName)) {

                } else if (XARDocumentModel.ELEMENT_REVISIONS.equals(elementName)) {

                }
            }
        }
    }
}
