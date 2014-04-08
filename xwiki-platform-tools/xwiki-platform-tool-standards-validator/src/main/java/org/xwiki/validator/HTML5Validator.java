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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.validator.framework.XMLErrorHandler;

import nu.validator.htmlparser.common.DoctypeExpectation;
import nu.validator.htmlparser.common.Heuristics;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;

/**
 * Validate HTML5 code of the provided input.
 *
 * @since 6.0RC1
 * @version $Id$
 */
public class HTML5Validator implements Validator
{
    private InputStream document;

    /**
     * Error handler.
     */
    private XMLErrorHandler errorHandler = new XMLErrorHandler();

    @Override
    public void setDocument(InputStream document)
    {
        this.document = document;
    }

    @Override
    public List<ValidationError> validate()
    {
        clear();
        HtmlParser parser = new HtmlParser();
        InputSource source = new InputSource(document);
        parser.setContentHandler(new DefaultHandler());
        parser.setErrorHandler(errorHandler);

        parser.setCommentPolicy(XmlViolationPolicy.ALLOW);
        parser.setContentNonXmlCharPolicy(XmlViolationPolicy.ALLOW);
        parser.setContentSpacePolicy(XmlViolationPolicy.ALTER_INFOSET);
        parser.setNamePolicy(XmlViolationPolicy.ALLOW);
        parser.setStreamabilityViolationPolicy(XmlViolationPolicy.FATAL);
        parser.setXmlnsPolicy(XmlViolationPolicy.ALTER_INFOSET);
        parser.setMappingLangToXmlLang(true);
        parser.setHtml4ModeCompatibleWithXhtml1Schemata(true);
        parser.setDoctypeExpectation(DoctypeExpectation.HTML);
        parser.setHeuristics(Heuristics.ALL);
        parser.setNamePolicy(XmlViolationPolicy.ALLOW);
        parser.setMappingLangToXmlLang(true);

        try {
            parser.parse(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            // Ignore - Let XMLErrorHandler handle it
        }
        return errorHandler.getErrors();
    }

    @Override
    public List<ValidationError> getErrors()
    {
        return errorHandler.getErrors();
    }

    @Override
    public void clear()
    {
        errorHandler.clear();
    }

    @Override
    public String getName()
    {
        return "HTML5";
    }
}
