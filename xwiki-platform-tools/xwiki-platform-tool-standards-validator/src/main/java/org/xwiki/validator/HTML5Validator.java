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

import java.io.InputStream;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xwiki.validator.framework.XMLErrorHandler;

import nu.validator.validation.SimpleDocumentValidator;

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
    private final XMLErrorHandler errorHandler = new XMLErrorHandler();

    @Override
    public void setDocument(InputStream document)
    {
        this.document = document;
    }

    @Override
    public List<ValidationError> validate()
    {
        clear();

        // Don't enable the language detection as it can lead to too many false positives. See XWIKI-17776 for more.
        SimpleDocumentValidator validator = new SimpleDocumentValidator(true, true, false);

        String schemaUrl = "http://s.validator.nu/html5-all.rnc";

        InputSource source = new InputSource(this.document);
        try {
            validator.setUpMainSchema(schemaUrl, this.errorHandler);
            validator.setUpValidatorAndParsers(this.errorHandler, false, false);
            validator.checkHtmlInputSource(source);
        } catch (SAXException e) {
            // Ignore - Let XMLErrorHandler handle it
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this.errorHandler.getErrors();
    }

    @Override
    public List<ValidationError> getErrors()
    {
        return this.errorHandler.getErrors();
    }

    @Override
    public void clear()
    {
        this.errorHandler.clear();
    }

    @Override
    public String getName()
    {
        return "HTML5";
    }
}
