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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiDocumentFilter;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.input.XARInputProperties.SourceType;
import org.xwiki.filter.xar.internal.XARAttachmentModel;
import org.xwiki.filter.xar.internal.XARClassModel;
import org.xwiki.filter.xar.internal.XARDocumentModel;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.filter.xar.internal.XARObjectModel;
import org.xwiki.filter.xar.internal.input.AttachmentReader.WikiAttachment;
import org.xwiki.filter.xar.internal.input.ClassReader.WikiClass;
import org.xwiki.filter.xar.internal.input.WikiObjectReader.WikiObject;
import org.xwiki.filter.xml.internal.input.XMLInputFilterStreamUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.xar.internal.model.XarDocumentModel;
import org.xwiki.xml.stax.StAXUtils;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component(roles = DocumentLocaleReader.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentLocaleReader extends AbstractReader
{
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeResolver;

    @Inject
    private XARXMLReader<WikiObjectReader.WikiObject> objectReader;

    @Inject
    private XARXMLReader<WikiObjectPropertyReader.WikiObjectProperty> objectPropertyReader;

    @Inject
    private XARXMLReader<ClassReader.WikiClass> classReader;

    @Inject
    private XARXMLReader<ClassPropertyReader.WikiClassProperty> classPropertyReader;

    @Inject
    private XARXMLReader<AttachmentReader.WikiAttachment> attachmentReader;

    private XARInputProperties properties;

    private String currentLegacySpace;

    private String currentLegacyDocument;

    private EntityReference sentSpaceReference;

    private EntityReference currentSpaceReference;

    private EntityReference currentDocumentReference;

    private Locale currentDocumentLocale;

    private boolean localeFromLegacy = true;

    private String currentDocumentRevision;

    private FilterEventParameters currentDocumentParameters = new FilterEventParameters();

    private FilterEventParameters currentDocumentLocaleParameters = new FilterEventParameters();

    private FilterEventParameters currentDocumentRevisionParameters = new FilterEventParameters();

    private boolean sentBeginWikiDocument;

    private boolean sentBeginWikiDocumentLocale;

    private boolean sentBeginWikiDocumentRevision;

    private SourceType currentSourceType = SourceType.DOCUMENT;

    private WikiClass currentClass = new WikiClass();

    private Queue<WikiObject> currentObjects = new LinkedList<WikiObject>();

    private Queue<WikiAttachment> currentAttachments = new LinkedList<WikiAttachment>();

    public void setProperties(XARInputProperties properties)
    {
        this.properties = properties;
    }

    public EntityReference getSentSpaceReference()
    {
        return this.sentSpaceReference;
    }

    public EntityReference getCurrentSpaceReference()
    {
        return this.currentSpaceReference;
    }

    public EntityReference getCurrentDocumentReference()
    {
        return this.currentDocumentReference;
    }

    private void resetDocument()
    {
        this.currentSpaceReference = null;
        this.currentLegacySpace = null;

        this.currentDocumentReference = null;
        this.currentLegacyDocument = null;
        this.currentDocumentLocale = null;
        this.currentDocumentRevision = null;

        this.currentDocumentParameters = new FilterEventParameters();
        // Defaults
        this.currentDocumentParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, Locale.ROOT);

        this.currentDocumentLocaleParameters = new FilterEventParameters();
        this.currentDocumentRevisionParameters = new FilterEventParameters();

        this.sentBeginWikiDocument = false;
        this.sentBeginWikiDocumentLocale = false;
        this.sentBeginWikiDocumentRevision = false;

        this.localeFromLegacy = true;
    }

    private void switchWikiSpace(XARInputFilter proxyFilter, boolean force) throws FilterException
    {
        if (canSendEndWikiSpace(force)) {
            sendEndWikiSpace(proxyFilter, force);
        }

        if (canSendBeginWikiSpace(force)) {
            sendBeginWikiSpace(proxyFilter, force);
        }
    }

    private boolean canSendBeginWikiSpace(boolean force)
    {
        return (this.sentSpaceReference == null || !this.sentSpaceReference.equals(this.currentSpaceReference))
            && (force || this.properties.getEntities() == null);
    }

    private void sendBeginWikiSpace(XARInputFilter proxyFilter, boolean force) throws FilterException
    {
        int sentSize = this.sentSpaceReference != null ? this.sentSpaceReference.size() : 0;
        int size = this.currentSpaceReference != null ? this.currentSpaceReference.size() : 0;

        int diff = size - sentSize;

        if (diff > 0) {
            List<EntityReference> spaces = this.currentSpaceReference.getReversedReferenceChain();
            for (int i = spaces.size() - diff; i < spaces.size(); ++i) {
                proxyFilter.beginWikiSpace(spaces.get(i).getName(), FilterEventParameters.EMPTY);
                this.sentSpaceReference =
                    new EntityReference(spaces.get(i).getName(), EntityType.SPACE, this.sentSpaceReference);
            }
        }
    }

    private boolean canSendEndWikiSpace(boolean force)
    {
        return this.sentSpaceReference != null && !this.sentSpaceReference.equals(this.currentSpaceReference)
            && (force || this.properties.getEntities() == null);
    }

    private void sendEndWikiSpace(XARInputFilter proxyFilter, boolean force) throws FilterException
    {
        List<EntityReference> sentSpaces = this.sentSpaceReference.getReversedReferenceChain();
        List<EntityReference> currentSpaces = this.currentSpaceReference.getReversedReferenceChain();

        // Find the first different level
        int i = 0;
        while (i < sentSpaces.size() && i < currentSpaces.size()) {
            if (!currentSpaces.get(i).equals(sentSpaces.get(i))) {
                break;
            }

            ++i;
        }

        if (i < sentSpaces.size()) {
            // Delete what is different
            for (int diff = sentSpaces.size() - i; diff > 0; --diff, this.sentSpaceReference =
                this.sentSpaceReference.getParent()) {
                proxyFilter.endWikiSpace(this.sentSpaceReference.getName(), FilterEventParameters.EMPTY);
            }
        }
    }

    private boolean canSendBeginWikiDocument(boolean force)
    {
        return this.sentSpaceReference != null && !this.sentBeginWikiDocument
            && (force || (this.currentDocumentReference != null
                && this.currentDocumentParameters.size() == XARDocumentModel.DOCUMENT_PARAMETERS.size()
                && this.properties.getEntities() == null));
    }

    private void sendBeginWikiDocument(XARInputFilter proxyFilter, boolean force) throws FilterException
    {
        switchWikiSpace(proxyFilter, force);

        if (canSendBeginWikiDocument(force)) {
            switchWikiSpace(proxyFilter, true);

            proxyFilter.beginWikiDocument(this.currentDocumentReference.getName(), this.currentDocumentParameters);
            this.sentBeginWikiDocument = true;
        }
    }

    private void sendEndWikiDocument(XARInputFilter proxyFilter) throws FilterException
    {
        sendBeginWikiDocument(proxyFilter, true);
        sendEndWikiDocumentLocale(proxyFilter);

        proxyFilter.endWikiDocument(this.currentDocumentReference.getName(), this.currentDocumentParameters);
        this.sentBeginWikiDocument = false;
    }

    private boolean canSendBeginWikiDocumentLocale(boolean force)
    {
        return this.sentBeginWikiDocument && !this.sentBeginWikiDocumentLocale
            && (force || (this.currentDocumentLocale != null
                && this.currentDocumentLocaleParameters.size() == XARDocumentModel.DOCUMENTLOCALE_PARAMETERS.size()));
    }

    private void sendBeginWikiDocumentLocale(XARInputFilter proxyFilter, boolean force) throws FilterException
    {
        if (force || (this.currentDocumentReference != null && this.currentDocumentLocale != null)) {
            LocalDocumentReference reference =
                new LocalDocumentReference(this.currentDocumentReference, this.currentDocumentLocale);

            if (this.properties.getEntities() != null && !this.properties.getEntities().matches(reference)) {
                throw new SkipEntityException(reference);
            }

            sendBeginWikiDocument(proxyFilter, force);

            if (canSendBeginWikiDocumentLocale(force)) {
                if (!this.properties.isWithHistory()) {
                    this.currentDocumentLocaleParameters.remove(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS);
                }

                proxyFilter.beginWikiDocumentLocale(this.currentDocumentLocale, this.currentDocumentLocaleParameters);
                this.sentBeginWikiDocumentLocale = true;
            }
        }
    }

    private void sendEndWikiDocumentLocale(XARInputFilter proxyFilter) throws FilterException
    {
        sendBeginWikiDocumentLocale(proxyFilter, true);
        sendEndWikiDocumentRevision(proxyFilter);

        proxyFilter.endWikiDocumentLocale(this.currentDocumentLocale, this.currentDocumentLocaleParameters);
        this.sentBeginWikiDocumentLocale = false;
    }

    private boolean canSendBeginWikiDocumentRevision(boolean force)
    {
        return this.sentBeginWikiDocumentLocale && !this.sentBeginWikiDocumentRevision
            && (force || (this.currentDocumentRevision != null && this.currentDocumentRevisionParameters
                .size() == XARDocumentModel.DOCUMENTREVISION_PARAMETERS.size()));
    }

    private void sendBeginWikiDocumentRevision(XARInputFilter proxyFilter, boolean force) throws FilterException
    {
        sendBeginWikiDocumentLocale(proxyFilter, force);

        if (canSendBeginWikiDocumentRevision(force)) {
            proxyFilter.beginWikiDocumentRevision(this.currentDocumentRevision, this.currentDocumentRevisionParameters);
            this.sentBeginWikiDocumentRevision = true;
        }
    }

    private void sendEndWikiDocumentRevision(XARInputFilter proxyFilter) throws FilterException
    {
        sendBeginWikiDocumentRevision(proxyFilter, true);

        proxyFilter.endWikiDocumentRevision(this.currentDocumentRevision, this.currentDocumentRevisionParameters);
        this.sentBeginWikiDocumentRevision = false;
    }

    public void read(Object filter, XARInputFilter proxyFilter) throws XMLStreamException, IOException, FilterException
    {
        XMLStreamReader xmlReader = XMLInputFilterStreamUtils.createXMLStreamReader(this.properties);

        try {
            read(xmlReader, filter, proxyFilter);
        } finally {
            this.properties.getSource().close();
        }
    }

    public void read(InputStream stream, Object filter, XARInputFilter proxyFilter)
        throws XMLStreamException, FilterException
    {
        XMLStreamReader xmlReader = this.properties.getEncoding() != null
            ? XML_INPUT_FACTORY.createXMLStreamReader(stream, this.properties.getEncoding())
            : XML_INPUT_FACTORY.createXMLStreamReader(stream);

        read(xmlReader, filter, proxyFilter);
    }

    public void read(XMLStreamReader xmlReader, Object filter, XARInputFilter proxyFilter)
        throws XMLStreamException, FilterException
    {
        resetDocument();

        // <xwikidoc>

        xmlReader.nextTag();

        this.currentSourceType = this.properties.getSourceType();
        if (this.currentSourceType != null) {
            switch (this.currentSourceType) {
                case ATTACHMENT:
                    readAttachment(xmlReader, filter, proxyFilter);
                    break;

                case CLASS:
                    readClass(xmlReader, filter, proxyFilter);

                    break;

                case CLASSPROPERTY:
                    readClassProperty(xmlReader, filter, proxyFilter);

                    break;

                case OBJECT:
                    readObject(xmlReader, filter, proxyFilter);

                    break;

                case OBJECTPROPERTY:
                    readObjectProperty(xmlReader, filter, proxyFilter);

                    break;

                default:
                    readDocument(xmlReader, filter, proxyFilter);

                    break;
            }
        } else {
            readDocument(xmlReader, filter, proxyFilter);
        }
    }

    private void readDocument(XMLStreamReader xmlReader, Object filter, XARInputFilter proxyFilter)
        throws XMLStreamException, FilterException
    {
        xmlReader.require(XMLStreamReader.START_ELEMENT, null, XarDocumentModel.ELEMENT_DOCUMENT);

        this.currentSourceType = SourceType.DOCUMENT;

        // Initialize with a few defaults (thing that don't exist in old XAR format)
        this.currentDocumentRevisionParameters.put(XWikiWikiDocumentFilter.PARAMETER_SYNTAX, Syntax.XWIKI_1_0);
        this.currentDocumentRevisionParameters.put(XWikiWikiDocumentFilter.PARAMETER_HIDDEN, false);
        this.currentDocumentRevisionParameters.put(XWikiWikiDocumentFilter.PARAMETER_ENFORCE_REQUIRED_RIGHTS, false);

        // Reference
        String referenceString = xmlReader.getAttributeValue(null, XARDocumentModel.ATTRIBUTE_DOCUMENT_REFERENCE);
        if (StringUtils.isNotEmpty(referenceString)) {
            this.currentDocumentReference = this.relativeResolver.resolve(referenceString, EntityType.DOCUMENT);
            this.currentSpaceReference = this.currentDocumentReference.getParent();

            // Send needed wiki spaces event if possible
            switchWikiSpace(proxyFilter, false);
        }

        // Locale
        String localeString = xmlReader.getAttributeValue(null, XARDocumentModel.ATTRIBUTE_DOCUMENT_LOCALE);
        if (localeString != null) {
            this.currentDocumentLocale = toLocale(localeString);
            this.localeFromLegacy = false;
        }

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (elementName.equals(XARAttachmentModel.ELEMENT_ATTACHMENT)) {
                readAttachment(xmlReader, filter, proxyFilter);
            } else if (elementName.equals(XARObjectModel.ELEMENT_OBJECT)) {
                readObject(xmlReader, filter, proxyFilter);
            } else if (elementName.equals(XARClassModel.ELEMENT_CLASS)) {
                readClass(xmlReader, filter, proxyFilter);
            } else {
                if (XarDocumentModel.ELEMENT_SPACE.equals(elementName)) {
                    this.currentLegacySpace = xmlReader.getElementText();

                    if (this.currentDocumentReference == null) {
                        // Its an old thing
                        if (this.currentLegacyDocument == null) {
                            this.currentSpaceReference = new EntityReference(this.currentLegacySpace, EntityType.SPACE);
                        } else {
                            this.currentDocumentReference =
                                new LocalDocumentReference(this.currentLegacySpace, this.currentLegacyDocument);
                            this.currentSpaceReference = this.currentDocumentReference.getParent();
                        }

                        // Send needed wiki spaces event if possible
                        switchWikiSpace(proxyFilter, false);
                    }
                } else if (XarDocumentModel.ELEMENT_NAME.equals(elementName)) {
                    this.currentLegacyDocument = xmlReader.getElementText();

                    if (this.currentDocumentReference == null) {
                        // Its an old thing
                        if (this.currentLegacySpace != null) {
                            this.currentDocumentReference =
                                new LocalDocumentReference(this.currentLegacySpace, this.currentLegacyDocument);
                            this.currentSpaceReference = this.currentDocumentReference.getParent();
                        }
                    }
                } else if (XarDocumentModel.ELEMENT_LOCALE.equals(elementName)) {
                    if (this.localeFromLegacy) {
                        this.currentDocumentLocale = toLocale(xmlReader.getElementText());
                    } else {
                        StAXUtils.skipElement(xmlReader);
                    }
                } else if (XarDocumentModel.ELEMENT_REVISION.equals(elementName)) {
                    this.currentDocumentRevision = xmlReader.getElementText();
                } else {
                    EventParameter parameter = XARDocumentModel.DOCUMENT_PARAMETERS.get(elementName);

                    if (parameter != null) {
                        Object wsValue = convert(parameter.type, xmlReader.getElementText());
                        if (wsValue != null) {
                            this.currentDocumentParameters.put(parameter.name, wsValue);
                        }
                    } else {
                        parameter = XARDocumentModel.DOCUMENTLOCALE_PARAMETERS.get(elementName);

                        if (parameter != null) {
                            Object wsValue = convert(parameter.type, xmlReader.getElementText());
                            if (wsValue != null) {
                                this.currentDocumentLocaleParameters.put(parameter.name, wsValue);
                            }
                        } else {
                            parameter = XARDocumentModel.DOCUMENTREVISION_PARAMETERS.get(elementName);

                            if (parameter != null) {
                                Object objectValue;
                                if (parameter.type == EntityReference.class) {
                                    objectValue =
                                        this.relativeResolver.resolve(xmlReader.getElementText(), EntityType.DOCUMENT);
                                } else {
                                    objectValue = convert(parameter.type, xmlReader.getElementText());
                                }

                                if (objectValue != null) {
                                    this.currentDocumentRevisionParameters.put(parameter.name, objectValue);
                                }
                            } else if (!XARDocumentModel.DOCUMENT_SKIPPEDPARAMETERS.contains(elementName)) {
                                unknownElement(xmlReader);
                            } else {
                                StAXUtils.skipElement(xmlReader);
                            }
                        }
                    }
                }
            }
        }

        sendBeginWikiDocumentRevision(proxyFilter, true);
        sendWikiAttachments(proxyFilter);
        sendWikiClass(proxyFilter);
        sendWikiObjects(proxyFilter);
        sendEndWikiDocument(proxyFilter);
    }

    private void readObject(XMLStreamReader xmlReader, Object filter, XARInputFilter proxyFilter)
        throws XMLStreamException, FilterException
    {
        if (this.currentSourceType == SourceType.DOCUMENT) {
            sendBeginWikiDocumentRevision(proxyFilter, false);
        }

        WikiObject wikiObject = this.objectReader.read(xmlReader, this.properties);

        if (this.currentSourceType != SourceType.DOCUMENT || this.sentBeginWikiDocumentRevision) {
            wikiObject.send(proxyFilter);
        } else {
            this.currentObjects.offer(wikiObject);
        }
    }

    private void readObjectProperty(XMLStreamReader xmlReader, Object filter, XARInputFilter proxyFilter)
        throws XMLStreamException, FilterException
    {
        this.objectPropertyReader.read(xmlReader, this.properties).send(proxyFilter);
    }

    private void readClass(XMLStreamReader xmlReader, Object filter, XARInputFilter proxyFilter)
        throws XMLStreamException, FilterException
    {
        if (this.currentSourceType == SourceType.DOCUMENT) {
            sendBeginWikiDocumentRevision(proxyFilter, false);
        }

        this.currentClass = this.classReader.read(xmlReader, this.properties);

        if (this.currentSourceType != SourceType.DOCUMENT || this.sentBeginWikiDocumentRevision) {
            sendWikiClass(proxyFilter);
        }
    }

    private void readClassProperty(XMLStreamReader xmlReader, Object filter, XARInputFilter proxyFilter)
        throws XMLStreamException, FilterException
    {
        this.classPropertyReader.read(xmlReader, this.properties).send(proxyFilter);
    }

    private void readAttachment(XMLStreamReader xmlReader, Object filter, XARInputFilter proxyFilter)
        throws XMLStreamException, FilterException
    {
        if (this.currentSourceType == SourceType.DOCUMENT) {
            sendBeginWikiDocumentRevision(proxyFilter, false);
        }

        WikiAttachment wikiAttachment = this.attachmentReader.read(xmlReader, this.properties);

        if (this.currentSourceType != SourceType.DOCUMENT || this.sentBeginWikiDocumentRevision) {
            wikiAttachment.send(proxyFilter);
        } else {
            this.currentAttachments.offer(wikiAttachment);
        }
    }

    private void sendWikiClass(XARInputFilter proxyFilter) throws FilterException
    {
        if (this.currentClass != null && !this.currentClass.isEmpty()) {
            this.currentClass.send(proxyFilter);
            this.currentClass = null;
        }
    }

    private void sendWikiObjects(XARInputFilter proxyFilter) throws FilterException
    {
        while (!this.currentObjects.isEmpty()) {
            this.currentObjects.poll().send(proxyFilter);
        }
    }

    private void sendWikiAttachments(XARInputFilter proxyFilter) throws FilterException
    {
        while (!this.currentAttachments.isEmpty()) {
            this.currentAttachments.poll().send(proxyFilter);
        }
    }
}
