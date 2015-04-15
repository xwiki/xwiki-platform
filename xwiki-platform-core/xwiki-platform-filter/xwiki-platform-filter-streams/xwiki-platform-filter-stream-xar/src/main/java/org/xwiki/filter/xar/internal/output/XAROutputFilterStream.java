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
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiClassFilter;
import org.xwiki.filter.event.model.WikiObjectFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiAttachmentFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiDocumentFilter;
import org.xwiki.filter.output.AbstractBeanOutputFilterStream;
import org.xwiki.filter.output.WriterOutputTarget;
import org.xwiki.filter.xar.internal.XARAttachmentModel;
import org.xwiki.filter.xar.internal.XARClassModel;
import org.xwiki.filter.xar.internal.XARClassPropertyModel;
import org.xwiki.filter.xar.internal.XARFilter;
import org.xwiki.filter.xar.internal.XARObjectModel;
import org.xwiki.filter.xar.internal.XARObjectPropertyModel;
import org.xwiki.filter.xar.internal.XARFilterUtils;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.filter.xml.internal.output.FilterStreamXMLStreamWriter;
import org.xwiki.filter.xml.output.ResultOutputTarget;
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
@Component
@Named(XARFilterUtils.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XAROutputFilterStream extends AbstractBeanOutputFilterStream<XAROutputProperties> implements XARFilter
{
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private EntityReferenceSerializer<String> defaultSerializer;

    @Inject
    private XarObjectPropertySerializerManager propertySerializerManager;

    private XARWikiWriter wikiWriter;

    private String currentSpace;

    private String currentDocument;

    private FilterEventParameters currentDocumentParameters;

    private LocalDocumentReference currentDocumentReference;

    private String currentDocumentVersion;

    private String currentObjectClass;

    private FilterStreamXMLStreamWriter writer;

    private Map<String, String> currentObjectProperties;

    /**
     * the size of the chunks used when attachments are streamed. As the attachment data is base64 encoded in the
     * process, this size must be a multiple of three to prevent padding between the chunks; see XWIKI-9830
     */
    private static final int ATTACHMENT_BUFFER_CHUNK_SIZE = 4095;

    @Override
    public void close() throws IOException
    {
        if (this.wikiWriter != null) {
            this.wikiWriter.close();
        }

        this.properties.getTarget().close();
    }

    public String toString(Object obj)
    {
        return Objects.toString(obj, null);
    }

    public String toString(Date date)
    {
        return date != null ? String.valueOf(date.getTime()) : null;
    }

    public String toString(Syntax syntax)
    {
        return syntax != null ? syntax.toIdString() : null;
    }

    public String toString(byte[] bytes)
    {
        return Base64.encodeBase64String(bytes);
    }

    public String toString(EntityReference reference)
    {
        return this.defaultSerializer.serialize(reference);
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
        this.wikiWriter =
            new XARWikiWriter(this.properties.getPackageName() != null ? this.properties.getPackageName() : name,
                this.properties);
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
        this.currentSpace = name;
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentSpace = null;
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentDocument = name;
        this.currentDocumentParameters = parameters;

        this.currentDocumentReference = new LocalDocumentReference(this.currentSpace, this.currentDocument);
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

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        if (this.writer == null) {
            if (this.wikiWriter == null && (this.properties.isForceDocument() || isTargetTextualContent())) {
                this.writer = new FilterStreamXMLStreamWriter(this.properties, true);
            } else {
                if (this.wikiWriter == null) {
                    this.wikiWriter =
                        new XARWikiWriter(this.properties.getPackageName() != null ? this.properties.getPackageName()
                            : "package", this.properties);
                }

                this.writer =
                    new FilterStreamXMLStreamWriter(this.wikiWriter.newEntry(new LocalDocumentReference(
                        this.currentDocumentReference, locale)), this.properties.getEncoding(),
                        this.properties.isFormat(), true);
            }
        }

        this.writer.writeStartDocument(this.properties.getEncoding(), "1.0");

        this.writer.writeStartElement(XarDocumentModel.ELEMENT_DOCUMENT);
        this.writer.writeAttribute(XarDocumentModel.ATTRIBUTE_STREAMVERSION, "1.1");

        this.writer.writeElement(XarDocumentModel.ELEMENT_SPACE, this.currentSpace);
        this.writer.writeElement(XarDocumentModel.ELEMENT_NAME, this.currentDocument);

        this.writer.writeElement(XarDocumentModel.ELEMENT_LOCALE, toString(locale));
        this.writer.writeElement(XarDocumentModel.ELEMENT_DEFAULTLOCALE,
            toString(this.currentDocumentParameters.get(XWikiWikiDocumentFilter.PARAMETER_LOCALE)));
        this.writer.writeElement(XarDocumentModel.ELEMENT_ISTRANSLATION, locale != null && !Locale.ROOT.equals(locale)
            ? "1" : "0");

        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_CREATION_AUTHOR)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_CREATION_AUTHOR,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_CREATION_AUTHOR));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_CREATION_DATE)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_CREATION_DATE,
                toString((Date) parameters.get(XWikiWikiDocumentFilter.PARAMETER_CREATION_DATE)));
        }

        if (this.properties.isPreserveVersion()
            && parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_REVISIONS,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS));
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
    public void beginWikiDocumentRevision(String version, FilterEventParameters parameters) throws FilterException
    {
        this.currentDocumentVersion = version;

        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_PARENT)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_PARENT,
                toString((EntityReference) parameters.get(XWikiWikiDocumentFilter.PARAMETER_PARENT)));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_REVISION_AUTHOR)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_REVISION_AUTHOR,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_REVISION_AUTHOR));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_CUSTOMCLASS)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_CUSTOMCLASS,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_CUSTOMCLASS));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_CONTENT_AUTHOR)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_CONTENT_AUTHOR,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_CONTENT_AUTHOR));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_REVISION_DATE)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_REVISION_DATE,
                toString((Date) parameters.get(XWikiWikiDocumentFilter.PARAMETER_REVISION_DATE)));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_CONTENT_DATE)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_CONTENT_DATE,
                toString((Date) parameters.get(XWikiWikiDocumentFilter.PARAMETER_CONTENT_DATE)));
        }
        this.writer.writeElement(XarDocumentModel.ELEMENT_REVISION, this.currentDocumentVersion);
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_TITLE)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_TITLE,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_TITLE));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_DEFAULTTEMPLATE,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_VALIDATIONSCRIPT,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_REVISION_COMMENT)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_REVISION_COMMENT,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_REVISION_COMMENT));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_REVISION_MINOR)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_REVISION_MINOR,
                toString(parameters.get(XWikiWikiDocumentFilter.PARAMETER_REVISION_MINOR)));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_SYNTAX)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_SYNTAX,
                toString((Syntax) parameters.get(XWikiWikiDocumentFilter.PARAMETER_SYNTAX)));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_HIDDEN)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_HIDDEN,
                toString(parameters.get(XWikiWikiDocumentFilter.PARAMETER_HIDDEN)));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_CONTENT)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_CONTENT,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_CONTENT));
        }
        if (parameters.containsKey(XWikiWikiDocumentFilter.PARAMETER_CONTENT_HTML)) {
            this.writer.writeElement(XarDocumentModel.ELEMENT_CONTENT_HTML,
                (String) parameters.get(XWikiWikiDocumentFilter.PARAMETER_CONTENT_HTML));
        }
    }

    @Override
    public void endWikiDocumentRevision(String version, FilterEventParameters parameters) throws FilterException
    {
        this.currentDocumentVersion = null;
    }

    @Override
    public void onWikiAttachment(String name, InputStream content, Long size, FilterEventParameters parameters)
        throws FilterException
    {
        this.writer.writeStartElement(XARAttachmentModel.ELEMENT_ATTACHMENT);

        this.writer.writeElement(XARAttachmentModel.ELEMENT_NAME, name);
        if (this.properties.isPreserveVersion()
            && parameters.containsKey(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS)) {
            this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISIONS,
                (String) parameters.get(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS));
        }

        if (parameters.containsKey(XWikiWikiAttachmentFilter.PARAMETER_MIMETYPE)) {
            this.writer.writeElement(XARAttachmentModel.ELEMENT_MIMETYPE,
                (String) parameters.get(XWikiWikiAttachmentFilter.PARAMETER_MIMETYPE));
        }
        if (parameters.containsKey(XWikiWikiAttachmentFilter.PARAMETER_REVISION_AUTHOR)) {
            this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISION_AUTHOR,
                (String) parameters.get(XWikiWikiAttachmentFilter.PARAMETER_REVISION_AUTHOR));
        }
        if (parameters.containsKey(XWikiWikiAttachmentFilter.PARAMETER_REVISION_DATE)) {
            this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISION_DATE,
                toString((Date) parameters.get(XWikiWikiAttachmentFilter.PARAMETER_REVISION_DATE)));
        }
        if (parameters.containsKey(XWikiWikiAttachmentFilter.PARAMETER_REVISION)) {
            this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISION,
                (String) parameters.get(XWikiWikiAttachmentFilter.PARAMETER_REVISION));
        }
        if (parameters.containsKey(XWikiWikiAttachmentFilter.PARAMETER_REVISION_COMMENT)) {
            this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISION_COMMENT,
                (String) parameters.get(XWikiWikiAttachmentFilter.PARAMETER_REVISION_COMMENT));
        }

        if (content != null) {
            long contentSize = 0;

            this.writer.writeStartElement(XARAttachmentModel.ELEMENT_CONTENT);
            byte[] buffer = new byte[ATTACHMENT_BUFFER_CHUNK_SIZE];
            int readSize;
            do {
                try {
                    readSize = content.read(buffer, 0, ATTACHMENT_BUFFER_CHUNK_SIZE);
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
            this.writer.writeEndElement();

            this.writer.writeElement(XARAttachmentModel.ELEMENT_CONTENT_SIZE, toString(contentSize));
        }

        this.writer.writeEndElement();
    }

    @Override
    public void beginWikiClass(FilterEventParameters parameters) throws FilterException
    {
        this.writer.writeStartElement(XARClassModel.ELEMENT_CLASS);

        this.writer.writeElement(XARClassModel.ELEMENT_NAME, this.currentObjectClass != null ? this.currentObjectClass
            : this.localSerializer.serialize(this.currentDocumentReference));

        if (parameters.containsKey(WikiClassFilter.PARAMETER_CUSTOMCLASS)) {
            this.writer.writeElement(XARClassModel.ELEMENT_CUSTOMCLASS,
                (String) parameters.get(WikiClassFilter.PARAMETER_CUSTOMCLASS));
        }
        if (parameters.containsKey(WikiClassFilter.PARAMETER_CUSTOMMAPPING)) {
            this.writer.writeElement(XARClassModel.ELEMENT_CUSTOMMAPPING,
                (String) parameters.get(WikiClassFilter.PARAMETER_CUSTOMMAPPING));
        }
        if (parameters.containsKey(WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW)) {
            this.writer.writeElement(XARClassModel.ELEMENT_SHEET_DEFAULTVIEW,
                (String) parameters.get(WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW));
        }
        if (parameters.containsKey(WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT)) {
            this.writer.writeElement(XARClassModel.ELEMENT_SHEET_DEFAULTEDIT,
                (String) parameters.get(WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT));
        }
        if (parameters.containsKey(WikiClassFilter.PARAMETER_DEFAULTSPACE)) {
            this.writer.writeElement(XARClassModel.ELEMENT_DEFAULTSPACE,
                (String) parameters.get(WikiClassFilter.PARAMETER_DEFAULTSPACE));
        }
        if (parameters.containsKey(WikiClassFilter.PARAMETER_NAMEFIELD)) {
            this.writer.writeElement(XARClassModel.ELEMENT_NAMEFIELD,
                (String) parameters.get(WikiClassFilter.PARAMETER_NAMEFIELD));
        }
        if (parameters.containsKey(WikiClassFilter.PARAMETER_VALIDATIONSCRIPT)) {
            this.writer.writeElement(XARClassModel.ELEMENT_VALIDATIONSCRIPT,
                (String) parameters.get(WikiClassFilter.PARAMETER_VALIDATIONSCRIPT));
        }
    }

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws FilterException
    {
        this.writer.writeEndElement();
    }

    @Override
    public void beginWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws FilterException
    {
        this.writer.writeStartElement(name);

        if (this.currentObjectProperties != null) {
            this.currentObjectProperties.put(name, type);
        }
    }

    @Override
    public void endWikiClassProperty(String name, String type, FilterEventParameters parameters) throws FilterException
    {
        this.writer.writeElement(XARClassPropertyModel.ELEMENT_CLASSTYPE, type);

        this.writer.writeEndElement();
    }

    @Override
    public void onWikiClassPropertyField(String name, String value, FilterEventParameters parameters)
        throws FilterException
    {
        this.writer.writeElement(name, value);
    }

    @Override
    public void beginWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        this.writer.writeStartElement(XARObjectModel.ELEMENT_OBJECT);

        this.currentObjectClass = (String) parameters.get(WikiObjectFilter.PARAMETER_CLASS_REFERENCE);

        this.writer.writeElement(XARObjectModel.ELEMENT_NAME,
            this.localSerializer.serialize(this.currentDocumentReference));
        this.writer.writeElement(XARObjectModel.ELEMENT_NUMBER,
            toString((Integer) parameters.get(WikiObjectFilter.PARAMETER_NUMBER)));
        this.writer.writeElement(XARObjectModel.ELEMENT_CLASSNAME, this.currentObjectClass);

        if (parameters.containsKey(WikiObjectFilter.PARAMETER_GUID)) {
            this.writer.writeElement(XARObjectModel.ELEMENT_GUID,
                (String) parameters.get(WikiObjectFilter.PARAMETER_GUID));
        }

        this.currentObjectProperties = new HashMap<String, String>();
    }

    @Override
    public void endWikiObject(String name, FilterEventParameters parameters) throws FilterException
    {
        this.writer.writeEndElement();

        this.currentObjectClass = null;
        this.currentObjectProperties = null;
    }

    @Override
    public void onWikiObjectProperty(String name, Object value, FilterEventParameters parameters)
        throws FilterException
    {
        this.writer.writeStartElement(XARObjectPropertyModel.ELEMENT_PROPERTY);

        this.writer.writeStartElement(name);

        try {
            this.propertySerializerManager.getPropertySerializer(this.currentObjectProperties.get(name)).write(
                this.writer.getWriter(), value);
        } catch (Exception e) {
            throw new FilterException("Failed to write property value", e);
        }

        this.writer.writeEndElement();

        this.writer.writeEndElement();
    }
}
