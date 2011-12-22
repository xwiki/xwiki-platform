package org.xwiki.security;

/**
 * Thrown by the RightService when an access to be denied.
 *
 * @version $Id$
 */
public class AccessDeniedException extends RightServiceException
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public AccessDeniedException()
    {
    }

    /**
     * @param message a message with details about the underlying cause.
     */
    public AccessDeniedException(String message)
    {
        super(message);
    }

    /**
     * @param message a message with details about the underlying cause.
     * @param t a Throwable providing details about the underlying cause.
     */
    public AccessDeniedException(String message, Throwable t)
    {
        super(message, t);
    }

    /**
     * @param t a Throwable providing details about the underlying cause.
     */
    public AccessDeniedException(Throwable t)
    {
        super(t);
    }
}
