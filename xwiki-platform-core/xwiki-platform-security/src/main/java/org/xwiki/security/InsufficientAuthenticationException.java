package org.xwiki.security;

/**
 * Thrown by the RightService when an access is check while the current user is not appropriately identified.
 *
 * @version $Id$
 */
public class InsufficientAuthenticationException extends RightServiceException
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public InsufficientAuthenticationException()
    {
    }

    /**
     * @param t a Throwable providing details about the underlying cause.
     */
    public InsufficientAuthenticationException(Throwable t)
    {
        super(t);
    }
}
