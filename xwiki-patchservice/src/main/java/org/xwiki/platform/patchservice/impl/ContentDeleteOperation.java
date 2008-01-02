package org.xwiki.platform.patchservice.impl;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.Position;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class ContentDeleteOperation extends AbstractOperationImpl implements RWOperation
{
    private Position position;

    private String removedContent;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_CONTENT_DELETE,
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
            if (!position.checkPosition(content)
                || !position.getTextAfterPosition(content).startsWith(this.removedContent)) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                    XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Patch does not fit. Expected ["
                        + StringUtils.abbreviate(this.removedContent, 20) + "], but found ["
                        + StringUtils.abbreviate(position.getTextAfterPosition(content), 20)
                        + "]");
            }
            content =
                position.getTextBeforePosition(content)
                    + position.getTextAfterPosition(content).substring(
                        this.removedContent.length());
            doc.setContent(content);
        } catch (StringIndexOutOfBoundsException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Patch cannot be applied",
                ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean delete(String text, Position position)
    {
        this.removedContent = text;
        this.position = position;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        Element textNode = (Element) e.getElementsByTagName(TEXT_NODE_NAME).item(0);
        this.removedContent = StringEscapeUtils.unescapeXml(textNode.getTextContent());
        this.position = new PositionImpl();
        position.fromXml((Element) e.getElementsByTagName(PositionImpl.NODE_NAME).item(0));
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(AbstractOperationImpl.NODE_NAME);
        xmlNode.setAttribute(AbstractOperationImpl.TYPE_ATTRIBUTE_NAME,
            Operation.TYPE_CONTENT_DELETE);
        Element textNode = doc.createElement(TEXT_NODE_NAME);
        textNode
            .appendChild(doc.createTextNode(StringEscapeUtils.escapeXml(this.removedContent)));
        xmlNode.appendChild(textNode);
        xmlNode.appendChild(position.toXml(doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            ContentDeleteOperation otherOperation = (ContentDeleteOperation) other;
            return otherOperation.position.equals(this.position)
                && otherOperation.removedContent.equals(this.removedContent);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return new HashCodeBuilder(5, 7).append(this.position).append(this.removedContent)
            .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getType() + ": [" + this.removedContent + "] at " + this.position;
    }
}
