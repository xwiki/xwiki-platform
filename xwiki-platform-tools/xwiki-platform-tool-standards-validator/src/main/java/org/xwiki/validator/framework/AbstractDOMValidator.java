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
package org.xwiki.validator.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.validator.ValidationError.Type;

/**
 * Various DOM utils.
 * 
 * @version $Id$
 */
public abstract class AbstractDOMValidator extends AbstractXMLValidator
{
    // XPATH

    /**
     * Catch All XPATH expression.
     */
    protected static final String XPATH_CATCHALL = "//";

    // Commons.

    /**
     * Submit.
     */
    protected static final String SUBMIT = "submit";

    /**
     * Image.
     */
    protected static final String IMAGE = "image";

    /**
     * Reset.
     */
    protected static final String RESET = "reset";

    /**
     * Button.
     */
    protected static final String BUTTON = "button";

    /**
     * mailto.
     */
    protected static final String MAILTO = "mailto:";

    /**
     * hidden.
     */
    protected static final String HIDDEN = "hidden";

    // Elements.

    /**
     * HTML element.
     */
    protected static final String ELEM_HTML = "html";

    /**
     * Body element.
     */
    protected static final String ELEM_BODY = "body";

    /**
     * Heading 1 element.
     */
    protected static final String ELEM_H1 = "h1";

    /**
     * Line break element.
     */
    protected static final String ELEM_BR = "br";

    /**
     * Bold element.
     */
    protected static final String ELEM_BOLD = "b";

    /**
     * Italics element.
     */
    protected static final String ELEM_ITALIC = "i";

    /**
     * Submit element.
     */
    protected static final String ELEM_SUBMIT = SUBMIT;

    /**
     * Frameset element.
     */
    protected static final String ELEM_FRAMESET = "frameset";

    /**
     * Frame element.
     */
    protected static final String ELEM_FRAME = "frame";

    /**
     * Iframe element.
     */
    protected static final String ELEM_IFRAME = "iframe";

    /**
     * Link element.
     */
    protected static final String ELEM_LINK = "a";

    /**
     * Input element.
     */
    protected static final String ELEM_INPUT = "input";

    /**
     * Image element.
     */
    protected static final String ELEM_IMG = "img";

    /**
     * Area element.
     */
    protected static final String ELEM_AREA = "area";

    /**
     * Table element.
     */
    protected static final String ELEM_TABLE = "table";

    /**
     * Table Header element.
     */
    protected static final String ELEM_TH = "th";

    /**
     * Form element.
     */
    protected static final String ELEM_FORM = "form";

    /**
     * Fieldset element.
     */
    protected static final String ELEM_FIELDSET = "fieldset";

    /**
     * Fieldset element.
     */
    protected static final String ELEM_META = "meta";

    // Attributes.

    /**
     * Type attribute.
     */
    protected static final String ATTR_TYPE = "type";

    /**
     * Type attribute.
     */
    protected static final String ATTR_ALT = "alt";

    /**
     * Href attribute.
     */
    protected static final String ATTR_HREF = "href";

    /**
     * Blur attribute.
     */
    protected static final String ATTR_BLUR = "onblur";

    /**
     * Change attribute.
     */
    protected static final String ATTR_CHANGE = "onchange";

    /**
     * Click attribute.
     */
    protected static final String ATTR_CLICK = "onclick";

    /**
     * Focus attribute.
     */
    protected static final String ATTR_FOCUS = "onfocus";

    /**
     * Load attribute.
     */
    protected static final String ATTR_LOAD = "onload";

    /**
     * Mouseover attribute.
     */
    protected static final String ATTR_MOUSEOVER = "onmouseover";

    /**
     * Select attribute.
     */
    protected static final String ATTR_SELECT = "onselect";

    /**
     * Submmit attribute.
     */
    protected static final String ATTR_SUBMIT = "onsubmit";

    /**
     * Unload attribute.
     */
    protected static final String ATTR_UNLOAD = "unload";

    /**
     * Accesskey attribute.
     */
    protected static final String ATTR_ACCESSKEY = "accesskey";

    /**
     * Scope attribute.
     */
    protected static final String ATTR_SCOPE = "scope";

    /**
     * ID attribute.
     */
    protected static final String ATTR_ID = "id";

    /**
     * Content attribute.
     */
    protected static final String ATTR_CONTENT = "content";

    /**
     * Charset attribute.
     */
    protected static final String ATTR_CHARSET = "charset";

    /**
     * XPath instance.
     */
    protected XPath xpath = XPathFactory.newInstance().newXPath();

    /**
     * Constructor.
     */
    public AbstractDOMValidator()
    {
        super();
    }

    /**
     * Constructor.
     * 
     * @param validateXML indicate if the XML input should be validated.
     */
    public AbstractDOMValidator(boolean validateXML)
    {
        super(validateXML);
    }

    /**
     * Asserts that a condition is false. If it isn't it puts an error message in the validation results.
     * 
     * @param errorType type of the error
     * @param message the message to add
     * @param condition condition to be checked
     */
    protected void assertFalse(Type errorType, String message, boolean condition)
    {
        if (condition) {
            // TODO: handle line/column
            addError(errorType, -1, -1, message);
        }
    }

    /**
     * Asserts that a condition is true. If it isn't it puts an error message in the validation results.
     * 
     * @param errorType type of the error
     * @param message the message to add
     * @param condition condition to be checked
     */
    protected void assertTrue(Type errorType, String message, boolean condition)
    {
        if (!condition) {
            // TODO: handle line/column
            addError(errorType, -1, -1, message);
        }
    }

