package org.xwiki.wikistream.xar.internal.output;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ObjectUtils;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.model.internal.reference.LocalizedStringEntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.WikiClassFilter;
import org.xwiki.wikistream.filter.WikiClassPropertyFilter;
import org.xwiki.wikistream.filter.WikiObjectFilter;
import org.xwiki.wikistream.internal.ParametersTree;
import org.xwiki.wikistream.internal.output.AbstractBeanOutputWikiStream;
import org.xwiki.wikistream.xar.internal.XARAttachmentModel;
import org.xwiki.wikistream.xar.internal.XARClassModel;
import org.xwiki.wikistream.xar.internal.XARClassPropertyModel;
import org.xwiki.wikistream.xar.internal.XARDocumentModel;
import org.xwiki.wikistream.xar.internal.XARFilter;
import org.xwiki.wikistream.xar.internal.XARObjectModel;
import org.xwiki.wikistream.xar.internal.XARObjectPropertyModel;
import org.xwiki.wikistream.xml.internal.output.WikiStreamXMLStreamWriter;
import org.xwiki.wikistream.xwiki.filter.XWikiWikiAttachmentFilter;
import org.xwiki.wikistream.xwiki.filter.XWikiWikiDocumentFilter;

public class XAROutputWikiStream extends AbstractBeanOutputWikiStream<XAROutputProperties> implements XARFilter
{
    private static final LocalizedStringEntityReferenceSerializer TOSTRING_SERIALIZER =
        new LocalizedStringEntityReferenceSerializer();

    private XARWikiWriter wikiWriter;

    private ParametersTree currentParameters;

    private String currentSpace;

    private String currentDocument;

    private LocalDocumentReference currentDocumentReference;

    private ParametersTree currentDocumentParameters;

    private Locale currentDocumentLocale = Locale.ROOT;

    private String currentDocumentVersion;

    private String currentAttachment;

    private String currentAttachmentVersion;

    private String currentObjectClass;

    private WikiStreamXMLStreamWriter writer;

    public XAROutputWikiStream(XAROutputProperties properties)
    {
        super(properties);
    }

