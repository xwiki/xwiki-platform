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

import org.xml.sax.SAXParseException;

/**
 * Validation error.
 * 
 * @version $Id$
 */
public class ValidationError
{
    /**
     * Error types.
     */
    public enum Type
    {
        /**
         * Warning type.
         */
        WARNING,
        
        /**
         * Error type. 
         */
        ERROR,
        
        /**
         * Fatal type.
         */
        FATAL
    }

    /**
     * Type of the error.
     */
    private final Type type;

    /**
     * Line where the error occurred.
     */
    private final int line;

    /**
     * Column where the error occurred.
     */
    private final int column;

    /**
     * Message of the error.
     */
    private final String message;

    /**
     * Constructor.
     * 
     * @param type error type
     * @param line line where the error occurred
     * @param column column where the error occurred
     * @param message message of the error
     */
    public ValidationError(Type type, int line, int column, String message)
    {
        this.type = type;
        this.line = line;
        this.column = column;
        this.message = message;
    }

    /**
     * Constructor.
     * 
     * @param type error type
     * @param e source exception 
     */
    public ValidationError(Type type, SAXParseException e)
    {
        this.type = type;
        this.line = e.getLineNumber();
        this.column = e.getColumnNumber();
        this.message = e.getMessage();
    }

    /** 
     * @return error type
     */
    public Type getType()
    {
        return this.type;
    }

    /**
     * @return line where the error occurred
     */
    public int getLine()
    {
        return this.line;
    }

    /** 
     * @return column where the error occurred
     */
    public int getColumn()
    {
        return this.column;
    }

    /**
     * @return message of the error
     */
    public String getMessage()
    {
        return this.message;
    }
    
    /**
     * @return the error in a human readable format
     */
    @Override
    public String toString()
    {
        String prefix;
        
        if (getType().equals(Type.ERROR)) {
            prefix = "ERROR: ";
        } else if (getType().equals(Type.WARNING)) {
            prefix = "WARNING: ";
        } else {
            prefix = "FATAL: ";
        }
        
        return prefix + this.message;
    }
}
