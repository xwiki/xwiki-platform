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
package org.xwiki.attachment.validation;

import java.util.List;

/**
 * Exception thrown in case of error while validating an attachment. This exception also provides additional accessors
 * to localize the error messages.
 *
 * @version $Id$
 * @since 14.10
 */
public class AttachmentValidationException extends Exception
{
    private static final long serialVersionUID = 1L;

    private final int httpStatus;

    private final String translationKey;

    private final List<Object> translationParameters;

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
        this.translationParameters = List.of();
    }

    /**
     * Construct a new exception with the specific error message, the http status corresponding to this error, a
     * translation key to localization this error and its parameters.
     *
     * @param message The detailed message. This can later be retrieved by the {@link #getMessage()} method
     * @param httpStatus the http status to return when this exception is caught
     * @param translationKey the translation key to use when localizing this error
     * @param translationParameters the translation parameters to use when localizing this error
     * @param contextMessage the context message to use for this error
     */
    public AttachmentValidationException(String message, int httpStatus, String translationKey,
        List<Object> translationParameters, String contextMessage)
    {
        super(message);
        this.httpStatus = httpStatus;
        this.translationKey = translationKey;
        this.translationParameters = translationParameters;
        this.contextMessage = contextMessage;
    }

    /**
     * Construct a new exception with a message, an http status and a translation key.
     *
     * @param message the error message
     * @param httpStatus the http status
     * @param translationKey the translation key
     */
    public AttachmentValidationException(String message, int httpStatus, String translationKey)
    {
        this(message, httpStatus, translationKey, List.of(), null);
    }

    /**
     * Contruct a new exception with a message and a cause, plus an http status and a translation key.
     *
     * @param message the error message
     * @param cause the exception cause
     * @param httpStatus the http status
     * @param translationKey the translation key
     */
    public AttachmentValidationException(String message, Throwable cause, int httpStatus, String translationKey)
    {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.translationKey = translationKey;
        this.contextMessage = null;
        this.translationParameters = List.of();
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

    /**
     * @return the parameters to use when translating this message
     */
    public List<Object> getTranslationParameters()
    {
        return this.translationParameters;
    }
}
