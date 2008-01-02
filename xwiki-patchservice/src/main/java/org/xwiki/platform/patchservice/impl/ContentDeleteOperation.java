package org.xwiki.platform.patchservice.impl;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class ContentDeleteOperation extends AbstractOperationImpl implements RWOperation
{
    public static final String POSITION_ATTRIBUTE_NAME = "position";

    private int position = -1;

    private String removedContent;

    static {
        OperationFactoryImpl.getInstance().registerTypeProvider(Operation.TYPE_CONTENT_DELETE,
            ContentDeleteOperation.class);
    }

    public ContentDeleteOperation()
    {
        this.setType(Operation.TYPE_CONTENT_DELETE);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc) throws XWikiException
    {
        try {
            String content = doc.getContent();
            if (content.indexOf(this.removedContent, this.position) != this.position) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                    XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Patch does not fit. Expected ["
                        + StringUtils.abbreviate(this.removedContent, 20)
                        + "], but found ["
                        + StringUtils.abbreviate(StringUtils.mid(content, this.position,
                            this.removedContent.length()), 20) + "]");
            }
            content =
                content.substring(0, position)
                    + content.substring(position + this.removedContent.length());
            doc.setContent(content);
        } catch (StringIndexOutOfBoundsException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Patch cannot be applied",
                ex);
        }
    }

    public boolean delete(String text, int position)
    {
        this.removedContent = text;
        this.position = position;
        return true;
    }

    public void fromXml(Element e) throws XWikiException
    {
        Element textNode = (Element) e.getFirstChild();
        this.position = Integer.parseInt(textNode.getAttribute(POSITION_ATTRIBUTE_NAME));
        this.removedContent = StringEscapeUtils.unescapeXml(textNode.getTextContent());
    }

    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(AbstractOperationImpl.NODE_NAME);
        xmlNode.setAttribute(AbstractOperationImpl.TYPE_ATTRIBUTE_NAME,
            Operation.TYPE_CONTENT_DELETE);
        Element textNode = doc.createElement(TEXT_NODE_NAME);
        textNode.setAttribute(POSITION_ATTRIBUTE_NAME, this.position + "");
        textNode
            .appendChild(doc.createTextNode(StringEscapeUtils.escapeXml(this.removedContent)));
        xmlNode.appendChild(textNode);
        return xmlNode;
    }

    public boolean equals(Object other)
    {
        try {
            ContentDeleteOperation otherOperation = (ContentDeleteOperation) other;
            return (otherOperation.position == this.position)
                && otherOperation.removedContent.equals(this.removedContent);
        } catch (Exception e) {
            return false;
        }
    }

    public int hashCode()
    {
        return new HashCodeBuilder(5, 7).append(this.position).append(this.removedContent)
            .toHashCode();
    }

    public String toString()
    {
        return Operation.TYPE_CONTENT_DELETE + ": [" + this.removedContent + "] at "
            + this.position;
    }
}
