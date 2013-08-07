package org.xwiki.wikistream.xar.internal.output;

import java.io.InputStream;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.WikiDocumentFilter;
import org.xwiki.wikistream.internal.PropertiesTree;
import org.xwiki.wikistream.internal.output.AbstractBeanOutputWikiStream;
import org.xwiki.wikistream.xar.internal.XARClassModel;
import org.xwiki.wikistream.xar.internal.XARDocumentModel;
import org.xwiki.wikistream.xar.internal.XARFilter;
import org.xwiki.wikistream.xar.internal.XARObjectModel;
import org.xwiki.wikistream.xml.internal.output.WikiStreamXMLStreamWriter;

public class XAROutputWikiStream extends AbstractBeanOutputWikiStream<XAROutputProperties> implements XARFilter
{
    private XARWikiWriter wikiWriter;

    private PropertiesTree currentProperties;

    private String currentSpace;

    private String currentDocument;

    private PropertiesTree currentDocumentProperties;

    private Locale currentLocale;

    private String currentVersion;

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

    public String toString(Boolean bool)
    {
        return bool != null ? (bool ? "1" : "0") : null;
    }

    public WikiStreamXMLStreamWriter getWriter() throws WikiStreamException
    {
        if (this.writer == null) {
            if (this.wikiWriter != null) {
                this.writer =
                    new WikiStreamXMLStreamWriter(this.wikiWriter.newEntry(this.currentSpace, this.currentDocument,
                        this.currentLocale), this.properties.getEncoding());
            } else {
                this.writer = new WikiStreamXMLStreamWriter(this.properties);
            }

            this.writer.writeStartElement(XARDocumentModel.ELEMENT_DOCUMENT);
            this.writer.writeElement(XARDocumentModel.ELEMENT_SPACE, this.currentSpace);
            this.writer.writeElement(XARDocumentModel.ELEMENT_NAME, this.currentDocument);

            this.writer.writeElement(XARDocumentModel.ELEMENT_LOCALE, toString(this.currentLocale));
            this.writer.writeElement(XARDocumentModel.ELEMENT_DEFAULTLOCALE,
                toString(this.currentDocumentProperties.get(WikiDocumentFilter.PROPERTY_LOCALE)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_ISTRANSLATION, this.currentLocale != null ? "1" : "0");
            this.writer.writeElement(XARDocumentModel.ELEMENT_PARENT,
                this.currentProperties.<String> get(WikiDocumentFilter.PROPERTY_PARENT));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CREATION_AUTHOR,
                this.currentProperties.<String> get(WikiDocumentFilter.PROPERTY_CREATION_AUTHOR));
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISION_AUTHOR,
                this.currentProperties.<String> get(WikiDocumentFilter.PROPERTY_REVISION_AUTHOR));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CUSTOMCLASS,
                this.currentProperties.<String> get("xwiki.document.customclass"));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CONTENT_AUTHOR,
                this.currentProperties.<String> get(WikiDocumentFilter.PROPERTY_CONTENT_AUTHOR));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CREATION_DATE,
                toString(this.currentProperties.<Date> get(WikiDocumentFilter.PROPERTY_CREATION_DATE)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISION_DATE,
                toString(this.currentProperties.<Date> get(WikiDocumentFilter.PROPERTY_REVISION_DATE)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CONTENT_DATE,
                toString(this.currentProperties.<Date> get(WikiDocumentFilter.PROPERTY_CONTENT_DATE)));            
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISION_VERSION, this.currentVersion);
            this.writer.writeElement(XARDocumentModel.ELEMENT_TITLE,
                this.currentProperties.<String> get(WikiDocumentFilter.PROPERTY_TITLE));
            this.writer.writeElement(XARDocumentModel.ELEMENT_DEFAULTTEMPLATE,
                this.currentProperties.<String> get("xwiki.document.defaulttemplate"));
            this.writer.writeElement(XARDocumentModel.ELEMENT_VALIDATIONSCRIPT,
                this.currentProperties.<String> get("xwiki.document.validationscript"));
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISION_COMMENT,
                this.currentProperties.<String> get(WikiDocumentFilter.PROPERTY_REVISION_COMMENT));
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISION_MINOR,
                toString(this.currentProperties.<Boolean> get(WikiDocumentFilter.PROPERTY_REVISION_MINOR)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_CONTENT_SYNTAX,
                toString(this.currentProperties.<Syntax> get(WikiDocumentFilter.PROPERTY_CONTENT_SYNTAX)));            
            this.writer.writeElement(XARDocumentModel.ELEMENT_ISHIDDEN,
                toString(this.currentProperties.<Boolean> get(WikiDocumentFilter.PROPERTY_HIDDEN)));

            // TODO: move the end ?
            this.writer.writeElement(XARDocumentModel.ELEMENT_CONTENT,
                toString(this.currentProperties.<String> get(WikiDocumentFilter.PROPERTY_CONTENT)));
            this.writer.writeElement(XARDocumentModel.ELEMENT_REVISIONS,
                this.currentProperties.<String> get("xwiki.document.jrcsrevisions"));
        }

        return this.writer;
    }

    // events

    @Override
    public void beginWikiFarm(Map<String, Object> properties) throws WikiStreamException
    {
        this.currentProperties = new PropertiesTree(properties, this.currentProperties);
    }

    @Override
    public void endWikiFarm(Map<String, Object> properties) throws WikiStreamException
    {
        this.currentProperties = this.currentProperties.getParent();
    }

    @Override
    public void beginWiki(String name, Map<String, Object> properties) throws WikiStreamException
    {
        this.wikiWriter = new XARWikiWriter(name, properties, this.properties);

        this.currentProperties = new PropertiesTree(properties, this.currentProperties);
    }

    @Override
    public void endWiki(String name, Map<String, Object> properties) throws WikiStreamException
    {
        this.wikiWriter.close();

        this.wikiWriter = null;
        this.currentProperties = this.currentProperties.getParent();
    }

    @Override
    public void beginWikiSpace(String name, Map<String, Object> properties) throws WikiStreamException
    {
        if (this.currentSpace != null) {
            throw new WikiStreamException("XAR format supports only one of space");
        }

        this.currentSpace = name;
        this.currentProperties = new PropertiesTree(properties, this.currentProperties);
    }

    @Override
    public void endWikiSpace(String name, Map<String, Object> properties) throws WikiStreamException
    {
        this.currentSpace = null;
        this.currentProperties = this.currentProperties.getParent();
    }

    @Override
    public void beginWikiDocument(String name, Map<String, Object> properties) throws WikiStreamException
    {
        this.currentDocument = name;
        this.currentDocumentProperties = new PropertiesTree(properties, this.currentProperties);
        this.currentProperties = this.currentDocumentProperties;
    }

    @Override
    public void endWikiDocument(String name, Map<String, Object> properties) throws WikiStreamException
    {
        if (this.writer != null) {
            getWriter().writeEndElement();
        }

        this.writer = null;
        this.currentDocument = null;
        this.currentProperties = this.currentProperties.getParent();
        this.currentDocumentProperties = null;
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, Map<String, Object> properties) throws WikiStreamException
    {
        this.currentLocale = locale;
        this.currentProperties = new PropertiesTree(properties, this.currentProperties);
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, Map<String, Object> properties) throws WikiStreamException
    {
        getWriter().writeEndElement();

        this.writer = null;
        this.currentLocale = null;
        this.currentProperties = this.currentProperties.getParent();
    }

    @Override
    public void beginWikiDocumentRevision(String version, Map<String, Object> properties) throws WikiStreamException
    {
        this.currentVersion = version;
        this.currentProperties = new PropertiesTree(properties, this.currentProperties);
    }

    @Override
    public void endWikiDocumentRevision(String version, Map<String, Object> properties) throws WikiStreamException
    {
        this.currentVersion = null;
        this.currentProperties = this.currentProperties.getParent();
    }

    @Override
    public void beginAttachment(String attachmentName, Map<String, Object> properties) throws WikiStreamException
    {

    }

    @Override
    public void onAttachmentContent(byte[] content, Map<String, Object> properties) throws WikiStreamException
    {

    }

    @Override
    public void onAttachmentContent(InputStream content, Map<String, Object> properties) throws WikiStreamException
    {

    }

    @Override
    public void endAttachment(String attachmentName, Map<String, Object> properties) throws WikiStreamException
    {

    }

    @Override
    public void beginWikiClass(Map<String, Object> properties) throws WikiStreamException
    {
        getWriter().writeStartElement(XARClassModel.ELEMENT_CLASS);
    }

    @Override
    public void endWikiClass(Map<String, Object> properties) throws WikiStreamException
    {
        getWriter().writeEndElement();
    }

    @Override
    public void beginWikiClassProperty(String propertyName, Map<String, Object> properties) throws WikiStreamException
    {
        getWriter().writeStartElement(propertyName);
    }

    @Override
    public void endWikiClassProperty(String propertyName, Map<String, Object> properties) throws WikiStreamException
    {
        getWriter().writeEndElement();
    }

    @Override
    public void beginWikiObject(String name, Map<String, Object> properties) throws WikiStreamException
    {
        getWriter().writeStartElement(XARObjectModel.ELEMENT_OBJECT);
    }

    @Override
    public void endWikiObject(String name, Map<String, Object> properties) throws WikiStreamException
    {
        getWriter().writeEndElement();
    }

    @Override
    public void beginWikiObjectProperty(String propertyName, Map<String, Object> properties) throws WikiStreamException
    {
        getWriter().writeStartElement(XARObjectModel.ELEMENT_PROPERTY);
    }

    @Override
    public void endWikiObjectProperty(String propertyName, Map<String, Object> properties) throws WikiStreamException
    {
        getWriter().writeEndElement();
    }
}
