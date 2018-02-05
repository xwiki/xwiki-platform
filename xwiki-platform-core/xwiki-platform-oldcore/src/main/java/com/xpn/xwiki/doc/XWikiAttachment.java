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
package com.xpn.xwiki.doc;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.dom4j.Element;
import org.dom4j.io.DocumentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.StringInputSource;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.output.DefaultWriterOutputTarget;
import org.xwiki.filter.output.OutputTarget;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.filter.xml.output.DefaultResultOutputTarget;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.tika.internal.TikaUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilterUtils;
import com.xpn.xwiki.internal.xml.XMLWriter;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

public class XWikiAttachment implements Cloneable
{
    public static interface AttachmentNameChanged
    {
        void onAttachmentNameModified(String previousAttachmentName, XWikiAttachment attachment);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiAttachment.class);

    /**
     * Used to convert a Document Reference to string (compact form without the wiki part if it matches the current
     * wiki).
     */
    private static EntityReferenceSerializer<String> getCompactWikiEntityReferenceSerializer()
    {
        return Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
    }

    private static EntityReferenceResolver<String> getXClassEntityReferenceResolver()
    {
        return Utils.getComponent(EntityReferenceResolver.TYPE_STRING, "xclass");
    }

    /**
     * Used to normalize references.
     */
    private DocumentReferenceResolver<EntityReference> getExplicitReferenceDocumentReferenceResolver()
    {
        return Utils.getComponent(DocumentReferenceResolver.TYPE_REFERENCE, "explicit");
    }

    private AttachmentReferenceResolver<String> getCurentAttachmentReferenceResolver()
    {
        return Utils.getComponent(AttachmentReferenceResolver.TYPE_STRING, "current");
    }

    private XWikiDocument doc;

    private long size;

    private String mimeType;

    private String filename;

    private String author;

    private DocumentReference authorReference;

    private Version version;

    private String comment;

    private Date date;

    private String contentStore;

    private boolean contentStoreSet;

    private XWikiAttachmentStoreInterface contentStoreInstance;

    private String archiveStore;

    private boolean archiveStoreSet;

    private AttachmentVersioningStore archiveStoreInstance;

    private XWikiAttachmentContent content;

    private XWikiAttachmentArchive attachment_archive;

    private boolean isMetaDataDirty = false;

    private AttachmentReference reference;

    private boolean forceSetFilesize;

    private List<AttachmentNameChanged> listeners = new CopyOnWriteArrayList<>();

    public XWikiAttachment(XWikiDocument doc, String filename)
    {
        this();

        setDoc(doc);
        setFilename(filename);

        // We know it's not Hibernate
        this.forceSetFilesize = true;
    }

    public XWikiAttachment()
    {
        this.size = 0;
        this.filename = "";
        this.comment = "";
        this.date = new Date();

        // It might be Hibernate
        this.forceSetFilesize = false;
    }

    public AttachmentReference getReference()
    {
        if (this.reference == null) {
            if (this.doc != null) {
                this.reference = new AttachmentReference(this.filename, this.doc.getDocumentReference());
            } else {
                // Try with current
                return getCurentAttachmentReferenceResolver().resolve(this.filename);
            }
        }

        return this.reference;
    }

    public long getId()
    {
        if (this.doc == null) {
            return this.filename.hashCode();
        } else {
            return (this.doc.getFullName() + "/" + this.filename).hashCode();
        }
    }

    public void setDocId(long id)
    {
    }

    public long getDocId()
    {
        return this.doc.getId();
    }

    public void setId(long id)
    {
    }

    @Override
    public XWikiAttachment clone()
    {
        XWikiAttachment attachment = null;

        try {
            attachment = (XWikiAttachment) super.clone();

            attachment.setComment(getComment());
            attachment.setDate(getDate());
            attachment.setFilename(getFilename());
            attachment.setMimeType(getMimeType());
            attachment.setLongSize(getLongSize());
            attachment.setRCSVersion(getRCSVersion());
            attachment.setMetaDataDirty(isMetaDataDirty());
            if (getAttachment_content() != null) {
                attachment.setAttachment_content((XWikiAttachmentContent) getAttachment_content().clone());
                attachment.getAttachment_content().setAttachment(attachment);
            }
            if (getAttachment_archive() != null) {
                attachment.setAttachment_archive((XWikiAttachmentArchive) getAttachment_archive().clone());
                attachment.getAttachment_archive().setAttachment(attachment);
            }
        } catch (CloneNotSupportedException e) {
            // This should not happen
            LOGGER.error("exception while attach.clone", e);
        }

        return attachment;
    }

