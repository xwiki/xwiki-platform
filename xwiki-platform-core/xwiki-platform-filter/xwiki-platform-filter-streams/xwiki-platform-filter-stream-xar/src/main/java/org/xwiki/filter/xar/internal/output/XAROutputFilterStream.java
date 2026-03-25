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
package org.xwiki.filter.xar.internal.output;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiClassFilter;
import org.xwiki.filter.event.model.WikiObjectFilter;
import org.xwiki.filter.event.model.WikiObjectPropertyFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiAttachmentFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiDocumentFilter;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.filter.output.AbstractBeanOutputFilterStream;
import org.xwiki.filter.output.WriterOutputTarget;
import org.xwiki.filter.xar.internal.XARAttachmentModel;
import org.xwiki.filter.xar.internal.XARClassModel;
import org.xwiki.filter.xar.internal.XARClassPropertyModel;
import org.xwiki.filter.xar.internal.XARFilter;
import org.xwiki.filter.xar.internal.XARFilterUtils;
import org.xwiki.filter.xar.internal.XARObjectModel;
import org.xwiki.filter.xar.internal.XARObjectPropertyModel;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.filter.xml.internal.output.FilterStreamXMLStreamWriter;
import org.xwiki.filter.xml.output.ResultOutputTarget;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.xar.internal.XarObjectPropertySerializerManager;
import org.xwiki.xar.internal.model.XarDocumentModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component(hints = {
    XARFilterUtils.ROLEHINT_17,
    XARFilterUtils.ROLEHINT_16,
    XARFilterUtils.ROLEHINT_15,
    XARFilterUtils.ROLEHINT_14,
    XARFilterUtils.ROLEHINT_13,
    XARFilterUtils.ROLEHINT_12,
    XARFilterUtils.ROLEHINT_11
})
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XAROutputFilterStream extends AbstractBeanOutputFilterStream<XAROutputProperties> implements XARFilter
{

    /**
     * the size of the chunks used when attachments are streamed. As the attachment data is base64 encoded in the
     * process, this size must be a multiple of three to prevent padding between the chunks; see XWIKI-9830
     */
    private static final int ATTACHMENT_BUFFER_CHUNK_SIZE = 4095;

    private static final String FAILED_WRITE_ATTACHMENT_ERROR =
        "Failed to write attachment [%s] from document [%s] with version [%s]";

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private EntityReferenceSerializer<String> defaultSerializer;

    @Inject
    private XarObjectPropertySerializerManager propertySerializerManager;

    private XARWikiWriter wikiWriter;

    private EntityReference currentSpaceReference;

    private String currentDocument;

    private FilterEventParameters currentDocumentParameters;

    private LocalDocumentReference currentDocumentReference;

    private String currentDocumentVersion;

    private String currentObjectClass;

    private FilterStreamXMLStreamWriter writer;

    private Map<String, String> currentObjectProperties;

    @Override
    public void close() throws IOException
    {
        if (this.writer != null) {
            try {
                this.writer.close();
                this.writer = null;
            } catch (FilterException e) {
                throw new IOException("Failed to close XML writer", e);
            }
        }

        if (this.wikiWriter != null) {
            this.wikiWriter.close();
            this.wikiWriter = null;
        }

        this.properties.getTarget().close();
    }

    private String toString(Object obj)
    {
        String result;
        if (obj instanceof String objString) {
            result = objString;
        } else if (obj instanceof Date objDate) {
            result = toString(objDate);
        } else if (obj instanceof Syntax objSyntax) {
            result = toString(objSyntax);
        } else if (obj instanceof EntityReference objEntityReference) {
            result = toString(objEntityReference);
        } else {
            result = Objects.toString(obj, null);
        }
        return result;
    }

    private String toString(Date date)
    {
        return date != null ? String.valueOf(date.getTime()) : null;
    }

    private String toString(Syntax syntax)
    {
        return syntax != null ? syntax.toIdString() : null;
    }

    private String toString(EntityReference reference)
    {
        return this.defaultSerializer.serialize(reference);
    }

    private void writeElementIfParameterExists(FilterEventParameters parameters, String parameterKey, String element)
        throws FilterException
    {
        if (parameters.containsKey(parameterKey)) {
            this.writer.writeElement(element, toString(parameters.get(parameterKey)));
        }
    }

    // events

    @Override
    public void beginWikiFarm(FilterEventParameters parameters) throws FilterException
    {
    }

    @Override
    public void endWikiFarm(FilterEventParameters parameters) throws FilterException
    {
    }

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.wikiWriter = new XARWikiWriter(
            this.properties.getPackageName() != null ? this.properties.getPackageName() : name, this.properties);
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        try {
            this.wikiWriter.close();
        } catch (IOException e) {
            throw new FilterException("Failed to close XAR writer", e);
        }

        this.wikiWriter = null;
    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentSpaceReference = currentSpaceReference == null ? new EntityReference(name, EntityType.SPACE)
            : new EntityReference(name, EntityType.SPACE, this.currentSpaceReference);
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentSpaceReference = this.currentSpaceReference.getParent();
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentDocument = name;
        this.currentDocumentParameters = parameters;

        this.currentDocumentReference = new LocalDocumentReference(this.currentDocument, this.currentSpaceReference);
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentDocument = null;
        this.currentDocumentReference = null;
        this.currentDocumentParameters = null;
    }

    private boolean isTargetTextualContent()
    {
        return this.properties.getTarget() instanceof WriterOutputTarget
            || this.properties.getTarget() instanceof ResultOutputTarget;
    }

    private void checkXMLWriter() throws FilterException
    {
        if (this.writer == null) {
            this.writer = new FilterStreamXMLStreamWriter(this.properties, true);
        }
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        initializeWriter(locale);

        this.writer.writeStartDocument(this.properties.getEncoding(), "1.1");

        this.writer.writeStartElement(XarDocumentModel.ELEMENT_DOCUMENT);
        this.writer.writeAttribute(XarDocumentModel.ATTRIBUTE_DOCUMENT_SPECVERSION, XarDocumentModel.VERSION_CURRENT);
        this.writer.writeAttribute(XarDocumentModel.ATTRIBUTE_DOCUMENT_REFERENCE,
            localSerializer.serialize(this.currentDocumentReference));
        this.writer.writeAttribute(XarDocumentModel.ATTRIBUTE_DOCUMENT_LOCALE, toString(locale));

        // Legacy space and name
        if (this.currentDocumentReference.getParent().getParent() == null) {
            // If single space behave as it used to and put the space name instead of the reference to keep
            // compatibility when importing in older version
            this.writer.writeElement(XarDocumentModel.ELEMENT_SPACE,
                this.currentDocumentReference.getParent().getName());
        } else {
            // If nested space put the space reference in the field
            this.writer.writeElement(XarDocumentModel.ELEMENT_SPACE,
                defaultSerializer.serialize(this.currentDocumentReference.getParent()));
        }
        this.writer.writeElement(XarDocumentModel.ELEMENT_NAME, this.currentDocumentReference.getName());

        // Legacy locale
        this.writer.writeElement(XarDocumentModel.ELEMENT_LOCALE, toString(locale));

        this.writer.writeElement(XarDocumentModel.ELEMENT_DEFAULTLOCALE,
            toString(this.currentDocumentParameters.get(XWikiWikiDocumentFilter.PARAMETER_LOCALE)));
        this.writer.writeElement(XarDocumentModel.ELEMENT_ISTRANSLATION,
            locale != null && !Locale.ROOT.equals(locale) ? "1" : "0");

        writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_CREATION_AUTHOR,
            XarDocumentModel.ELEMENT_CREATION_AUTHOR);
        writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_CREATION_DATE,
            XarDocumentModel.ELEMENT_CREATION_DATE);

        if (this.properties.isPreserveVersion()) {
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS,
                XarDocumentModel.ELEMENT_REVISIONS);
        }
    }

    private void initializeWriter(Locale locale) throws FilterException
    {
        if (this.writer == null) {
            if (this.wikiWriter == null && (this.properties.isForceDocument() || isTargetTextualContent())) {
                checkXMLWriter();
            } else {
                if (this.wikiWriter == null) {
                    this.wikiWriter = new XARWikiWriter(
                        this.properties.getPackageName() != null ? this.properties.getPackageName() : "package",
                        this.properties);
                }

                this.writer = new FilterStreamXMLStreamWriter(
                    this.wikiWriter.newEntry(new LocalDocumentReference(this.currentDocumentReference, locale)),
                    this.properties.getEncoding(), this.properties.isFormat(), true);
            }
        }
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        this.writer.writeEndElement();
        this.writer.writeEndDocument();

        this.writer.close();

        if (this.wikiWriter != null) {
            this.wikiWriter.closeEntry();
        }

        this.writer = null;
    }

    @Override
    public void beginWikiDocumentRevision(String revision, FilterEventParameters parameters) throws FilterException
    {
        checkXMLWriter();

        this.currentDocumentVersion = revision;

        try {
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_PARENT,
                XarDocumentModel.ELEMENT_PARENT);
            writeElementIfParameterExists(parameters,
                XWikiWikiDocumentFilter.PARAMETER_REVISION_EFFECTIVEMETADATA_AUTHOR,
                XarDocumentModel.ELEMENT_REVISION_EFFECTIVEMEDATAAUTHOR);
            writeElementIfParameterExists(parameters,
                XWikiWikiDocumentFilter.PARAMETER_REVISION_ORIGINALMETADATA_AUTHOR,
                XarDocumentModel.ELEMENT_REVISION_ORIGINALMEDATAAUTHOR);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_CUSTOMCLASS,
                XarDocumentModel.ELEMENT_CUSTOMCLASS);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_CONTENT_AUTHOR,
                XarDocumentModel.ELEMENT_CONTENT_AUTHOR);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_REVISION_DATE,
                XarDocumentModel.ELEMENT_REVISION_DATE);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_CONTENT_DATE,
                XarDocumentModel.ELEMENT_CONTENT_DATE);
            this.writer.writeElement(XarDocumentModel.ELEMENT_REVISION, this.currentDocumentVersion);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_TITLE,
                XarDocumentModel.ELEMENT_TITLE);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE,
                XarDocumentModel.ELEMENT_DEFAULTTEMPLATE);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT,
                XarDocumentModel.ELEMENT_VALIDATIONSCRIPT);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_REVISION_COMMENT,
                XarDocumentModel.ELEMENT_REVISION_COMMENT);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_REVISION_MINOR,
                XarDocumentModel.ELEMENT_REVISION_MINOR);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_SYNTAX,
                XarDocumentModel.ELEMENT_SYNTAX);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_HIDDEN,
                XarDocumentModel.ELEMENT_HIDDEN);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_ENFORCE_REQUIRED_RIGHTS,
                XarDocumentModel.ELEMENT_ENFORCE_REQUIRED_RIGHTS);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_CONTENT,
                XarDocumentModel.ELEMENT_CONTENT);
            writeElementIfParameterExists(parameters, XWikiWikiDocumentFilter.PARAMETER_CONTENT_HTML,
                XarDocumentModel.ELEMENT_CONTENT_HTML);
        } catch (Exception e) {
            throw new FilterException(String.format("Failed to write begin document [%s] with version [%s]",
                this.currentDocumentReference, this.currentDocumentVersion), e);
        }
    }

    @Override
    public void endWikiDocumentRevision(String revision, FilterEventParameters parameters) throws FilterException
    {
        this.currentDocumentVersion = null;
    }

    @Override
    public void onWikiAttachment(String name, InputStream content, Long size, FilterEventParameters parameters)
        throws FilterException
    {
        try {
            beginWikiDocumentAttachment(name, content != null ? new DefaultInputStreamInputSource(content) : null, size,
                parameters);
            endWikiDocumentAttachment(name, null, size, parameters);
        } catch (Exception e) {
            throw new FilterException(
                String.format(FAILED_WRITE_ATTACHMENT_ERROR, name,
                    this.currentDocumentReference, this.currentDocumentVersion),
                e);
        }
    }

    @Override
    public void beginWikiDocumentAttachment(String name, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {
        checkXMLWriter();

        try {
            this.writer.writeStartElement(XARAttachmentModel.ELEMENT_ATTACHMENT);

            this.writer.writeElement(XARAttachmentModel.ELEMENT_NAME, name);
            if (this.properties.isPreserveVersion()) {
                writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS,
                    XARAttachmentModel.ELEMENT_JRCSVERSIONS);
            }

            writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_MIMETYPE,
                XARAttachmentModel.ELEMENT_MIMETYPE);
            writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_CHARSET,
                XARAttachmentModel.ELEMENT_CHARSET);

            // Author
            writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_REVISION_AUTHOR,
                XARAttachmentModel.ELEMENT_REVISION_AUTHOR);

            // Date
            writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_REVISION_DATE,
                XARAttachmentModel.ELEMENT_REVISION_DATE);

            // Version
            writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_REVISION,
                XARAttachmentModel.ELEMENT_VERSION);

            // Comment
            writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_REVISION_COMMENT,
                XARAttachmentModel.ELEMENT_REVISION_COMMENT);

            // Content
            writeContent(content, size);
        } catch (Exception e) {
            throw new FilterException(
                String.format(FAILED_WRITE_ATTACHMENT_ERROR, name,
                    this.currentDocumentReference, this.currentDocumentVersion),
                e);
        }
    }

    @Override
    public void endWikiDocumentAttachment(String name, InputSource content, Long size, FilterEventParameters parameters)
        throws FilterException
    {
        try {
            this.writer.writeEndElement();
        } catch (Exception e) {
            throw new FilterException(
                String.format(FAILED_WRITE_ATTACHMENT_ERROR, name,
                    this.currentDocumentReference, this.currentDocumentVersion),
                e);
        }
    }

    @Override
    public void beginWikiAttachmentRevisions(FilterEventParameters parameters) throws FilterException
    {
        this.writer.writeStartElement(XARAttachmentModel.ELEMENT_REVISIONS);
    }

    @Override
    public void endWikiAttachmentRevisions(FilterEventParameters parameters) throws FilterException
    {
        this.writer.writeEndElement();
    }

    @Override
    public void beginWikiAttachmentRevision(String version, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {
        this.writer.writeStartElement(XARAttachmentModel.ELEMENT_REVISION);

        // Author
        writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_REVISION_AUTHOR,
            XARAttachmentModel.ELEMENT_REVISION_AUTHOR);

        // Date
        writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_REVISION_DATE,
            XARAttachmentModel.ELEMENT_REVISION_DATE);

        // Version
        writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_REVISION,
            XARAttachmentModel.ELEMENT_VERSION);

        // Comment
        writeElementIfParameterExists(parameters, XWikiWikiAttachmentFilter.PARAMETER_REVISION_COMMENT,
            XARAttachmentModel.ELEMENT_REVISION_COMMENT);

        // Revision content storage optimization
        String contentAlias = (String) parameters.get(XWikiWikiAttachmentFilter.PARAMETER_REVISION_CONTENT_ALIAS);
        if (contentAlias != null) {
            this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISION_CONTENT_ALIAS, contentAlias);
        }

        // Content
        writeContent(content, size, contentAlias);
    }

    @Override
    public void endWikiAttachmentRevision(String version, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {
        this.writer.writeEndElement();
    }

    private void writeContent(InputSource content, Long size) throws FilterException
    {
        writeContent(content, size, null);
    }

    private void writeContent(InputSource content, Long size, String contentAlias) throws FilterException
    {
        if (content != null && (!this.properties.isOptimized() || contentAlias == null)) {
            writeContent(content);
        } else if (size != null) {
            this.writer.writeElement(XARAttachmentModel.ELEMENT_CONTENT_SIZE, toString(size));
        }
    }

    private void writeContent(InputSource content) throws FilterException
    {
        this.writer.writeStartElement(XARAttachmentModel.ELEMENT_CONTENT);

        long contentSize = 0;

        try (InputSource source = content) {
            InputStream stream = getInputStream(source);

            byte[] buffer = new byte[ATTACHMENT_BUFFER_CHUNK_SIZE];
            int readSize;
            do {
                try {
                    readSize = stream.read(buffer, 0, ATTACHMENT_BUFFER_CHUNK_SIZE);
                } catch (IOException e) {
                    throw new FilterException("Failed to read content stream", e);
                }

                if (readSize > 0) {
                    String chunk;
                    if (readSize == ATTACHMENT_BUFFER_CHUNK_SIZE) {
                        chunk = Base64.encodeBase64String(buffer);
                    } else {
                        chunk = Base64.encodeBase64String(ArrayUtils.subarray(buffer, 0, readSize));
                    }
                    this.writer.writeCharacters(chunk);
                    contentSize += readSize;
                }
            } while (readSize == ATTACHMENT_BUFFER_CHUNK_SIZE);
        } catch (IOException e) {
            throw new FilterException("Failed to close stream", e);
        }

        this.writer.writeEndElement();

        this.writer.writeElement(XARAttachmentModel.ELEMENT_CONTENT_SIZE, toString(contentSize));
    }

    private InputStream getInputStream(InputSource content) throws FilterException
    {
        if (content instanceof InputStreamInputSource) {
            try {
                return ((InputStreamInputSource) content).getInputStream();
            } catch (IOException e) {
                throw new FilterException("Failed to get the content input stream", e);
            }
        } else {
            throw new FilterException("Unsupported input source with class [" + content.getClass() + "]");
        }
    }

    @Override
    public void beginWikiClass(FilterEventParameters parameters) throws FilterException
    {
        checkXMLWriter();

        try {
            this.writer.writeStartElement(XARClassModel.ELEMENT_CLASS);

            if (parameters.containsKey(WikiClassFilter.PARAMETER_NAME)) {
                this.writer.writeElement(XARClassModel.ELEMENT_NAME,
                    (String) parameters.get(WikiClassFilter.PARAMETER_NAME));
            } else {
                this.writer.writeElement(XARClassModel.ELEMENT_NAME, this.currentObjectClass != null
                    ? this.currentObjectClass : this.localSerializer.serialize(this.currentDocumentReference));
            }

            writeElementIfParameterExists(parameters, WikiClassFilter.PARAMETER_CUSTOMCLASS,
                XARClassModel.ELEMENT_CUSTOMCLASS);
            writeElementIfParameterExists(parameters, WikiClassFilter.PARAMETER_CUSTOMMAPPING,
                XARClassModel.ELEMENT_CUSTOMMAPPING);
            writeElementIfParameterExists(parameters, WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW,
                XARClassModel.ELEMENT_SHEET_DEFAULTVIEW);
            writeElementIfParameterExists(parameters, WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT,
                XARClassModel.ELEMENT_SHEET_DEFAULTEDIT);
            writeElementIfParameterExists(parameters, WikiClassFilter.PARAMETER_DEFAULTSPACE,
                XARClassModel.ELEMENT_DEFAULTSPACE);
            writeElementIfParameterExists(parameters, WikiClassFilter.PARAMETER_NAMEFIELD,
                XARClassModel.ELEMENT_NAMEFIELD);
            writeElementIfParameterExists(parameters, WikiClassFilter.PARAMETER_VALIDATIONSCRIPT,
                XARClassModel.ELEMENT_VALIDATIONSCRIPT);
        } catch (Exception e) {
            throw new FilterException(String.format("Failed to write begin xclass [%s] with version [%s]",
                this.currentDocumentReference, this.currentDocumentVersion), e);
        }
    }

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws FilterException
    {
        try {
            this.writer.writeEndElement();
        } catch (Exception e) {
            throw new FilterException(String.format("Failed to write end xclass [%s] with version [%s]",
                this.currentDocumentReference, this.currentDocumentVersion), e);
        }
    }

    @Override
    public void beginWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws FilterException
    {
        checkXMLWriter();

        try {
            this.writer.writeStartElement(name);

            if (this.currentObjectProperties != null) {
                this.currentObjectProperties.put(name, type);
            }
        } catch (Exception e) {
            throw new FilterException(
                String.format("Failed to write begin property [%s] from class [%s] with version [%s]", name,
                    this.currentDocumentReference, this.currentDocumentVersion),
                e);
        }
    }

    @Override
    public void endWikiClassProperty(String name, String type, FilterEventParameters parameters) throws FilterException
    {
        try {
            this.writer.writeElement(XARClassPropertyModel.ELEMENT_CLASSTYPE, type);

            this.writer.writeEndElement();
        } catch (Exception e) {
            throw new FilterException(
                String.format("Failed to write end property [%s] from class [%s] with version [%s]", name,
                    this.currentDocumentReference, this.currentDocumentVersion),
                e);
        }
    }

    @Override
    public void onWikiClassPropertyField(String name, String value, FilterEventParameters parameters)
        throws FilterException
    {
        checkXMLWriter();

        try {
            this.writer.writeElement(name, value);
        } catch (Exception e) {
            throw new FilterException(
                String.format("Failed to write property field [%s:%s] from class [%s] with version [%s]", name, value,
                    this.currentDocumentReference, this.currentDocumentVersion),
                e);
        }
    }

    @Override
    public void beginWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        checkXMLWriter();

        try {
            this.writer.writeStartElement(XARObjectModel.ELEMENT_OBJECT);

            this.currentObjectClass = (String) parameters.get(WikiObjectFilter.PARAMETER_CLASS_REFERENCE);

            if (parameters.containsKey(WikiObjectFilter.PARAMETER_NAME)) {
                this.writer.writeElement(XARObjectModel.ELEMENT_NAME,
                    (String) parameters.get(WikiObjectFilter.PARAMETER_NAME));
            } else {
                this.writer.writeElement(XARObjectModel.ELEMENT_NAME,
                    this.localSerializer.serialize(this.currentDocumentReference));
            }
            this.writer.writeElement(XARObjectModel.ELEMENT_NUMBER,
                toString(parameters.get(WikiObjectFilter.PARAMETER_NUMBER)));
            this.writer.writeElement(XARObjectModel.ELEMENT_CLASSNAME, this.currentObjectClass);

            if (parameters.containsKey(WikiObjectFilter.PARAMETER_GUID)) {
                this.writer.writeElement(XARObjectModel.ELEMENT_GUID,
                    (String) parameters.get(WikiObjectFilter.PARAMETER_GUID));
            }

            this.currentObjectProperties = new HashMap<>();
        } catch (Exception e) {
            throw new FilterException(
                String.format("Failed to write begin xobject [%s] from document [%s] with version [%s]", name,
                    this.currentDocumentReference, this.currentDocumentVersion),
                e);
        }
    }

    @Override
    public void endWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        try {
            this.writer.writeEndElement();

            this.currentObjectClass = null;
            this.currentObjectProperties = null;
        } catch (Exception e) {
            throw new FilterException(
                String.format("Failed to write end xobject [%s] from document [%s] with version [%s]", name,
                    this.currentDocumentReference, this.currentDocumentVersion),
                e);
        }
    }

    @Override
    public void onWikiObjectProperty(String name, Object value, FilterEventParameters parameters) throws FilterException
    {
        checkXMLWriter();

        try {
            this.writer.writeStartElement(XARObjectPropertyModel.ELEMENT_PROPERTY);
            Object objectPropertyType = parameters.get(WikiObjectPropertyFilter.PARAMETER_OBJECTPROPERTY_TYPE);
            boolean objectPropertyTypeExists = false;
            if (objectPropertyType instanceof String stringObjectPropertyType
                && StringUtils.isNotBlank(stringObjectPropertyType)) {
                this.writer.writeAttribute(XARObjectPropertyModel.ATTRIBUTE_TYPE, stringObjectPropertyType);
                objectPropertyTypeExists = true;
            }

            this.writer.writeStartElement(name);

            String type = (String) parameters.get(WikiObjectPropertyFilter.PARAMETER_TYPE);
            if (type == null && this.currentObjectProperties != null) {
                type = this.currentObjectProperties.get(name);
            }

            // we want to ensure to get the serializer based on the property type attribute when available if the
            // field doesn't exist anymore so that the serialization still can be used.
            if (type == null && objectPropertyTypeExists) {
                type = (String) objectPropertyType;
            }

            try {
                this.propertySerializerManager.getPropertySerializer(type).write(this.writer.getWriter(), value);
            } catch (Exception e) {
                throw new FilterException("Failed to write property value", e);
            }

            this.writer.writeEndElement();

            this.writer.writeEndElement();
        } catch (Exception e) {
            throw new FilterException(
                String.format("Failed to write xobject property [%s:%s] from document [%s] with version [%s]", name,
                    value, this.currentDocumentReference, this.currentDocumentVersion),
                e);
        }
    }
}
