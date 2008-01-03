package org.xwiki.platform.patchservice.impl;

import java.util.Formatter;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class AttachmentDeleteOperation extends AbstractOperationImpl implements RWOperation
{
    private String filename;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_ATTACHMENT_DELETE,
            AttachmentDeleteOperation.class);
    }

    public AttachmentDeleteOperation()
    {
        this.setType(Operation.TYPE_ATTACHMENT_DELETE);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            XWikiAttachment attachment = doc.getAttachment(filename);
            if (attachment == null) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                    XWikiException.ERROR_XWIKI_UNKNOWN,
                    new Formatter().format(
                        "Cannot apply patch: Attachment does not exists: [%s]",
                        new Object[] {filename}).toString());
            }
            doc.deleteAttachment(attachment, context);
        } catch (Exception ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid attachment: " + this.filename);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteAttachment(String filename)
    {
        this.filename = filename;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.filename = getAttachmentFilename(e);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        xmlNode.appendChild(createAttachmentNode(filename, doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            AttachmentDeleteOperation that = (AttachmentDeleteOperation) other;
            return this.getType().equals(that.getType()) && this.filename.equals(that.filename);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return new HashCodeBuilder(37, 41).append(this.getType()).append(this.filename)
            .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getType() + ": [" + this.filename + "]";
    }
}