    public String toString(Object obj)
    {
        return ObjectUtils.toString(obj, null);
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

    public WikiStreamXMLStreamWriter getWriter() throws WikiStreamException
    {
        if (this.writer == null) {
            if (this.wikiWriter != null) {
                this.writer =
                    new WikiStreamXMLStreamWriter(this.wikiWriter.newEntry(this.currentDocumentReference,
                        this.currentDocumentLocale), this.properties.getEncoding());
            } else {
                this.writer = new WikiStreamXMLStreamWriter(this.properties);
            }

            this.writer.writeStartElement(XARDocumentModel.ELEMENT_DOCUMENT);
            this.writer.writeElement(XARDocumentModel.ELEMENT_SPACE, this.currentSpace);
            this.writer.writeElement(XARDocumentModel.ELEMENT_NAME, this.currentDocument);

            this.writer.writeElement(XARDocumentModel.ELEMENT_LOCALE, toString(this.currentDocumentLocale));
            this.writer.writeElement(XARDocumentModel.ELEMENT_DEFAULTLOCALE,
                toString(this.currentDocumentParameters.get(XWikiWikiDocumentFilter.PARAMETER_LOCALE)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_ISTRANSLATION, this.currentDocumentLocale != null
                && !Locale.ROOT.equals(this.currentDocumentLocale) ? "1" : "0");
            this.writer.writeElement(XARDocumentModel.ELEMENT_PARENT,
                this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_PARENT));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CREATION_AUTHOR,
                this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_CREATION_AUTHOR));
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISION_AUTHOR,
                this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_REVISION_AUTHOR));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CUSTOMCLASS,
                this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_CUSTOMCLASS));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CONTENT_AUTHOR,
                this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_CONTENT_AUTHOR));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CREATION_DATE,
                toString(this.currentParameters.<Date> get(XWikiWikiDocumentFilter.PARAMETER_CREATION_DATE)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISION_DATE,
                toString(this.currentParameters.<Date> get(XWikiWikiDocumentFilter.PARAMETER_REVISION_DATE)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CONTENT_DATE,
                toString(this.currentParameters.<Date> get(XWikiWikiDocumentFilter.PARAMETER_CONTENT_DATE)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISION_VERSION, this.currentDocumentVersion);
            this.writer.writeElement(XARDocumentModel.ELEMENT_TITLE,
                this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_TITLE));
            this.writer.writeElement(XARDocumentModel.ELEMENT_DEFAULTTEMPLATE,
                this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_DEFAULTTEMPLATE));
            this.writer.writeElement(XARDocumentModel.ELEMENT_VALIDATIONSCRIPT,
                this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_VALIDATIONSCRIPT));
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISION_COMMENT,
                this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_REVISION_COMMENT));
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISION_MINOR,
                toString(this.currentParameters.get(XWikiWikiDocumentFilter.PARAMETER_REVISION_MINOR)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CONTENT_SYNTAX,
                toString(this.currentParameters.<Syntax> get(XWikiWikiDocumentFilter.PARAMETER_SYNTAX)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_ISHIDDEN,
                toString(this.currentParameters.get(XWikiWikiDocumentFilter.PARAMETER_HIDDEN)));
        }

        return this.writer;
    }

    // events

    @Override
    public void beginWikiFarm(FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endWikiFarm(FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.wikiWriter = new XARWikiWriter(name, parameters, this.properties);

        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.wikiWriter.close();

        this.wikiWriter = null;
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        if (this.currentSpace != null) {
            throw new WikiStreamException("XAR format supports only one of space");
        }

        this.currentSpace = name;
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentSpace = null;
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentDocument = name;
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
        this.currentDocumentParameters = this.currentParameters;

        this.currentDocumentReference = new LocalDocumentReference(this.currentSpace, this.currentDocument);
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentDocument = null;
        this.currentDocumentReference = null;
        this.currentParameters = this.currentParameters.getParent();
        this.currentDocumentParameters = null;
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentDocumentLocale = locale;
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws WikiStreamException
    {
        getWriter().writeElement(XARDocumentModel.ELEMENT_REVISIONS,
            this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_JRCSREVISIONS));

        getWriter().writeEndElement();

        this.writer = null;
        this.currentDocumentLocale = null;
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWikiDocumentRevision(String version, FilterEventParameters parameters)
        throws WikiStreamException
    {
        this.currentDocumentVersion = version;
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endWikiDocumentRevision(String version, FilterEventParameters parameters) throws WikiStreamException
    {
        getWriter().writeElement(XARDocumentModel.ELEMENT_CONTENT,
            this.currentParameters.<String> get(XWikiWikiDocumentFilter.PARAMETER_CONTENT));

        this.currentDocumentVersion = null;
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWikiAttachment(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        getWriter().writeStartElement(XARAttachmentModel.ELEMENT_ATTACHMENT);

        this.currentAttachment = name;
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endWikiAttachment(String attachmentName, FilterEventParameters parameters) throws WikiStreamException
    {
        this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISIONS,
            this.currentParameters.<String> get(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS));

        if (this.writer != null) {
            getWriter().writeEndElement();
        }

        this.currentAttachment = null;
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWikiAttachmentRevision(String version, FilterEventParameters parameters)
        throws WikiStreamException
    {
        this.currentAttachmentVersion = version;
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);
    }

    @Override
    public void endWikiAttachmentRevision(String version, FilterEventParameters parameters)
        throws WikiStreamException
    {
        this.writer.writeElement(XARAttachmentModel.ELEMENT_NAME, this.currentAttachment);
        this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISION_AUTHOR,
            this.currentParameters.<String> get(XWikiWikiAttachmentFilter.PARAMETER_REVISION_AUTHOR));
        this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISION_DATE,
            toString(this.currentParameters.<Date> get(XWikiWikiAttachmentFilter.PARAMETER_REVISION_DATE)));
        this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISION_VERSION, this.currentAttachmentVersion);
        this.writer.writeElement(XARAttachmentModel.ELEMENT_REVISION_COMMENT,
            this.currentParameters.<String> get(XWikiWikiAttachmentFilter.PARAMETER_REVISION_COMMENT));

        byte[] content = this.currentParameters.<byte[]> get(XWikiWikiAttachmentFilter.PARAMETER_CONTENT);
        if (content != null) {
            this.writer.writeElement(XARAttachmentModel.ELEMENT_CONTENT, toString(content));
            this.writer.writeElement(XARAttachmentModel.ELEMENT_CONTENT_SIZE, toString(content.length));
        }

        this.currentAttachmentVersion = null;
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWikiClass(FilterEventParameters parameters) throws WikiStreamException
    {
        getWriter().writeStartElement(XARClassModel.ELEMENT_CLASS);

        this.currentParameters = new ParametersTree(parameters, this.currentParameters);

        this.writer.writeElement(XARClassModel.ELEMENT_NAME, this.currentObjectClass != null ? this.currentObjectClass
            : TOSTRING_SERIALIZER.serialize(this.currentDocumentReference));

        this.writer.writeElement(XARClassModel.ELEMENT_CUSTOMCLASS,
            this.currentParameters.<String> get(WikiClassFilter.PARAMETER_CUSTOMCLASS));
        this.writer.writeElement(XARClassModel.ELEMENT_CUSTOMMAPPING,
            this.currentParameters.<String> get(WikiClassFilter.PARAMETER_CUSTOMMAPPING));
        this.writer.writeElement(XARClassModel.ELEMENT_DEFAULTVIEWSHEET,
            this.currentParameters.<String> get(WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW));
        this.writer.writeElement(XARClassModel.ELEMENT_DEFAULTEDITSHEET,
            this.currentParameters.<String> get(WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT));
        this.writer.writeElement(XARClassModel.ELEMENT_DEFAULTWEB,
            this.currentParameters.<String> get(WikiClassFilter.PARAMETER_DEFAULTSPACE));
        this.writer.writeElement(XARClassModel.ELEMENT_NAMEFIELD,
            this.currentParameters.<String> get(WikiClassFilter.PARAMETER_NAMEFIELD));
        this.writer.writeElement(XARClassModel.ELEMENT_VALIDATIONSCRIPT,
            this.currentParameters.<String> get(WikiClassFilter.PARAMETER_VALIDATIONSCRIPT));
    }

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws WikiStreamException
    {
        getWriter().writeEndElement();

        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws WikiStreamException
    {
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);

        getWriter().writeStartElement(name);

        Map<String, String> fields =
            this.currentParameters.<Map<String, String>> get(WikiClassPropertyFilter.PARAMETER_FIELDS);
        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                this.writer.writeElement(entry.getKey(), entry.getValue());
            }
        }

        this.writer.writeElement(XARClassPropertyModel.ELEMENT_CLASSTYPE, type);
    }

    @Override
    public void endWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws WikiStreamException
    {
        getWriter().writeEndElement();

        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWikiObject(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);

        getWriter().writeStartElement(XARObjectModel.ELEMENT_OBJECT);

        this.currentObjectClass = this.currentParameters.<String> get(WikiObjectFilter.PARAMETER_CLASS_REFERENCE);

        this.writer.writeElement(XARObjectModel.ELEMENT_NAME,
            TOSTRING_SERIALIZER.serialize(this.currentDocumentReference));
        this.writer.writeElement(XARObjectModel.ELEMENT_NUMBER,
            toString(this.currentParameters.<Integer> get(WikiObjectFilter.PARAMETER_NUMBER)));
        this.writer.writeElement(XARObjectModel.ELEMENT_CLASSNAME, this.currentObjectClass);
        this.writer.writeElement(XARObjectModel.ELEMENT_GUID,
            this.currentParameters.<String> get(WikiObjectFilter.PARAMETER_GUID));
    }

    @Override
    public void endWikiObject(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        getWriter().writeEndElement();

        this.currentObjectClass = null;
        this.currentParameters = this.currentParameters.getParent();
    }

    @Override
    public void beginWikiObjectProperty(String name, String value, FilterEventParameters parameters)
        throws WikiStreamException
    {
        this.currentParameters = new ParametersTree(parameters, this.currentParameters);

        getWriter().writeStartElement(XARObjectPropertyModel.ELEMENT_PROPERTY);

        this.writer.writeElement(name, value);
    }

    @Override
    public void endWikiObjectProperty(String name, String value, FilterEventParameters parameters)
        throws WikiStreamException
    {
        getWriter().writeEndElement();

        this.currentParameters = this.currentParameters.getParent();
    }
}
