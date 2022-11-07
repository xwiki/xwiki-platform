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
package org.xwiki.attachment;

/**
 * @version $Id$
 * @since 14.10RC1
 */
public class AttachmentValidationException extends Exception
{
    private static final long serialVersionUID = 1L;

    private final int httpStatus;

    private final String translationKey;

    private final String contextMessage;

    /**
     * Construct a new exception with the specified detail message.
     *
     * @param message The detailed message. This can later be retrieved by the {@link #getMessage()} method
     * @param httpStatus the http status to return when this exception is caught
     * @param translationKey the translation key to use when localizing this error
     * @param contextMessage the context message to use for this error
     */
    public AttachmentValidationException(String message, int httpStatus, String translationKey, String contextMessage)
    {
        super(message);
        this.httpStatus = httpStatus;
        this.translationKey = translationKey;
        this.contextMessage = contextMessage;
    }

    /**
     * @return the http status to return when this exception is caught
     */
    public int getHttpStatus()
    {
        return this.httpStatus;
    }

    /**
     * @return the translation key to use when localizing this error
     */
    public String getTranslationKey()
    {
        return this.translationKey;
    }

    /**
     * @return the context message to use for this error
     */
    public String getContextMessage()
    {
        return this.contextMessage;
    }
}