    protected void assertEmpty(Type errorType, String message, String xpath)
    {
        assertTrue(errorType, message,
            ((NodeList) evaluate(this.document, xpath, XPathConstants.NODESET)).getLength() == 0);
    }

    // Dom utils

    /**
     * Check if the document contains the given element.
     * 
     * @param tagName element to search
     * @return true if the document contains the element, false otherwise
     */
    public boolean containsElement(String tagName)
    {
        return this.document.getElementsByTagName(tagName).getLength() > 0;
    }

    /**
     * Get a list of elements matching a given tag name in the document.
     * 
     * @param tagName tag name to search for
     * @return the list of matching elements
     */
    public NodeListIterable getElements(String tagName)
    {
        return new NodeListIterable(this.document.getElementsByTagName(tagName));
    }

    /**
     * Evaluate a XPATH string against a node.
     * 
     * @param node node to evaluate
     * @param exprString evaluation expression
     * @param returnType type of the results to return
     * @return the result of the xpath evaluation
     */
    public Object evaluate(Node node, String exprString, QName returnType)
    {
        try {
            XPathExpression expr = xpath.compile(exprString);
            return expr.evaluate(node, returnType);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get all the elements matching one of the given tags.
     * 
     * @param tagNames tag names to match
     * @return the list of matching tags
     */
    public NodeListIterable getElements(Collection<String> tagNames)
    {
        String exprString = XPATH_CATCHALL + StringUtils.join(tagNames, "|" + XPATH_CATCHALL);
        return new NodeListIterable((NodeList) evaluate(this.document, exprString, XPathConstants.NODESET));
    }

    /**
     * Check if an element has an child element with the given tag name.
     * 
     * @param element element to analyze
     * @param tagName tag name to search for
     * @return true if the element has an child element with the given tag name
     */
    public boolean hasChildElement(Node element, String tagName)
    {
        String exprString = XPATH_CATCHALL + tagName;
        return (Boolean) evaluate(element, exprString, XPathConstants.BOOLEAN);

    }

    /**
     * Get children of a given type.
     * 
     * @param element element to search in
     * @param tagName name of the tags to match
     * @return a list of matching tags
     */
    public NodeListIterable getChildren(Node element, String tagName)
    {
        String exprString = XPATH_CATCHALL + tagName;
        NodeList nodeList = (NodeList) evaluate(element, exprString, XPathConstants.NODESET);
        return new NodeListIterable(nodeList);
    }

    /**
     * Get the names of all the children elements of an element.
     * 
     * @param element parent element
     * @return the names of all the children elements of an element.
     */
    public List<String> getChildrenTagNames(Node element)
    {
        List<String> childrenTagNames = new ArrayList<String>();
        String exprString = XPATH_CATCHALL + "*";

        NodeListIterable children =
            new NodeListIterable((NodeList) evaluate(element, exprString, XPathConstants.NODESET));
        for (Node child : children) {
            childrenTagNames.add(child.getNodeName());
        }

        return childrenTagNames;
    }

    /**
     * Check if an element has the given attribute.
     * 
     * @param element element to analyze
     * @param attributeName name of the attribute to search
     * @return true if the element has the given attribute, false otherwise
     */
    public static boolean hasAttribute(Node element, String attributeName)
    {
        return getAttributeNames(element).contains(attributeName);
    }

    /**
     * Get the names of all the attribute of an element.
     * 
     * @param element element to analyze
     * @return the names of all the attribute of the given element
     */
    public static List<String> getAttributeNames(Node element)
    {
        List<String> attributeNames = new ArrayList<String>();
        NamedNodeMap attributes = element.getAttributes();

        for (int i = 0; i < attributes.getLength(); i++) {
            attributeNames.add(attributes.item(i).getNodeName());
        }

        return attributeNames;
    }

    /**
     * Get the value of an element attribute.
     * 
     * @param element element to analyze
     * @param attributeName name of the attribute to search
     * @return the value of the given attribute
     */
    public static String getAttributeValue(Node element, String attributeName)
    {
        NamedNodeMap attributes = element.getAttributes();

        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (attribute.getNodeName().equals(attributeName)) {
                return attribute.getNodeValue();
            }
        }

        return null;
    }

    /**
     * Retrieve a list of values of an attribute for a list of elements.
     * 
     * @param elements the list of elements to get the attribute from
     * @param attributeName name of the attribute to retrieve the value from
     * @return the list of values of the given attribute for all the elements in the given element list
     */
    public static List<String> getAttributeValues(NodeListIterable elements, String attributeName)
    {
        return getAttributeValues(elements.getNodeList(), attributeName);
    }

    /**
     * Retrieve a list of values of an attribute for a list of nodes.
     * 
     * @param nodes the list of nodes to get the attribute from
     * @param attributeName name of the attribute to retrieve the value from
     * @return the list of values of the given attribute for all the elements in the given element list
     */
    public static List<String> getAttributeValues(NodeList nodes, String attributeName)
    {
        List<String> results = new ArrayList<String>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node element = nodes.item(i);
            String value = getAttributeValue(element, attributeName);

            if (value != null) {
                results.add(value);
            }
        }

        return results;
    }

    /**
     * @param tagName name of the tag to match
     * @return The first element found for the given tag name in the XHTML document.
     */
    public Node getElement(String tagName)
    {
        return getElements(tagName).getNodeList().item(0);
    }
}
