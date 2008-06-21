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
 *
 */
package org.xwiki.platform.patchservice.impl;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Position;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiException;

public abstract class AbstractOperationImpl implements RWOperation
{
    public static final String NODE_NAME = "operation";

    public static final String OPERATION_TYPE_ATTRIBUTE_NAME = "type";

    public static final String TEXT_NODE_NAME = "text";

    public static final String OBJECT_NODE_NAME = "object";

    public static final String OBJECT_TYPE_ATTRIBUTE_NAME = OPERATION_TYPE_ATTRIBUTE_NAME;

    public static final String OBJECT_NUMBER_ATTRIBUTE_NAME = "number";

    public static final String CLASS_NODE_NAME = "class";

    public static final String CLASS_NAME_ATTRIBUTE_NAME = "name";

    public static final String PROPERTY_NODE_NAME = "property";

    public static final String PROPERTY_NAME_ATTRIBUTE_NAME = CLASS_NAME_ATTRIBUTE_NAME;

    public static final String PROPERTY_TYPE_ATTRIBUTE_NAME = OPERATION_TYPE_ATTRIBUTE_NAME;

    public static final String PROPERTY_VALUE_ATTRIBUTE_NAME = "value";

    public static final String ATTACHMENT_NODE_NAME = "attachment";

    public static final String ATTACHMENT_FILANAME_ATTRIBUTE_NAME = CLASS_NAME_ATTRIBUTE_NAME;

    public static final String ATTACHMENT_AUTHOR_ATTRIBUTE_NAME = "author";

    private String type;

    /**
     * {@inheritDoc}
     */
    public boolean insert(String text, Position position)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean delete(String text, Position position)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setProperty(String property, String value)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean createType(String className, String typeName, String typeType, Map<String, ? > typeConfig)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean modifyType(String className, String typeName, Map<String, ? > typeConfig)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteType(String className, String typeName)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean addObject(String objectClass)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteObject(String objectClass, int index)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setObjectProperty(String objectClass, int index, String propertyName, String value)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean insertInProperty(String objectClass, int index, String property, String text, Position position)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteFromProperty(String objectClass, int index, String property, String text, Position position)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean addAttachment(InputStream is, String filename, String author)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setAttachment(InputStream is, String filename, String author)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteAttachment(String name)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public String getType()
    {
        return this.type;
    }

    public Element createOperationNode(Document doc)
    {
        Element xmlNode = doc.createElement(NODE_NAME);
        xmlNode.setAttribute(OPERATION_TYPE_ATTRIBUTE_NAME, getType());
        return xmlNode;
    }

    public String getOperationType(Element e)
    {
        return e.getAttribute(OPERATION_TYPE_ATTRIBUTE_NAME);
    }

    public Element createTextNode(String content, Document doc)
    {
        Element xmlNode = doc.createElement(TEXT_NODE_NAME);
        xmlNode.setTextContent(content);
        return xmlNode;
    }

    public Element getTextNode(Element e)
    {
        return (Element) e.getElementsByTagName(TEXT_NODE_NAME).item(0);
    }

    public String getTextValue(Element e)
    {
        return getTextNode(e).getTextContent();
    }

    public Element createObjectNode(String className, Document doc)
    {
        return createObjectNode(className, -1, doc);
    }

    public Element createObjectNode(String className, int number, Document doc)
    {
        Element xmlNode = doc.createElement(OBJECT_NODE_NAME);
        xmlNode.setAttribute(OBJECT_TYPE_ATTRIBUTE_NAME, className);
        if (number >= 0) {
            xmlNode.setAttribute(OBJECT_NUMBER_ATTRIBUTE_NAME, number + "");
        }
        return xmlNode;
    }

    public Element getObjectNode(Element e)
    {
        return (Element) e.getElementsByTagName(OBJECT_NODE_NAME).item(0);
    }

    public String getObjectClassname(Element e)
    {
        return getObjectNode(e).getAttribute(OBJECT_TYPE_ATTRIBUTE_NAME);
    }

