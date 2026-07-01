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
package org.xwiki.template;

import org.slf4j.Marker;
import org.xwiki.logging.AbstractMessageException;
import org.xwiki.logging.Message;

/**
 * Exception thrown when a template requirement is not met.
 * 
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.1
 */
public class TemplateRequirementException extends AbstractMessageException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * @param translationKey the key used to find the translation
     * @param defaultMessage the default message pattern, support SLF4J syntax for parameters
     * @param arguments the arguments to insert in the message. If the last argument is a {@link Throwable}, it's
     *            extracted as the cause.
     * @see Message#Message
     */
    public TemplateRequirementException(String translationKey, String defaultMessage, Object... arguments)
    {
        super(translationKey, defaultMessage, arguments);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * 
     * @param marker the log marker
     * @param defaultMessage the default message pattern, support SLF4J syntax for parameters
     * @param argumentArray the event arguments to insert in the message
     * @param cause the throwable associated to the event
     * @see Message#Message(Marker, String, Object[], Throwable)
     */
    public TemplateRequirementException(Marker marker, String defaultMessage, Object[] argumentArray, Throwable cause)
    {
        super(marker, defaultMessage, argumentArray, cause);
    }
}
