package org.xwiki.rendering.macro.parameter;

/**
 * Encapsulate macro parameter error when can't found a required parameter.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class MacroParameterRequiredException extends MacroParameterException
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
    public MacroParameterRequiredException(String message)
    {
        super(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Exception#Exception(String, Throwable)
     */
    public MacroParameterRequiredException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
