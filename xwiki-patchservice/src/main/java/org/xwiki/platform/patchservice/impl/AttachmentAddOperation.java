package org.xwiki.platform.patchservice.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class AttachmentAddOperation extends AbstractOperationImpl implements RWOperation
{
    private byte[] data;

    private String filename;

    private String author;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_ATTACHMENT_ADD,
            AttachmentAddOperation.class);
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_ATTACHMENT_SET,
            AttachmentAddOperation.class);
    }

    public AttachmentAddOperation()
    {
        this.setType(Operation.TYPE_ATTACHMENT_ADD);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            XWikiAttachment attachment = doc.getAttachment(filename);
            if (this.getType().equals(TYPE_ATTACHMENT_ADD)) {
                if (attachment != null) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                        XWikiException.ERROR_XWIKI_UNKNOWN,
                        new Formatter().format(
                            "Cannot apply patch: Attachment already exists: [%s]",
                            new Object[] {filename}).toString());
                }
                attachment = new XWikiAttachment();
            }
            doc.getAttachmentList().add(attachment);
            attachment.setContent(data);
            attachment.setFilename(filename);
            attachment.setAuthor(author);
            attachment.setDoc(doc);
        } catch (Exception ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid attachment: " + this.filename);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean addAttachment(InputStream is, String filename, String author)
    {
        this.setType(TYPE_ATTACHMENT_ADD);
        try {
            data = org.apache.commons.io.IOUtils.toByteArray(is);
            this.filename = filename;
            this.author = author;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean setAttachment(InputStream is, String filename, String author)
    {
        if (!this.addAttachment(is, filename, author)) {
            return false;
        }
        this.setType(TYPE_ATTACHMENT_SET);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.setType(getOperationType(e));
        this.data = getAttachmentContent(e);
        this.filename = getAttachmentFilename(e);
        this.author = getAttachmentAuthor(e);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        xmlNode.appendChild(createAttachmentNode(data, filename, author, doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            AttachmentAddOperation that = (AttachmentAddOperation) other;
            return this.getType().equals(that.getType()) && this.filename.equals(that.filename)
                && this.author.equals(that.author) && ArrayUtils.isEquals(this.data, that.data);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return new HashCodeBuilder(37, 41).append(this.getType()).append(this.filename).append(
            this.author).append(this.data).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getType() + ": [" + this.filename + "] by [" + this.author + "]";
    }
}
