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
