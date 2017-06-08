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
package org.xwiki.test.escaping.framework;

import java.util.List;

import org.xwiki.validator.ValidationError;


/**
 * Error thrown in various escaping tests. Can handle a list of validation errors.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class EscapingError extends AssertionError
{
    /** Serial version ID. */
    private static final long serialVersionUID = 7784831403359592333L;

    /**
     * Create new EscapingError.
     * 
     * @param message error message
     */
    public EscapingError(String message)
    {
        super(message);
    }

    /**
     * Create new EscapingError.
     * 
     * @param cause error cause
     */
    public EscapingError(Throwable cause)
    {
        super(cause);
    }

    /**
     * Create new EscapingError listing a list of validation errors.
     * 
     * @param message error message
     * @param fileName file name that was tested
     * @param url URL to reproduce
     * @param errors list of validation errors
     */
    public EscapingError(String message, String fileName, String url, List<ValidationError> errors)
    {
        super(formatMessage(message, fileName, url, errors));
    }

    /**
     * Create new EscapingError listing a list of escaping errors.
     * 
     * @param message error message
     * @param errors list of escaping errors
     */
    public EscapingError(String message, List<EscapingError> errors)
    {
        super(formatMessageList(message, errors));
    }

    /**
     * Compose a nice error message from the given list of validation errors.
     * 
     * @param message error description
     * @param fileName file name that was tested
     * @param url URL to reproduce
     * @param errors list of validation errors
     * @return formatted message
     */
    public static final String formatMessage(String message, String fileName, String url, List<ValidationError> errors)
    {
        StringBuilder result = new StringBuilder(message == null ? "" : message);
        result.append("\n  Tested file: ").append(fileName);
        result.append("\n  URL: ").append(url);

        if (errors == null || errors.size() == 0) {
            result.append('\n');
            return result.toString();
        }
        StringBuilder fatalBuilder = new StringBuilder();
        StringBuilder errorBuilder = new StringBuilder();
        StringBuilder warningBuilder = new StringBuilder();
        final String format = "\n    line %4d  column %3d  %s";
        for (ValidationError error : errors) {
            String str = String.format(format, error.getLine(), error.getColumn(), error.toString());
            switch (error.getType()) {
                case FATAL:
                    fatalBuilder.append(str);
                    break;
                case ERROR:
                    errorBuilder.append(str);
                    break;
                case WARNING:
                    warningBuilder.append(str);
                    break;
                default:
                    throw new RuntimeException("Should not happen: Error type = " + error.getType());
            }
        }
        result.append("\n  List of validation errors:");
        result.append(fatalBuilder);
        result.append(errorBuilder);
        result.append(warningBuilder);
        result.append('\n');
        return result.toString();
    }

    /**
     * Compose a nice error message from the given list of escaping errors. Each escaping error might
     * contain a list of validation errors.
     * 
     * @param message error description
     * @param errors list of escaping errors
     * @return formatted message
     */
    public static String formatMessageList(String message, List<EscapingError> errors)
    {
        StringBuilder result = new StringBuilder(message == null ? "" : message + "\n");
        for (EscapingError error : errors) {
            result.append(error.getMessage());
        }
        return result.toString();
    }
}

