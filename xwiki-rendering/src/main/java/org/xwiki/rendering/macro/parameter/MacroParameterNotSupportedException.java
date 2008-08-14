package org.xwiki.rendering.macro.parameter;

/**
 * Encapsulate macro parameter error when using to get not not supported parameter in the current macro.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class MacroParameterNotSupportedException extends MacroParameterException
{
    /**
     * Class ID for serialization.
     */
    private static final long serialVersionUID = 7808925465855287882L;

    /**
     * {@inheritDoc}
     * 
     * @see Exception#Exception(String)
     */
    public MacroParameterNotSupportedException(String message)
    {
        super(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Exception#Exception(String, Throwable)
     */
    public MacroParameterNotSupportedException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
