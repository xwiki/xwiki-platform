package org.xwiki.platform.patchservice.impl;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class ContentInsertOperation extends AbstractOperationImpl implements RWOperation
{
    public static final String POSITION_ATTRIBUTE_NAME = "position";

    private int position = -1;

    private String addedContent;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_CONTENT_INSERT,
            ContentInsertOperation.class);
    }
    
    public ContentInsertOperation()
    {
        this.setType(Operation.TYPE_CONTENT_INSERT);
    }

    public void apply(XWikiDocument doc) throws XWikiException
    {
        try {
            String content = doc.getContent();
            content =
                content.substring(0, position) + this.addedContent + content.substring(position);
            doc.setContent(content);
        } catch (StringIndexOutOfBoundsException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Patch cannot be applied",
                ex);
        }
    }

    public boolean insert(String text, int position)
    {
        this.addedContent = text;
        this.position = position;
        return true;
    }

    public void fromXml(Element e) throws XWikiException
    {
        Element textNode = (Element) e.getFirstChild();
        this.position = Integer.parseInt(textNode.getAttribute(POSITION_ATTRIBUTE_NAME));
        this.addedContent = StringEscapeUtils.unescapeXml(textNode.getTextContent());
    }

    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(AbstractOperationImpl.NODE_NAME);
        xmlNode.setAttribute(AbstractOperationImpl.TYPE_ATTRIBUTE_NAME,
            Operation.TYPE_CONTENT_INSERT);
        Element textNode = doc.createElement(TEXT_NODE_NAME);
        textNode.setAttribute(POSITION_ATTRIBUTE_NAME, this.position + "");
        textNode.appendChild(doc.createTextNode(StringEscapeUtils.escapeXml(this.addedContent)));
        xmlNode.appendChild(textNode);
        return xmlNode;
    }

    public boolean equals(Object other)
    {
        try {
            ContentInsertOperation otherOperation = (ContentInsertOperation) other;
            return (otherOperation.position == this.position)
                && otherOperation.addedContent.equals(this.addedContent);
        } catch (Exception e) {
            return false;
        }
    }

    public int hashCode()
    {
        return new HashCodeBuilder(3, 5).append(this.position).append(this.addedContent)
            .toHashCode();
    }

    public String toString()
    {
        return Operation.TYPE_CONTENT_INSERT + ": [" + this.addedContent + "] at " + this.position;
    }
}
