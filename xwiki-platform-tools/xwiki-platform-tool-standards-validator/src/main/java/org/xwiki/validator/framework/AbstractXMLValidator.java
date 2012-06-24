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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xwiki.validator.ValidationError;
import org.xwiki.validator.Validator;
import org.xwiki.validator.ValidationError.Type;

/**
 * XML based standard validator.
 * 
 * @version $Id$
 */
public abstract class AbstractXMLValidator implements Validator
{
    /**
     * Document to be validated.
     */
    protected Document document;

    /**
     * Error handler.
     */
    private XMLErrorHandler errorHandler = new XMLErrorHandler();

    /**
     * XML document builder.
     */
    private DocumentBuilder documentBuilder;

    /**
     * Indicate if the XML itself is validated.
     */
    private boolean validateXML = true;

    /**
     * Constructor.
     */
    public AbstractXMLValidator()
    {
        this(true);
    }

    /**
     * Constructor.
     * 
     * @param validateXML indicate if the XML input should be validated.
     */
    public AbstractXMLValidator(boolean validateXML)
    {
        setValidateXML(validateXML);

        this.errorHandler = createXMLErrorHandler();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);

        try {
            this.documentBuilder = factory.newDocumentBuilder();
            this.documentBuilder.setEntityResolver(new XMLResourcesEntityResolver());
            this.documentBuilder.setErrorHandler(this.errorHandler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the error handler to use when parsing the xml content
     */
    protected XMLErrorHandler createXMLErrorHandler()
    {
        return new XMLErrorHandler();
    }

    /**
     * @param validateXML indicate if the XML input should be validated.
     */
    public void setValidateXML(boolean validateXML)
    {
        this.validateXML = validateXML;
    }

    @Override
    public void setDocument(InputStream document)
    {
        this.document = null;

        if (document != null) {
            try {
                clear();

                this.document = this.documentBuilder.parse(document);
            } catch (SAXException e) {
                // Ignore - Let XMLErrorHandler handle it
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<ValidationError> validate()
    {
        if (this.document == null) {
            return this.errorHandler.getErrors();
        }

        if (!this.validateXML) {
            this.errorHandler.clear();
        }

        validate(this.document);

        return this.errorHandler.getErrors();
    }

    /**
     * @param document the XML document
     */
    protected void validate(Document document)
    {
        // should be overridden
    }

    /**
     * @return validator results
     */
    public List<ValidationError> getErrors()
    {
        return this.errorHandler.getErrors();
    }

    /**
     * Clear the validator errors.
     */
    public void clear()
    {
        this.errorHandler.clear();
    }

    /**
     * @return the XML document
     */
    public Document getDocument()
    {
        return this.document;
    }

    /**
     * @return the XML error handler
     */
    protected XMLErrorHandler getErrorHandler()
    {
        return errorHandler;
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
        this.errorHandler.addError(new ValidationError(errorType, line, column, message));
    }
}
