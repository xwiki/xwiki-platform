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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xwiki.validator.framework.XMLErrorHandler;
import org.xwiki.validator.framework.XMLResourcesEntityResolver;

/**
 * Validate provided input.
 * 
 * @version $Id$
 */
public class XHTMLValidator implements Validator
{
    /**
     * Error handler.
     */
    private XMLErrorHandler errorHandler = new XMLErrorHandler();

    /**
     * Document to validate.
     */
    private InputStream document;

    /**
     * XML Document builder.
     */
    private DocumentBuilder documentBuilder;

    /**
     * Constructor.
     */
    public XHTMLValidator()
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);

        try {
            this.documentBuilder = factory.newDocumentBuilder();
            this.documentBuilder.setEntityResolver(new XMLResourcesEntityResolver());
            this.documentBuilder.setErrorHandler(this.errorHandler);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.validator.Validator#setDocument(java.io.InputStream)
     */
    public void setDocument(InputStream document)
    {
        this.document = document;
    }

    /**
     * Validate the given XTML document against its DTD.
     * 
     * @return list of errors found during validation
     */
    public List<ValidationError> validate()
    {
        try {
            this.errorHandler.clear();
            this.documentBuilder.parse(this.document);
        } catch (SAXException e) {
            // Ignore - Let XMLErrorHandler handle it
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this.errorHandler.getErrors();
    }

    /**
     * @return validation errors
     */
    public List<ValidationError> getErrors()
    {
        return this.errorHandler.getErrors();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.validator.Validator#clear()
     */
    public void clear()
    {
        this.errorHandler.clear();
    }
}
