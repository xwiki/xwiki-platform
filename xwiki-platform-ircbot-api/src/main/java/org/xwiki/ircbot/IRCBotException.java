package org.xwiki.ircbot;

/**
 * Encapsulate a parsing error.
 *
 * @version $Id$
 */
public class IRCBotException extends Exception
{
    /**
     * Class ID for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new Exception with the specified detail message.
     *
     * @param message The detailed message. This can later be retrieved by the Throwable.getMessage() method.
     */
    public IRCBotException(String message)
    {
        super(message);
    }

    /**
     * Construct a new Exception with the specified detail message and cause.
     *
     * @param message The detailed message. This can later be retrieved by the Throwable.getMessage() method.
     * @param throwable the cause. This can be retrieved later by the Throwable.getCause() method. (A null value
     *        is permitted, and indicates that the cause is nonexistent or unknown)
     */
    public IRCBotException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
