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
package org.xwiki.component.logging;

import java.text.MessageFormat;

/**
 * Helper methods to help in implementation of Loggers.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public abstract class AbstractLogger implements Logger
{
    /**
     * Formats the message like {@code MessageFormat.format(String, Object...)} but also checks for Exceptions and
     * catches them as logging should be robust and not interfere with normal program flow. The Exception caught will be
     * passed to the loggers debug output.
     * 
     * @param message message in Formatter format syntax
     * @param objects Objects to fill in
     * @return the formatted String if possible, else the message and all objects concatenated.
     * @see MessageFormat
     */
    protected String formatMessage(String message, Object... objects)
    {
        try {
            return MessageFormat.format(message, objects);
        } catch (IllegalArgumentException e) {
            debug("Caught exception while formatting logging message: " + message, e);

            // Try to save the message for logging output and just append the passed objects instead
            if (objects != null) {
                StringBuffer sb = new StringBuffer(message);
                for (Object object : objects) {
                    if (object != null) {
                        sb.append(object);
                    } else {
                        sb.append("(null)");
                    }
                    sb.append(" ");
                }
                return sb.toString();
            }
            return message;
        }
    }
}
