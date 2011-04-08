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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.validator.ValidationError;
import org.xwiki.validator.Validator;
import org.xwiki.validator.ValidationError.Type;

/**
 * Various DOM utils.
 * 
 * @version $Id$
 */
public abstract class AbstractDOMValidator implements Validator
{
    // Commons.

    /**
     * Submit.
     */
    protected static final String SUBMIT = "submit";

    // Elements.

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
     * Link element.
     */
    protected static final String ELEM_LINK = "a";

    // Attributes.

    /**
     * Href attribute.
     */
    protected static final String ATTR_HREF = "href";

    /**
     * Blur attribute.
     */
    protected static final String ATTR_BLUR = "blur";

    /**
     * Change attribute.
     */
    protected static final String ATTR_CHANGE = "change";

    /**
     * Click attribute.
     */
    protected static final String ATTR_CLICK = "click";

    /**
     * Focus attribute.
     */
    protected static final String ATTR_FOCUS = "focus";

    /**
     * Load attribute.
     */
    protected static final String ATTR_LOAD = "load";

    /**
     * Mouseover attribute.
     */
    protected static final String ATTR_MOUSEOVER = "mouseover";

    /**
     * Select attribute.
     */
    protected static final String ATTR_SELECT = "select";

    /**
     * Submmit attribute.
     */
    protected static final String ATTR_SUBMIT = SUBMIT;

    /**
     * Unload attribute.
     */
    protected static final String ATTR_UNLOAD = "unload";

    /**
     * XPath instance.
     */
    protected XPath xpath = XPathFactory.newInstance().newXPath();

    /**
     * Document to be validated.
     */
    protected Document document;
    
    /**
     * XML document builder.
     */
    private DocumentBuilder documentBuilder;

    /**
     * Results of the validation.
     */
    private List<ValidationError> errors = new ArrayList<ValidationError>();

    /**
     * Constructor.
     *  
     */
    public AbstractDOMValidator()
    {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = docBuilderFactory.newDocumentBuilder();
            documentBuilder.setEntityResolver(new XMLResourcesEntityResolver());            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 
     * {@inheritDoc}
     * 
     * @see org.xwiki.validator.Validator#setDocument(java.io.InputStream)
     */
    public void setDocument(InputStream document)
    {
        try {
            clear();
            this.document = documentBuilder.parse(document);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return validator results
     */
    public List<ValidationError> getErrors()
    {
        return errors;
    }

    /**
     * Clear the validator errors.
     */
    public void clear()
    {
        errors.clear();
    }

    /**
     * Add an error message to the list.
     * 
     * @param errorType type of the error
     * @param line line where the error occurred
     * @param column where the error occurred
     * @param message the message to add
     */
    protected void addError(Type errorType, int line, int column, String message)
    {
        errors.add(new ValidationError(errorType, line, column, message));
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

    // Dom utils

    /**
     * Check if the document contains the given element.
     * 
     * @param tagName element to search
     * @return true if the document contains the element, false otherwise
     */
    public boolean containsElement(String tagName)
    {
        return document.getElementsByTagName(tagName).getLength() > 0;
    }

    /**
     * Get a list of elements matching a given tag name in the document.
     * 
     * @param tagName tag name to search for
     * @return the list of matching elements
     */
    public NodeListIterable getElements(String tagName)
    {
        return new NodeListIterable(document.getElementsByTagName(tagName));
    }

    /**
     * Get all the elements matching one of the given tags.
     * 
     * @param tagNames tag names to match
     * @return the list of matching tags
     */
    public NodeListIterable getElements(Collection<String> tagNames)
    {
        String exprString = StringUtils.join(tagNames, "|//");

        try {
            XPathExpression expr = xpath.compile(exprString);
            return new NodeListIterable((NodeList) expr.evaluate(document, XPathConstants.NODESET));
        } catch (XPathExpressionException e) {
            // This cannot
        }

        return null;
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
        String exprString = "//submit";
        
        try {
            XPathExpression expr = xpath.compile(exprString);
            return (Boolean) expr.evaluate(element, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
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
        String exprString = "//*";
        
        try {
            XPathExpression expr = xpath.compile(exprString);
            NodeListIterable children = new NodeListIterable((NodeList) expr.evaluate(element, XPathConstants.NODESET));
            for (Node child : children) {
                childrenTagNames.add(child.getNodeName());
            }
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
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
}
