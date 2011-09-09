package com.xpn.xwiki.util;

import org.slf4j.Logger;
import org.w3c.tidy.TidyMessage;
import org.w3c.tidy.TidyMessage.Level;
import org.w3c.tidy.TidyMessageListener;

/**
 * Utility class for logging JTidy messages.
 */
public class TidyMessageLogger implements TidyMessageListener
{
    private final Logger logger;

    public TidyMessageLogger(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public void messageReceived(TidyMessage message)
    {
        String text =
            "line " + String.valueOf(message.getLine()) + " column " + String.valueOf(message.getColumn()) + " - "
                + message.getMessage();
        Level level = message.getLevel();
        if (level.equals(Level.ERROR)) {
            this.logger.error(text);
        } else if (level.equals(Level.INFO)) {
            this.logger.info(text);
        } else if (level.equals(Level.SUMMARY)) {
            this.logger.info(text);
        } else if (level.equals(Level.WARNING)) {
            this.logger.warn(text);
        }
    }
}
