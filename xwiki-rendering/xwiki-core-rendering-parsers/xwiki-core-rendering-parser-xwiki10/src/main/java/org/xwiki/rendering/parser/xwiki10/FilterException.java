package org.xwiki.rendering.parser.xwiki10;

public class FilterException extends Exception
{
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
