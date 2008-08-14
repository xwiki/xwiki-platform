package org.xwiki.rendering.macro.parameter;

import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * Encapsulate macro parameter error.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class MacroParameterException extends MacroExecutionException
{
    /**
     * Class ID for serialization.
     */
    private static final long serialVersionUID = -6486465521283310564L;

    /**
     * {@inheritDoc}
     * 
     * @see Exception#Exception(String)
     */
    public MacroParameterException(String message)
    {
        super(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Exception#Exception(String, Throwable)
     */
    public MacroParameterException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
