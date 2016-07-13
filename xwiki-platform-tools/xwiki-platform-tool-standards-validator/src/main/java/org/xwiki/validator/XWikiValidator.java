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
package org.xwiki.validator;

import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.xwiki.validator.ValidationError.Type;
import org.xwiki.validator.framework.AbstractDOMValidator;
import org.xwiki.validator.framework.XMLResourcesEntityResolver;

import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.dom.HtmlDocumentBuilder;

/**
 * Validator allowing to validate (X)HTML content against some XWiki rules.
 * 
 * @version $Id$
 */
public class XWikiValidator extends AbstractDOMValidator
{
    /**
     * Constructor.
     */
    public XWikiValidator()
    {
        setValidateXML(false);

        this.errorHandler = createXMLErrorHandler();

        try {
            // Use the HTML5 Document builder (from the 'nu' project) instead of the default DOM builder that fails
            // with HTML 5
            this.documentBuilder = new HtmlDocumentBuilder(XmlViolationPolicy.ALTER_INFOSET);
            this.documentBuilder.setEntityResolver(new XMLResourcesEntityResolver());
            this.documentBuilder.setErrorHandler(this.errorHandler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void validate(Document document)
    {
        validateFailingMacros();
    }

    /**
     * Check if there is any rendering error in the generated XHTML.
     */
    public void validateFailingMacros()
    {
        String exprString = "//*[@class='xwikirenderingerror']";
        assertFalse(Type.ERROR, "Found rendering error", (Boolean) evaluate(this.document, exprString,
            XPathConstants.BOOLEAN));
    }

    @Override
    public String getName()
    {
        return "XWiki";
    }
}
