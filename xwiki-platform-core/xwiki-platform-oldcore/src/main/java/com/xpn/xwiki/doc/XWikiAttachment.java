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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.model.reference.AttachmentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.xml.DOMXMLWriter;
import com.xpn.xwiki.internal.xml.XMLWriter;

public class XWikiAttachment implements Cloneable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiAttachment.class);

    private XWikiDocument doc;

    private int filesize;

    private String filename;

    private String author;

    private Version version;

    private String comment;

    private Date date;

    private XWikiAttachmentContent attachment_content;

    private XWikiAttachmentArchive attachment_archive;

    private boolean isMetaDataDirty = false;
    
    private AttachmentReference reference;

    public XWikiAttachment(XWikiDocument doc, String filename)
    {
        setDoc(doc);
        setFilename(filename);
    }

    public XWikiAttachment()
    {
        this.filesize = 0;
        this.filename = "";
        this.author = "";
        this.comment = "";
        this.date = new Date();
    }

    public AttachmentReference getReference()
    {
        if (this.reference == null) {
            this.reference = new AttachmentReference(this.filename, doc.getDocumentReference());
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

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone()
    {
        XWikiAttachment attachment = null;
        try {
            attachment = getClass().newInstance();
        } catch (Exception e) {
            // This should not happen
            LOGGER.error("exception while attach.clone", e);
        }

        attachment.setAuthor(getAuthor());
        attachment.setComment(getComment());
        attachment.setDate(getDate());
        attachment.setDoc(getDoc());
        attachment.setFilename(getFilename());
        attachment.setFilesize(getFilesize());
        attachment.setRCSVersion(getRCSVersion());
        if (getAttachment_content() != null) {
            attachment.setAttachment_content((XWikiAttachmentContent) getAttachment_content().clone());
            attachment.getAttachment_content().setAttachment(attachment);
        }
        if (getAttachment_archive() != null) {
            attachment.setAttachment_archive((XWikiAttachmentArchive) getAttachment_archive().clone());
            attachment.getAttachment_archive().setAttachment(attachment);
        }

        return attachment;
    }

    /**
     * @return the cached filesize in byte of the attachment, stored as metadata
     */
    public int getFilesize()
    {
        return this.filesize;
    }

    /**
     * Set cached filesize of the attachment that will be stored as metadata
     *
     * @param filesize in byte
     */
    public void setFilesize(int filesize)
    {
        if (filesize != this.filesize) {
            setMetaDataDirty(true);
        }

        this.filesize = filesize;
    }

    /**
     * @param context current XWikiContext
     * @return the real filesize in byte of the attachment. We cannot trust the metadata that may be
     *         publicly changed.
     * @throws XWikiException
     * @since 2.3M2
     */
    public int getContentSize(XWikiContext context) throws XWikiException
    {
        if (this.attachment_content == null) {
            this.doc.loadAttachmentContent(this, context);
        }

        return this.attachment_content.getSize();
    }

    public String getFilename()
    {
        return this.filename;
    }

    public void setFilename(String filename)
    {
        filename = filename.replaceAll("\\+", " ");
        if (!filename.equals(this.filename)) {
            setMetaDataDirty(true);
            this.filename = filename;
        }
        this.reference = null;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public void setAuthor(String author)
    {
        if (!author.equals(this.author)) {
            setMetaDataDirty(true);
        }

        this.author = author;
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
        if (!getComment().equals(comment)) {
            setMetaDataDirty(true);
        }

        this.comment = comment;
    }

    public XWikiDocument getDoc()
    {
        return this.doc;
    }

    public void setDoc(XWikiDocument doc)
    {
        this.doc = doc;
        this.reference = null;
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
        if (this.attachment_content == null) {
            return false;
        } else {
            return this.attachment_content.isContentDirty();
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
    }

    /**
     * Retrieve an attachment as an XML string. You should prefer
     * {@link #toXML(com.xpn.xwiki.internal.xml.XMLWriter, boolean, boolean, com.xpn.xwiki.XWikiContext)
     * to avoid memory loads when appropriate.
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
        // This is very bad. baos holds the entire attachment on the heap, then it makes a copy when toByteArray
        // is called, then String forces us to make a copy when we construct a new String.
        // Unfortunately this can't be fixed because jrcs demands the content as a String.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            XMLWriter wr = new XMLWriter(baos, new OutputFormat("", true, context.getWiki().getEncoding()));
            Document doc = new DOMDocument();
            wr.writeDocumentStart(doc);
            toXML(wr, bWithAttachmentContent, bWithVersions, context);
            wr.writeDocumentEnd(doc);
            byte[] array = baos.toByteArray();
            baos = null;
            return new String(array, context.getWiki().getEncoding());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
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
        // IMPORTANT: we don't use SAX apis here because the specified XMLWriter could be a DOMXMLWriter for retro
        // compatibility reasons

        Element docel = new DOMElement("attachment");
        wr.writeOpen(docel);

        Element el = new DOMElement("filename");
        el.addText(getFilename());
        wr.write(el);

        el = new DOMElement("filesize");
        el.addText("" + getFilesize());
        wr.write(el);

        el = new DOMElement("author");
        el.addText(getAuthor());
        wr.write(el);

        long d = getDate().getTime();
        el = new DOMElement("date");
        el.addText("" + d);
        wr.write(el);

        el = new DOMElement("version");
        el.addText(getVersion());
        wr.write(el);

        el = new DOMElement("comment");
        el.addText(getComment());
        wr.write(el);

        if (bWithAttachmentContent) {
            el = new DOMElement("content");
            // We need to make sure content is loaded
            loadContent(context);
            XWikiAttachmentContent acontent = getAttachment_content();
            if (acontent != null) {
                wr.writeBase64(el, getAttachment_content().getContentInputStream());
            } else {
                el.addText("");
                wr.write(el);
            }
        }

        if (bWithVersions) {
            // We need to make sure content is loaded
            XWikiAttachmentArchive aarchive = loadArchive(context);
            if (aarchive != null) {
                el = new DOMElement("versions");
                try {
                    el.addText(new String(aarchive.getArchive()));
                    wr.write(el);
                } catch (XWikiException e) {
                }
            }
        }

        wr.writeClose(docel);
    }

    /**
     * Retrieve XML representation of attachment's metadata into an {@link Element}. You should prefer
     * {@link #toXML(com.xpn.xwiki.internal.xml.XMLWriter, boolean, boolean, com.xpn.xwiki.XWikiContext)}
     * to avoid memory loads when appropriate.
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
        Document doc = new DOMDocument();
        DOMXMLWriter wr = new DOMXMLWriter(doc, new OutputFormat("", true, context.getWiki().getEncoding()));

        try {
            toXML(wr, bWithAttachmentContent, bWithVersions, context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return doc.getRootElement();
    }

    public void fromXML(String data) throws XWikiException
    {
        SAXReader reader = new SAXReader();
        Document domdoc = null;
        try {
            StringReader in = new StringReader(data);
            domdoc = reader.read(in);
        } catch (DocumentException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml", e, null);
        }
        Element docel = domdoc.getRootElement();
        fromXML(docel);
    }

    public void fromXML(Element docel) throws XWikiException
    {
        setFilename(docel.element("filename").getText());
        setFilesize(Integer.parseInt(docel.element("filesize").getText()));
        setAuthor(docel.element("author").getText());
        setVersion(docel.element("version").getText());
        setComment(docel.element("comment").getText());

        String sdate = docel.element("date").getText();
        Date date = new Date(Long.parseLong(sdate));
        setDate(date);

        Element contentel = docel.element("content");
        if (contentel != null) {
            String base64content = contentel.getText();
            byte[] content = Base64.decodeBase64(base64content.getBytes());
            setContent(content);
        }
        Element archiveel = docel.element("versions");
        if (archiveel != null) {
            String archive = archiveel.getText();
            setArchive(archive);
        }
    }

    public XWikiAttachmentContent getAttachment_content()
    {
        return this.attachment_content;
    }

    public void setAttachment_content(XWikiAttachmentContent attachment_content)
    {
        this.attachment_content = attachment_content;
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
     * Retrive the content of this attachment as a byte array.
     *
     * @param context current XWikiContext
     * @return a byte array containing the binary data content of the attachment
     * @throws XWikiException when an error occurs during wiki operation
     * @deprecated use {@link #getContentInputStream(XWikiContext)} instead
     */
    @Deprecated
    public byte[] getContent(XWikiContext context) throws XWikiException
    {
        if (this.attachment_content == null) {
            this.doc.loadAttachmentContent(this, context);
        }

        return this.attachment_content.getContent();
    }

    /**
     * Retrive the content of this attachment as an input stream.
     *
     * @param context current XWikiContext
     * @return an InputStream to consume for receiving the content of this attachment
     * @throws XWikiException when an error occurs during wiki operation
     * @since 2.3M2
     */
    public InputStream getContentInputStream(XWikiContext context) throws XWikiException
    {
        if (this.attachment_content == null) {
            this.doc.loadAttachmentContent(this, context);
        }

        return this.attachment_content.getContentInputStream();
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

        this.attachment_archive.setArchive(data.getBytes());
    }

    public synchronized Version[] getVersions()
    {
        try {
            return getAttachment_archive().getVersions();
        } catch (Exception ex) {
            LOGGER.warn(String.format("Cannot retrieve versions of attachment [%s@%s]: %s", getFilename(), getDoc()
                .getFullName(), ex.getMessage()));

            return new Version[] {new Version(this.getVersion())};
        }
    }

    /**
     * Get the list of all versions up to the current.
     * We assume versions go from 1.1 to the current one
     * This allows not to read the full archive file.
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
        if (this.attachment_content == null) {
            this.attachment_content = new XWikiAttachmentContent();
            this.attachment_content.setAttachment(this);
        }

        this.attachment_content.setContent(data);
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
        if (this.attachment_content == null) {
            this.attachment_content = new XWikiAttachmentContent();
            this.attachment_content.setAttachment(this);
        }

        this.attachment_content.setContent(is, length);
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
        if (this.attachment_content == null) {
            this.attachment_content = new XWikiAttachmentContent(this);
        }

        this.attachment_content.setContent(is);
    }

    public void loadContent(XWikiContext context) throws XWikiException
    {
        if (this.attachment_content == null) {
            try {
                context.getWiki().getAttachmentStore().loadAttachmentContent(this, context, true);
            } catch (Exception ex) {
                LOGGER.warn(String.format("Failed to load content for attachment [%s@%s]. "
                    + "This attachment is broken, please consider re-uploading it. " + "Internal error: %s",
                    getFilename(), (this.doc != null) ? this.doc.getFullName() : "<unknown>", ex.getMessage()));
            }
        }
    }

    public XWikiAttachmentArchive loadArchive(XWikiContext context) throws XWikiException
    {
        if (this.attachment_archive == null) {
            try {
                this.attachment_archive =
                    context.getWiki().getAttachmentVersioningStore().loadArchive(this, context, true);
            } catch (Exception ex) {
                LOGGER.warn(String.format("Failed to load archive for attachment [%s@%s]. "
                    + "This attachment is broken, please consider re-uploading it. " + "Internal error: %s",
                    getFilename(), (this.doc != null) ? this.doc.getFullName() : "<unknown>", ex.getMessage()));
            }
        }

        return this.attachment_archive;
    }

    public void updateContentArchive(XWikiContext context) throws XWikiException
    {
        if (this.attachment_content == null) {
            return;
        }

        // XWikiAttachmentArchive no longer uses the byte array passed as it's first parameter making it redundant.
        loadArchive(context).updateArchive(null, context);
    }

    public String getMimeType(XWikiContext context)
    {
        // Choose the right content type
        String mimetype = context.getEngineContext().getMimeType(getFilename().toLowerCase());
        if (mimetype != null) {
            return mimetype;
        } else {
            return "application/octet-stream";
        }
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

}