    public int getObjectNumber(Element e)
    {
        try {
            return Integer.parseInt(getObjectNode(e).getAttribute(OBJECT_NUMBER_ATTRIBUTE_NAME));
        } catch (Exception ex) {
            return -1;
        }
    }

    public Element createPropertyNode(String propertyName, Document doc)
    {
        return createPropertyNode(propertyName, null, doc);
    }

    public Element createPropertyNode(String propertyName, String value, Document doc)
    {
        Element xmlNode = doc.createElement(PROPERTY_NODE_NAME);
        xmlNode.setAttribute(PROPERTY_NAME_ATTRIBUTE_NAME, propertyName);
        if (value != null) {
            xmlNode.setAttribute(PROPERTY_VALUE_ATTRIBUTE_NAME, value);
        }
        return xmlNode;
    }

    public Element createClassPropertyNode(String propertyName, String propertyType, Document doc)
    {
        Element xmlNode = doc.createElement(PROPERTY_NODE_NAME);
        xmlNode.setAttribute(PROPERTY_NAME_ATTRIBUTE_NAME, propertyName);
        xmlNode.setAttribute(PROPERTY_TYPE_ATTRIBUTE_NAME, propertyType);
        return xmlNode;
    }

    public Element getPropertyNode(Element e)
    {
        return (Element) e.getElementsByTagName(PROPERTY_NODE_NAME).item(0);
    }

    public String getPropertyName(Element e)
    {
        return getPropertyNode(e).getAttribute(PROPERTY_NAME_ATTRIBUTE_NAME);
    }

    public String getPropertyType(Element e)
    {
        return getPropertyNode(e).getAttribute(PROPERTY_TYPE_ATTRIBUTE_NAME);
    }

    public String getPropertyValue(Element e)
    {
        Element propertyNode = getPropertyNode(e);
        return (propertyNode != null) ? propertyNode.getAttribute(PROPERTY_VALUE_ATTRIBUTE_NAME) : null;
    }

    public Element createClassNode(String className, Document doc)
    {
        Element xmlNode = doc.createElement(CLASS_NODE_NAME);
        xmlNode.setAttribute(CLASS_NAME_ATTRIBUTE_NAME, className);
        return xmlNode;
    }

    public Element getClassNode(Element e)
    {
        return (Element) e.getElementsByTagName(CLASS_NODE_NAME).item(0);
    }

    public String getClassName(Element e)
    {
        return getClassNode(e).getAttribute(CLASS_NAME_ATTRIBUTE_NAME);
    }

    public Element createAttachmentNode(String filename, Document doc)
    {
        return createAttachmentNode(null, filename, null, doc);
    }

    public Element createAttachmentNode(byte[] content, String filename, String author, Document doc)
    {
        Element xmlNode = doc.createElement(ATTACHMENT_NODE_NAME);
        xmlNode.setAttribute(ATTACHMENT_FILANAME_ATTRIBUTE_NAME, filename);
        if (author != null) {
            xmlNode.setAttribute(ATTACHMENT_AUTHOR_ATTRIBUTE_NAME, author);
        }
        if (content != null) {
            xmlNode.setTextContent(new String(Base64.encodeBase64(content)));
        }
        return xmlNode;
    }

    public Element getAttachmentNode(Element e)
    {
        return (Element) e.getElementsByTagName(ATTACHMENT_NODE_NAME).item(0);
    }

    public byte[] getAttachmentContent(Element e)
    {
        try {
            return Base64.decodeBase64(getAttachmentNode(e).getTextContent().getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException e1) {
        } catch (DOMException e1) {
        }
        return new byte[0];
    }

    public String getAttachmentFilename(Element e)
    {
        return getAttachmentNode(e).getAttribute(ATTACHMENT_FILANAME_ATTRIBUTE_NAME);
    }

    public String getAttachmentAuthor(Element e)
    {
        return getAttachmentNode(e).getAttribute(ATTACHMENT_AUTHOR_ATTRIBUTE_NAME);
    }

    public Position loadPositionNode(Element e) throws XWikiException
    {
        Position position = new PositionImpl();
        position.fromXml((Element) e.getElementsByTagName(PositionImpl.NODE_NAME).item(0));
        return position;
    }
}
