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
package org.xwiki.annotation.internal.renderer;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.HTMLUtils;

/**
 * Helper for transforming an HTML content into valid XHTML and getting it's text content.
 * 
 * @version $Id$
 */
@Component(roles = HTMLTextExtracter.class)
@Singleton
public class HTMLTextExtracter implements Initializable
{
    @Inject
    private HTMLCleaner htmlCleaner;

    /**
     * Helper object for manipulating DOM Level 3 Load and Save APIs.
     **/
    private DOMImplementationLS lsImpl;

    private Map<String, String> htmlCleanerParametersMap;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.lsImpl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
        } catch (Exception exception) {
            throw new InitializationException("Failed to initialize DOM Level 3 Load and Save APIs.", exception);
        }
        htmlCleanerParametersMap = new HashMap<>();
        // We need to parse the clean HTML as XML later and we don't want to resolve the entity references from the DTD.
        htmlCleanerParametersMap.put(HTMLCleanerConfiguration.USE_CHARACTER_REFERENCES, Boolean.toString(true));
    }

    /**
     * Parse and clean a HTML given as string and compute it's text content.
     *
     * @param html an HTML as string
     * @return text content of the HTML document
     */
    public String getTextContent(String html)
    {
        String fullContent = "";
        Document htmlDoc = parseHTML(html);
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            NodeList textNodes = (NodeList) xPath.compile("//text()").evaluate(htmlDoc, XPathConstants.NODESET);
            Node node;
            for (int i = 0; i < textNodes.getLength(); i++) {
                node = textNodes.item(i);
                String textContent = node.getTextContent();
                fullContent = fullContent.concat(textContent);
            }
        } catch (XPathExpressionException e) {
            return fullContent;
        }
        return fullContent;
    }

    private Document parseHTML(String html)
    {
        // We need to clean the HTML because it may have been generated with the HTML macro using clean=false.
        return parseXML(cleanHTML(html));
    }

    private String cleanHTML(String html)
    {
        HTMLCleanerConfiguration config = this.htmlCleaner.getDefaultConfiguration();
        config.setParameters(htmlCleanerParametersMap);
        Document htmlDoc = this.htmlCleaner.clean(new StringReader(wrap(html)), config);
        // We serialize and parse again the HTML as XML because the HTML Cleaner doesn't handle entity and character
        // references very well: they all end up as plain text (they are included in the value returned by
        // Node#getNodeValue()).
        return HTMLUtils.toString(htmlDoc);
    }

    private Document parseXML(String xml)
    {
        LSInput input = this.lsImpl.createLSInput();
        input.setStringData(xml);
        return XMLUtils.parse(input);
    }

    private String wrap(String fragment)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html>"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head></head><body>" + fragment + "</body></html>";
    }
}
