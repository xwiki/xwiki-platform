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
import java.util.List;
import java.util.Objects;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xwiki.validator.ValidationError;
import org.xwiki.validator.ValidationError.Type;

/**
 * Implements {@link ErrorHandler} to stored found errors.
 *
 * @version $Id$
 */
public class XMLErrorHandler implements ErrorHandler
{
    /**
     * List of validation errors.
     */
    private List<ValidationError> errors = new ArrayList<>();

    /**
     * Clear the list of validation errors.
     */
    public void clear()
    {
        this.errors.clear();
    }

    @Override
    public void warning(SAXParseException e)
    {
        // I couldn't find a way to ignore this error by configuration of the validaor.  Instead, the error is
        // ignored by it's label.
        // TODO: see XWIKI-23198 for enabling this validation.
        if (!Objects.equals(e.getMessage(),
            "Trailing slash on void elements has no effect and interacts badly with unquoted attribute values."))
        {
            this.errors.add(new ValidationError(Type.WARNING, e));
        }
    }

    @Override
    public void error(SAXParseException e)
    {
        this.errors.add(new ValidationError(Type.ERROR, e));
    }

    @Override
    public void fatalError(SAXParseException e)
    {
        this.errors.add(new ValidationError(Type.FATAL, e));
    }

    /**
     * @return a list of validation errors.
     */
    public List<ValidationError> getErrors()
    {
        return this.errors;
    }

    /**
     * @param error an error
     */
    public void addError(ValidationError error)
    {
        this.errors.add(error);
    }
}
