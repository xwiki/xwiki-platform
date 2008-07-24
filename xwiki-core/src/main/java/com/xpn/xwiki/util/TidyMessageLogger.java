package com.xpn.xwiki.util;

import org.apache.commons.logging.Log;
import org.w3c.tidy.TidyMessage;
import org.w3c.tidy.TidyMessageListener;
import org.w3c.tidy.TidyMessage.Level;

/**
 * Utility class for logging JTidy messages.
 */
public class TidyMessageLogger implements TidyMessageListener
{
    private final Log log;

    public TidyMessageLogger(Log log)
    {
        this.log = log;
    }

    /**
     * {@inheritDoc}
     * 
     * @see TidyMessageListener#messageReceived(TidyMessage)
     */
    public void messageReceived(TidyMessage message)
    {
        String text =
            "line " + String.valueOf(message.getLine()) + " column " + String.valueOf(message.getColumn()) + " - "
                + message.getMessage();
        Level level = message.getLevel();
        if (level.equals(Level.ERROR)) {
            log.error(text);
        } else if (level.equals(Level.INFO)) {
            log.info(text);
        } else if (level.equals(Level.SUMMARY)) {
            log.info(text);
        } else if (level.equals(Level.WARNING)) {
            log.warn(text);
        }
    }
}