    /**
     * @return the number of bytes in this attachment content
     * @deprecated since 9.0RC1, use {@link #getLongSize()} instead
     */
    @Deprecated
    public int getFilesize()
    {
        long longSize = getLongSize();

        return longSize > (long) Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) longSize;
    }

    /**
     * Set cached filesize of the attachment that will be stored as metadata.
     *
     * @param filesize the number of bytes in this attachment content
     * @deprecated since 9.0RC1, use {@link #setLongSize(long)} instead
     */
    @Deprecated
    public void setFilesize(int filesize)
    {
        // There is no way to tell Hibernate to not call #setFilesize and we don't want to break the size if it's bigger
        // than an int (#setFilesize is usually called after setLongSize by Hibernate)
        if (filesize >= 0 || filesize < Integer.MAX_VALUE || this.forceSetFilesize) {
            setLongSize(filesize);
        }
    }

    /**
     * @return the metadata holding the number of bytes in this attachment content
     * @since 9.0RC1
     */
    public long getLongSize()
    {
        return this.size;
    }

    /**
     * The size is automatically calculated from the attachment content so this method is mostly internal API that
     * should not be used.
     * 
     * @param size the metadata holding the number of bytes in this attachment content
     * @since 9.0RC1
     */
    public void setLongSize(long size)
    {
        if (size != this.size) {
            setMetaDataDirty(true);
        }

        this.size = size;
    }

    /**
     * @param context current XWikiContext
     * @return the real filesize in byte of the attachment. We cannot trust the metadata that may be publicly changed.
     * @throws XWikiException
     * @since 2.3M2
     * @deprecated since 9.0RC1, use {@link #getContentLongSize(XWikiContext)} instead
     */
    @Deprecated
    public int getContentSize(XWikiContext context) throws XWikiException
    {
        long longSize = getContentLongSize(context);

        return longSize > (long) Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) longSize;
    }

    /**
     * @param context current XWikiContext
     * @return the real filesize in byte of the attachment. We cannot trust the metadata that may be publicly changed.
     * @throws XWikiException
     * @since 9.0RC1
     */
    public long getContentLongSize(XWikiContext context) throws XWikiException
    {
        if (this.content == null && context != null) {
            loadAttachmentContent(context);
        }

        return this.content.getLongSize();
    }

    public String getFilename()
    {
        return this.filename;
    }

    public void setFilename(String filename)
    {
        if (ObjectUtils.notEqual(getFilename(), filename)) {
            setMetaDataDirty(true);

            String previousFileName = this.getFilename();

            this.filename = filename;

            notificateNameModifed(previousFileName, this);
        }

        this.reference = null;
    }

    /**
     * @since 6.4M1
     */
    public DocumentReference getAuthorReference()
    {
        if (this.authorReference == null) {
            if (this.doc != null) {
                this.authorReference = userStringToReference(this.author);
            } else {
                // Don't store the reference when generated based on context (it might become wrong when actually
                // setting the document)
                return userStringToReference(this.author);
            }
        }

        return this.authorReference;
    }

    /**
     * @since 6.4M1
     */
    public void setAuthorReference(DocumentReference authorReference)
    {
        if (ObjectUtils.notEqual(authorReference, getAuthorReference())) {
            setMetaDataDirty(true);
        }

        this.authorReference = authorReference;
        this.author = null;

        // Log this since it's probably a mistake so that we find who is doing bad things
        if (this.authorReference != null && this.authorReference.getName().equals(XWikiRightService.GUEST_USER)) {
            LOGGER.warn("A reference to XWikiGuest user has been set instead of null. This is probably a mistake.",
                new Exception("See stack trace"));
        }
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     *
     * @deprecated since 6.4M1 use {@link #getAuthorReference()} instead
     */
    @Deprecated
    public String getAuthor()
    {
        if (this.author == null) {
            this.author = userReferenceToString(getAuthorReference());
        }

        return this.author != null ? this.author : "";
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for loading a XWikiDocument.
     *
     * @deprecated since 6.4M1 use {@link #setAuthorReference} instead
     */
    @Deprecated
    public void setAuthor(String author)
    {
        if (!Objects.equals(getAuthor(), author)) {
            this.author = author;
            this.authorReference = null;

            setMetaDataDirty(true);
        }
    }

    public String getVersion()
    {
        return getRCSVersion().toString();
    }

    public void setVersion(String version)
    {
        this.version = new Version(version);
    }

    public String getNextVersion()
    {
        if (this.version == null) {
            return "1.1";
        } else {
            return ((Version) this.version.clone()).next().toString();
        }
    }

    public Version getRCSVersion()
    {
        if (this.version == null) {
            return new Version("1.1");
        }

        return (Version) this.version.clone();
    }

    public void setRCSVersion(Version version)
    {
        this.version = version;
    }

    public String getComment()
    {
        return this.comment != null ? this.comment : "";
    }

    public void setComment(String comment)
    {
        if (ObjectUtils.notEqual(getComment(), comment)) {
            setMetaDataDirty(true);
            this.comment = comment;
        }
    }

    public XWikiDocument getDoc()
    {
        return this.doc;
    }

    public void setDoc(XWikiDocument doc)
    {
        setDoc(doc, true);
    }

    /**
     * @param doc the document to associate to the attachment
     * @param updateDirty false if the document metadata dirty flag should not be modified
     * @since 9.10RC1
     */
    public void setDoc(XWikiDocument doc, boolean updateDirty)
    {
        if (this.doc != doc) {
            this.doc = doc;
            this.reference = null;

            if (updateDirty) {
                if (isMetaDataDirty() && doc != null) {
                    doc.setMetaDataDirty(true);
                }
                if (getAttachment_content() != null) {
                    getAttachment_content().setOwnerDocument(doc);
                }
            }
        }
    }

    public Date getDate()
    {
        return this.date;
    }

    public void setDate(Date date)
    {
        // Make sure we drop milliseconds for consistency with the database
        if (date != null) {
            date.setTime((date.getTime() / 1000) * 1000);
        }

        this.date = date;
    }

    public boolean isContentDirty()
    {
        if (this.content == null) {
            return false;
        } else {
            return this.content.isContentDirty();
        }
    }

    public void incrementVersion()
    {
        if (this.version == null) {
            this.version = new Version("1.1");
        } else {
            this.version = this.version.next();
        }
    }

    public boolean isMetaDataDirty()
    {
        return this.isMetaDataDirty;
    }

    public void setMetaDataDirty(boolean metaDataDirty)
    {
        this.isMetaDataDirty = metaDataDirty;
        if (metaDataDirty && this.doc != null) {
            this.doc.setMetaDataDirty(true);
        }
    }

    /**
     * Retrieve an attachment as an XML string. You should prefer
     * {@link #toXML(OutputTarget, boolean, boolean, boolean, XWikiContext)} to avoid memory loads when appropriate.
     *
     * @param bWithAttachmentContent if true, binary content of the attachment is included (base64 encoded)
     * @param bWithVersions if true, all archived versions are also included
     * @param context current XWikiContext
     * @return a string containing an XML representation of the attachment
     * @throws XWikiException when an error occurs during wiki operations
     */
    public String toStringXML(boolean bWithAttachmentContent, boolean bWithVersions, XWikiContext context)
        throws XWikiException
    {
        try {
            StringWriter writer = new StringWriter();
            toXML(new DefaultWriterOutputTarget(writer), bWithAttachmentContent, bWithVersions, true, context);

            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Failed to write attachment XML", e);

            return "";
        }
    }

    /**
     * Retrieve an attachment as an XML string. You should prefer
     * {@link #toXML(OutputTarget, boolean, boolean, boolean, String)} to avoid memory loads when appropriate.
     *
     * @return a string containing an XML representation of the attachment
     * @throws XWikiException when an error occurs during wiki operations
     * @since 9.10RC1
     */
    public String toXML() throws XWikiException
    {
        StringWriter writer = new StringWriter();
        toXML(new DefaultWriterOutputTarget(writer), true, true, true, StandardCharsets.UTF_8.name());

        return writer.toString();
    }

    /**
     * Retrieve XML representation of attachment's metadata into an {@link Element}.
     *
     * @return a {@link Element} containing an XML representation of the attachment without content
     * @throws XWikiException when an error occurs during wiki operations
     */
    public Element toXML(XWikiContext context) throws XWikiException
    {
        return toXML(false, false, context);
    }

    /**
     * Write an XML representation of the attachment into an {@link com.xpn.xwiki.internal.xml.XMLWriter}
     *
     * @param wr the XMLWriter to write to
     * @param bWithAttachmentContent if true, binary content of the attachment is included (base64 encoded)
     * @param bWithVersions if true, all archive version is also included
     * @param context current XWikiContext
     * @throws IOException when an error occurs during streaming operation
     * @throws XWikiException when an error occurs during xwiki operation
     * @since 2.3M2
     */
    public void toXML(XMLWriter wr, boolean bWithAttachmentContent, boolean bWithVersions, XWikiContext context)
        throws IOException, XWikiException
    {
        // IMPORTANT: we don't use directly XMLWriter's SAX apis here because it's not really working well
        DocumentResult domResult = new DocumentResult();

        toXML(new DefaultResultOutputTarget(domResult), bWithAttachmentContent, bWithVersions, true, context);

        wr.write(domResult.getDocument().getRootElement());
    }

    /**
     * Write an XML representation of the attachment into an {@link com.xpn.xwiki.internal.xml.XMLWriter}
     *
     * @param out the output where to write the XML
     * @param bWithAttachmentContent if true, binary content of the attachment is included (base64 encoded)
     * @param bWithVersions if true, all archive version is also included
     * @param format true if the XML should be formated
     * @param context current XWikiContext
     * @throws IOException when an error occurs during streaming operation
     * @throws XWikiException when an error occurs during xwiki operation
     * @since 9.0RC1
     */
    public void toXML(OutputTarget out, boolean bWithAttachmentContent, boolean bWithVersions, boolean format,
        XWikiContext context) throws IOException, XWikiException
    {
        toXML(out, bWithAttachmentContent, bWithVersions, format, context.getWiki().getEncoding());
    }

    /**
     * Write an XML representation of the attachment into an {@link com.xpn.xwiki.internal.xml.XMLWriter}
     *
     * @param out the output where to write the XML
     * @param bWithAttachmentContent if true, binary content of the attachment is included (base64 encoded)
     * @param bWithVersions if true, all archive version is also included
     * @param format true if the XML should be formated
     * @param encoding the encoding to use when serializing XML
     * @throws XWikiException when an error occurs during xwiki operation
     * @since 9.10RC1
     */
    public void toXML(OutputTarget out, boolean bWithAttachmentContent, boolean bWithVersions, boolean format,
        String encoding) throws XWikiException
    {
        // Input
        DocumentInstanceInputProperties documentProperties = new DocumentInstanceInputProperties();
        documentProperties.setWithWikiAttachmentsContent(bWithAttachmentContent);
        documentProperties.setWithJRCSRevisions(bWithVersions);
        documentProperties.setWithRevisions(false);

        // Output
        XAROutputProperties xarProperties = new XAROutputProperties();
        xarProperties.setPreserveVersion(bWithVersions);
        xarProperties.setEncoding(encoding);
        xarProperties.setFormat(format);

        try {
            Utils.getComponent(XWikiDocumentFilterUtils.class).exportEntity(this, out, xarProperties,
                documentProperties);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml", e, null);
        }
    }

    /**
     * Retrieve XML representation of attachment's metadata into an {@link Element}. You should prefer
     * {@link #toXML(com.xpn.xwiki.internal.xml.XMLWriter, boolean, boolean, com.xpn.xwiki.XWikiContext)} to avoid
     * memory loads when appropriate.
     *
     * @param bWithAttachmentContent if true, binary content of the attachment is included (base64 encoded)
     * @param bWithVersions if true, all archived versions are also included
     * @param context current XWikiContext
     * @return an {@link Element} containing an XML representation of the attachment
     * @throws XWikiException when an error occurs during wiki operations
     * @since 2.3M2
     */
    public Element toXML(boolean bWithAttachmentContent, boolean bWithVersions, XWikiContext context)
        throws XWikiException
    {
        DocumentResult domResult = new DocumentResult();

        try {
            toXML(new DefaultResultOutputTarget(domResult), bWithAttachmentContent, bWithVersions, true, context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return domResult.getDocument().getRootElement();
    }

    public void fromXML(String source) throws XWikiException
    {
        if (!source.isEmpty()) {
            fromXML(new StringInputSource(source));
        }
    }

    /**
     * @param source the XML source to parse
     * @throws XWikiException when failing to parse the XML
     * @since 9.0RC1
     */
    public void fromXML(InputSource source) throws XWikiException
    {
        try {
            Utils.getComponent(XWikiDocumentFilterUtils.class).importEntity(this, source);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml", e, null);
        }

        // The setters we're calling above will set the metadata dirty flag to true since they're changing the
        // attachment's identity. However since this method is about loading the attachment from XML it shouldn't be
        // considered as dirty.
        setMetaDataDirty(false);
    }

    public void fromXML(Element docel) throws XWikiException
    {
        // Serialize the Document (could not find a way to convert a dom4j Element into a usable StAX source)
        StringWriter writer = new StringWriter();
        try {
            org.dom4j.io.XMLWriter domWriter = new org.dom4j.io.XMLWriter(writer);
            domWriter.write(docel);
            domWriter.flush();
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml", e, null);
        }

        // Actually parse the XML
        fromXML(writer.toString());
    }

    /**
     * @return the type of the store used for the content
     * @since 9.10RC1
     */
    public String getContentStore()
    {
        return this.contentStore;
    }

    /**
     * @return true if the content store of this attachment has been explicitly set
     * @since 9.10RC1
     */
    public boolean isContentStoreSet()
    {
        return this.contentStoreSet;
    }

    /**
     * @param contentStore the type of the store used for the content
     * @since 9.10RC1
     */
    public void setContentStore(String contentStore)
    {
        this.contentStore = contentStore;
        this.contentStoreSet = true;
    }

    public XWikiAttachmentContent getAttachment_content()
    {
        return this.content;
    }

    public void setAttachment_content(XWikiAttachmentContent attachment_content)
    {
        this.content = attachment_content;
        if (attachment_content != null) {
            attachment_content.setOwnerDocument(this.doc);
        }
    }

    /**
     * @return the type of the store used for the archive
     * @since 9.10RC1
     */
    public String getArchiveStore()
    {
        return this.archiveStore;
    }

    /**
     * @return true if the archive store of this attachment has been explicitly set
     * @since 9.10RC1
     */
    public boolean isArchiveStoreSet()
    {
        return this.archiveStoreSet;
    }

    /**
     * @param archiveStore the type of the store used for the archive
     * @since 9.10RC1
     */
    public void setArchiveStore(String archiveStore)
    {
        this.archiveStore = archiveStore;
        this.archiveStoreSet = true;
    }

    public XWikiAttachmentArchive getAttachment_archive()
    {
        return this.attachment_archive;
    }

    public void setAttachment_archive(XWikiAttachmentArchive attachment_archive)
    {
        this.attachment_archive = attachment_archive;
    }

    /**
     * Retrieve the content of this attachment as a byte array.
     *
     * @param context current XWikiContext
     * @return a byte array containing the binary data content of the attachment
     * @throws XWikiException when an error occurs during wiki operation
     * @deprecated use {@link #getContentInputStream(XWikiContext)} instead
     */
    @Deprecated
    public byte[] getContent(XWikiContext context) throws XWikiException
    {
        if (this.content == null && context != null) {
            loadAttachmentContent(context);
        }

        return this.content.getContent();
    }

    /**
     * Retrieve the content of this attachment as an input stream.
     *
     * @param context current XWikiContext
     * @return an InputStream to consume for receiving the content of this attachment
     * @throws XWikiException when an error occurs during wiki operation
     * @since 2.3M2
     */
    public InputStream getContentInputStream(XWikiContext context) throws XWikiException
    {
        if (this.content == null && context != null) {
            if (Objects.equals(getVersion(), getLatestStoredVersion(context))) {
                // Load the attachment content from the xwikiattachment_content table.
                loadAttachmentContent(context);
            } else {
                // Load the attachment content from the xwikiattachment_archive table.
                // We don't use #getAttachmentRevision() because it checks if the requested version equals the version
                // of the target attachment (XWIKI-1938).
                XWikiAttachment archivedVersion = loadArchive(context).getRevision(this, getVersion(), context);
                XWikiAttachmentContent archivedContent =
                    archivedVersion != null ? archivedVersion.getAttachment_content() : null;
                if (archivedContent != null) {
                    setAttachment_content(archivedContent);
                } else {
                    // Fall back on the version of the content stored in the xwikiattachment_content table.
                    loadAttachmentContent(context);
                }
            }
        }

        return this.content != null ? this.content.getContentInputStream() : null;
    }

    /**
     * The {@code xwikiattachment_content} table stores only the latest version of an attachment (which is identified by
     * the attachment file name and the owner document reference). The rest of the attachment versions are stored in
     * {@code xwikiattachment_archive} table. This method can be used to determine from where to load the attachment
     * content.
     * 
     * @param context the XWiki context
     * @return the latest stored version of this attachment
     */
    private String getLatestStoredVersion(XWikiContext context)
    {
        try {
            XWikiDocument ownerDocumentLastestVersion =
                context.getWiki().getDocument(this.doc.getDocumentReference(), context);
            XWikiAttachment latestStoredVersion = ownerDocumentLastestVersion.getAttachment(this.filename);

            return latestStoredVersion != null ? latestStoredVersion.getVersion() : null;
        } catch (XWikiException e) {
            LOGGER.warn(ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * @deprecated since 2.6M1 please do not use this, it is bound to a jrcs based implementation.
     */
    @Deprecated
    public Archive getArchive()
    {
        if (this.attachment_archive == null) {
            return null;
        } else {
            return this.attachment_archive.getRCSArchive();
        }
    }

    /**
     * @deprecated since 2.6M1 please do not use this, it is bound to a jrcs based implementation.
     */
    @Deprecated
    public void setArchive(Archive archive)
    {
        if (this.attachment_archive == null) {
            this.attachment_archive = new XWikiAttachmentArchive();
            this.attachment_archive.setAttachment(this);
        }

        this.attachment_archive.setRCSArchive(archive);
    }

    public void setArchive(String data) throws XWikiException
    {
        if (this.attachment_archive == null) {
            this.attachment_archive = new XWikiAttachmentArchive();
            this.attachment_archive.setAttachment(this);
        }

        this.attachment_archive.setArchive(data);
    }

    public synchronized Version[] getVersions()
    {
        try {
            return getAttachment_archive().getVersions();
        } catch (Exception ex) {
            LOGGER.warn("Cannot retrieve versions of attachment [{}@{}]: {}",
                new Object[] { getFilename(), getDoc().getDocumentReference(), ex.getMessage() });
            return new Version[] { new Version(this.getVersion()) };
        }
    }

    /**
     * Get the list of all versions up to the current. We assume versions go from 1.1 to the current one This allows not
     * to read the full archive file.
     *
     * @return a list of Version from 1.1 to the current version.
     * @throws XWikiException never happens.
     */
    public List<Version> getVersionList() throws XWikiException
    {
        final List<Version> list = new ArrayList<Version>();
        final String currentVersion = this.version.toString();
        Version v = new Version("1.1");
        for (;;) {
            list.add(v);
            if (v.toString().equals(currentVersion)) {
                break;
            }
            v = v.next();
        }

        return list;
    }

    /**
     * Set the content of an attachment from a byte array.
     *
     * @param data a byte array with the binary content of the attachment
     * @deprecated use {@link #setContent(java.io.InputStream, int)} instead
     */
    @Deprecated
    public void setContent(byte[] data)
    {
        if (this.content == null) {
            this.content = new XWikiAttachmentContent();
            this.content.setAttachment(this);
        }

        this.content.setContent(data);
    }

    /**
     * Set the content of an attachment from an InputStream.
     *
     * @param is the input stream that will be read
     * @param length the length in byte to read
     * @throws IOException when an error occurs during streaming operation
     * @since 2.3M2
     */
    public void setContent(InputStream is, int length) throws IOException
    {
        if (this.content == null) {
            this.content = new XWikiAttachmentContent();
            this.content.setAttachment(this);
        }

        this.content.setContent(is, length);
    }

    /**
     * Set the content of the attachment from an InputStream.
     *
     * @param is the input stream that will be read
     * @throws IOException when an error occurs during streaming operation
     * @since 2.6M1
     */
    public void setContent(InputStream is) throws IOException
    {
        if (this.content == null) {
            this.content = new XWikiAttachmentContent(this);
        }

        this.content.setContent(is);
    }

    public void loadAttachmentContent(XWikiContext xcontext) throws XWikiException
    {
        if (this.content == null) {
            WikiReference currentWiki = xcontext.getWikiReference();

            try {
                // Make sure we work on the attachment's wiki
                WikiReference attachmentWiki = getReference().getDocumentReference().getWikiReference();
                if (attachmentWiki != null) {
                    xcontext.setWikiReference(attachmentWiki);
                }

                try {
                    XWikiAttachmentStoreInterface store = getAttachmentContentStore(xcontext);

                    store.loadAttachmentContent(this, xcontext, true);
                } catch (ComponentLookupException e) {
                    throw new XWikiException("Failed to find store for attachment [" + getReference() + "]", e);
                }
            } finally {
                if (currentWiki != null) {
                    xcontext.setWikiReference(currentWiki);
                }
            }
        }
    }

    /**
     * @deprecated since 9.11RC1, use {@link #loadAttachmentContent(XWikiContext)} instead
     */
    @Deprecated
    public void loadContent(XWikiContext xcontext)
    {
        try {
            loadAttachmentContent(xcontext);
        } catch (Exception e) {
            LOGGER.error(
                "Failed to load content for attachment [{}@{}]. "
                    + "This attachment is broken, please consider re-uploading it",
                this.doc != null ? this.doc.getDocumentReference() : "<unknown>", getFilename(), e);
        }
    }

    public XWikiAttachmentArchive loadArchive(XWikiContext xcontext)
    {
        if (this.attachment_archive == null) {
            WikiReference currentWiki = xcontext.getWikiReference();

            try {
                // Make sure we work on the attachment's wiki
                WikiReference attachmentWiki = getReference().getDocumentReference().getWikiReference();
                if (attachmentWiki != null) {
                    xcontext.setWikiReference(attachmentWiki);
                }

                try {
                    AttachmentVersioningStore store = getAttachmentVersioningStore(xcontext);

                    this.attachment_archive = store.loadArchive(this, xcontext, true);
                } catch (Exception e) {
                    LOGGER.warn(
                        "Failed to load archive for attachment [{}@{}]. "
                            + "This attachment is broken, please consider re-uploading it",
                        this.doc != null ? this.doc.getDocumentReference() : "<unknown>", getFilename(), e);
                }
            } finally {
                if (currentWiki != null) {
                    xcontext.setWikiReference(currentWiki);
                }
            }
        }

        return this.attachment_archive;
    }

    public void updateContentArchive(XWikiContext context) throws XWikiException
    {
        if (this.content == null) {
            return;
        }

        loadArchive(context).updateArchive(context);
    }

    /**
     * Return the stored media type. If none is stored try to detects the media type of this attachment's content using
     * {@link Tika}. We first try to determine the media type based on the file name extension and if the extension is
     * unknown we try to determine the media type by reading the first bytes of the attachment content.
     *
     * @param xcontext the XWiki context
     * @return the media type of this attachment's content
     */
    public String getMimeType(XWikiContext xcontext)
    {
        String type = getMimeType();

        if (StringUtils.isEmpty(type)) {
            type = extractMimeType(xcontext);
        }

        return type;
    }

    /**
     * Return the stored media type.
     * 
     * @return the media type of this attachment's content
     * @since 7.1M1
     */
    public String getMimeType()
    {
        return this.mimeType;
    }

    /**
     * @param mimeType the explicit mime type of the file
     * @since 7.1M1
     */
    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    /**
     * Extract the mime type from the file name and content and remember it to be stored.
     * 
     * @param xcontext the {@link XWikiContext} use to load the content if it's not already loaded
     * @since 7.1M1
     */
    public void resetMimeType(XWikiContext xcontext)
    {
        this.mimeType = extractMimeType(xcontext);
    }

    private String extractMimeType(XWikiContext xcontext)
    {
        // We try name-based detection and then fall back on content-based detection. We don't do this in a single step,
        // by passing both the content and the file name to Tika, because the default detector looks at the content
        // first which can be an issue for large attachments. Our approach is less accurate but has better performance.
        String mediaType = getFilename() != null ? TikaUtils.detect(getFilename()) : MediaType.OCTET_STREAM.toString();
        if (MediaType.OCTET_STREAM.toString().equals(mediaType)) {
            try {
                // Content-based detection is more accurate but it may require loading the attachment content in memory
                // (from the database) if it hasn't been cached as a temporary file yet. This can be an issue for large
                // attachments when database storage is used. Only the first bytes are normally read but still this
                // process is slower than name-based detection.
                //
                // We wrap the content input stream in a BufferedInputStream to make sure that all the detectors can
                // read the content even if the input stream is configured to auto close when it reaches the end. This
                // can happen for small files if AutoCloseInputStream is used, which supports the mark and reset methods
                // so Tika uses it directly. In this case, the input stream is automatically closed after the first
                // detector reads it so the next detector fails to read it.
                mediaType = TikaUtils.detect(new BufferedInputStream(getContentInputStream(xcontext)));
            } catch (Exception e) {
                LOGGER.warn("Failed to read the content of [{}] in order to detect its mime type.", getReference());
            }
        }

        return mediaType;
    }

    public boolean isImage(XWikiContext context)
    {
        String contenttype = getMimeType(context);
        if (contenttype.startsWith("image/")) {
            return true;
        } else {
            return false;
        }
    }

    public XWikiAttachment getAttachmentRevision(String rev, XWikiContext context) throws XWikiException
    {
        if (StringUtils.equals(rev, this.getVersion())) {
            return this;
        }

        return loadArchive(context).getRevision(this, rev, context);
    }

    /**
     * Apply the provided attachment so that the current one contains the same informations and indicate if it was
     * necessary to modify it in any way.
     *
     * @param attachment the attachment to apply
     * @return true if the attachment has been modified
     * @since 5.3M2
     */
    public boolean apply(XWikiAttachment attachment)
    {
        boolean modified = false;

        if (getLongSize() != attachment.getLongSize()) {
            setLongSize(attachment.getLongSize());
            modified = true;
        }

        if (StringUtils.equals(getMimeType(), attachment.getMimeType())) {
            setMimeType(attachment.getMimeType());
            modified = true;
        }

        try {
            if (!IOUtils.contentEquals(getContentInputStream(null), attachment.getContentInputStream(null))) {
                setContent(attachment.getContentInputStream(null));
                modified = true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to compare content of attachments", e);
        }

        return modified;
    }

    public boolean equalsData(XWikiAttachment otherAttachment, XWikiContext xcontext) throws XWikiException
    {
        try {
            if (getLongSize() == otherAttachment.getLongSize()) {
                InputStream is = getContentInputStream(xcontext);

                try {
                    InputStream otherIS = otherAttachment.getContentInputStream(xcontext);

                    try {
                        return IOUtils.contentEquals(is, otherIS);
                    } finally {
                        otherIS.close();
                    }
                } finally {
                    is.close();
                }
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to compare attachments", e);
        }

        return false;
    }

    public void merge(XWikiAttachment previousAttachment, XWikiAttachment nextAttachment,
        MergeConfiguration configuration, XWikiContext xcontext, MergeResult mergeResult)
    {
        try {
            if (equalsData(previousAttachment, xcontext)) {
                this.apply(nextAttachment);
                mergeResult.setModified(true);
            } else {
                if (this.equals(nextAttachment)) {
                    // Already modified as expected in the DB, lets assume the user is prescient
                    mergeResult.getLog().warn("Attachment [{}] already modified", getReference());
                }
            }
        } catch (Exception e) {
            mergeResult.getLog().error("Failed to merge attachment [{}]", this, e);
        }
    }

    /**
     * @param userReference the user {@link DocumentReference} to convert to {@link String}
     * @return the user as String
     */
    private String userReferenceToString(DocumentReference userReference)
    {
        String userString;

        if (userReference != null) {
            userString = getCompactWikiEntityReferenceSerializer().serialize(userReference, getReference());
        } else {
            userString = XWikiRightService.GUEST_USER_FULLNAME;
        }

        return userString;
    }

    /**
     * @param userString the user {@link String} to convert to {@link DocumentReference}
     * @return the user as {@link DocumentReference}
     */
    private DocumentReference userStringToReference(String userString)
    {
        DocumentReference userReference;

        if (StringUtils.isEmpty(userString)) {
            userReference = null;
        } else {
            userReference = getExplicitReferenceDocumentReferenceResolver()
                .resolve(getXClassEntityReferenceResolver().resolve(userString, EntityType.DOCUMENT), getReference());

            if (userReference.getName().equals(XWikiRightService.GUEST_USER)) {
                userReference = null;
            }
        }

        return userReference;
    }

    private XWikiAttachmentStoreInterface getAttachmentContentStore(XWikiContext xcontext)
        throws ComponentLookupException
    {
        if (this.contentStoreInstance == null) {
            if (!this.contentStoreSet) {
                this.contentStoreInstance = xcontext.getWiki().getDefaultAttachmentContentStore();

                setContentStore(this.contentStoreInstance.getHint());
            } else {
                String hint = getContentStore();

                if (hint != null) {
                    this.contentStoreInstance =
                        Utils.getContextComponentManager().getInstance(XWikiAttachmentStoreInterface.class, hint);
                } else {
                    return Utils.getContextComponentManager().getInstance(XWikiAttachmentStoreInterface.class,
                        XWikiHibernateBaseStore.HINT);
                }
            }
        }

        return this.contentStoreInstance;
    }

    private AttachmentVersioningStore getAttachmentVersioningStore(XWikiContext xcontext)
        throws ComponentLookupException
    {
        if (this.archiveStoreInstance == null) {
            if (!this.archiveStoreSet) {
                this.archiveStoreInstance = xcontext.getWiki().getDefaultAttachmentArchiveStore();

                setContentStore(this.archiveStoreInstance.getHint());
            } else {
                String hint = getArchiveStore();

                if (hint != null) {
                    this.archiveStoreInstance =
                        Utils.getContextComponentManager().getInstance(AttachmentVersioningStore.class, hint);
                } else {
                    this.archiveStoreInstance = Utils.getContextComponentManager()
                        .getInstance(AttachmentVersioningStore.class, XWikiHibernateBaseStore.HINT);
                }
            }
        }

        return this.archiveStoreInstance;
    }

    public void addNameModifiedListener(AttachmentNameChanged listener)
    {
        this.listeners.add(listener);
    }

    public void removeNameModifiedListener(AttachmentNameChanged listener)
    {
        this.listeners.remove(listener);
    }

    public void notificateNameModifed(String previousAttachmentName, XWikiAttachment attachment)
    {
        for (AttachmentNameChanged listener : this.listeners) {
            listener.onAttachmentNameModified(previousAttachmentName, attachment);
        }
    }
}
