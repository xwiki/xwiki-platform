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
package org.xwiki.rendering.parser.xwiki10;

/**
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class FilterException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * The file that caused the ParseException..
     */
    private String fileName;

    /**
     * Line number where the parse exception occurred.
     */
    private int lineNumber;

    /**
     * Construct a new ParseException with the specified detail message.
     * 
     * @param message The detailed message. This can later be retrieved by the Throwable.getMessage() method.
     */
    public FilterException(String message)
    {
        this(null, message, null, -1);
    }

    /**
     * Construct a new ParseException with the specified detail message and cause.
     * 
     * @param message The detailed message. This can later be retrieved by the Throwable.getMessage() method.
     * @param e the cause. This can be retrieved later by the Throwable.getCause() method. (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     */
    public FilterException(String message, Exception e)
    {
        this(e, message, null, -1);
    }

    /**
     * Constructs a new exception with the specified cause. The error message is (cause == null ? null :
     * cause.toString() ).
     * 
     * @param e the cause. This can be retrieved later by the Throwable.getCause() method. (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     */
    public FilterException(Exception e)
    {
        this(e, null, null, -1);
    }

    /**
     * Construct a new ParseException with the specified cause, filename and linenumber.
     * 
     * @param e the cause. This can be retrieved later by the Throwable.getCause() method. (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     * @param file Name of a file that couldn't be parsed. This can later be retrieved by the getFileName() method.
     * @param line The line number where the parsing failed. This can later be retrieved by the getLineNumber() method.
     */
    public FilterException(Exception e, String file, int line)
    {
        this(e, null, file, line);
    }

    /**
     * Construct a new ParseException with the specified cause, detail message, filename and linenumber.
     * 
     * @param e the cause. This can be retrieved later by the Throwable.getCause() method. (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     * @param message The detailed message. This can later be retrieved by the Throwable.getMessage() method.
     * @param file Name of a file that couldn't be parsed. This can later be retrieved by the getFileName() method.
     * @param line The line number where the parsing failed. This can later be retrieved by the getLineNumber() method.
     */
    public FilterException(Exception e, String message, String file, int line)
    {
        super((message == null) ? ((e == null) ? null : e.getMessage()) : message, e);

        this.fileName = file;
        this.lineNumber = line;
    }

    /**
     * Returns the file that caused the ParseException.
     * 
     * @return the file that caused the ParseException.
     */
    public String getFileName()
    {
        return fileName;
    }

    /**
     * Returns the line number where the ParseException ocurred.
     * 
     * @return the line number.
     */
    public int getLineNumber()
    {
        return lineNumber;
    }
}
