package org.xwiki.platform.patchservice.impl;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.Position;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class ContentInsertOperation extends AbstractOperationImpl implements RWOperation
{
    private Position position;

    private String addedContent;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_CONTENT_INSERT,
            ContentInsertOperation.class);
    }

    public ContentInsertOperation()
    {
        this.setType(Operation.TYPE_CONTENT_INSERT);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc) throws XWikiException
    {
        String content = doc.getContent();
        if (!position.checkPosition(content)) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Patch cannot be applied: invalid position " + position);
        }
        content =
            position.getTextBeforePosition(content) + this.addedContent
                + position.getTextAfterPosition(content);
        doc.setContent(content);
    }

    /**
     * {@inheritDoc}
     */
    public boolean insert(String text, Position position)
    {
        this.addedContent = text;
        this.position = position;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        Element textNode = (Element) e.getElementsByTagName(TEXT_NODE_NAME).item(0);
        this.addedContent = StringEscapeUtils.unescapeXml(textNode.getTextContent());
        this.position = new PositionImpl();
        position.fromXml((Element) e.getElementsByTagName(PositionImpl.NODE_NAME).item(0));
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(AbstractOperationImpl.NODE_NAME);
        xmlNode.setAttribute(AbstractOperationImpl.TYPE_ATTRIBUTE_NAME, this.getType());
        Element textNode = doc.createElement(TEXT_NODE_NAME);
        textNode.appendChild(doc.createTextNode(StringEscapeUtils.escapeXml(this.addedContent)));
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
            ContentInsertOperation otherOperation = (ContentInsertOperation) other;
            return otherOperation.position.equals(this.position)
                && otherOperation.addedContent.equals(this.addedContent);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return new HashCodeBuilder(3, 5).append(this.position).append(this.addedContent)
            .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getType() + ": [" + this.addedContent + "] at " + this.position;
    }
}
