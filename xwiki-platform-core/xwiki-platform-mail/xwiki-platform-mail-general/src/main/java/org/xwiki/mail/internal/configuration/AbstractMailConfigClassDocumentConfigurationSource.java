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
package org.xwiki.mail.internal.configuration;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiException;

/**
 * Provides configuration from the {@code Mail.MailConfig} document.
 *
 * @version $Id$
 * @since 12.4RC1
 */
public abstract class AbstractMailConfigClassDocumentConfigurationSource
    extends AbstractDocumentConfigurationSource
{
    /**
     * The space where the mail pages are located.
     */
    public static final String MAIL_SPACE = "Mail";

    /**
     * The local reference of the Mail.MailConfig document.
     */
    public static final LocalDocumentReference MAILCONFIG_REFERENCE =
        new LocalDocumentReference(MAIL_SPACE, "MailConfig");

    @Override
    protected <T> T getPropertyInternal(String key, T defaultValue)
    {
        T result;
        if (defaultValue != null) {
            try {
                if (getBaseObject() == null) {
                    // No Mail.GeneralMailConfigClass xobject in the Mail.MailConfig document, use the default value!
                    result = super.getPropertyInternal(key, defaultValue);
                } else {
                    // A Mail.GeneralMailConfigClass xobject exists in the Mail.MailConfig document, always use the
                    // value from it.
                    result = super.getPropertyInternal(key, (Class<? extends T>) defaultValue.getClass());
                }
            } catch (XWikiException e) {
                this.logger.warn("Failed to access configuration property [{}]. Returning null. Root cause: [{}]", key,
                    ExceptionUtils.getRootCauseMessage(e));
                result = null;
            }
        } else {
            result = super.getPropertyInternal(key);
        }
        return result;
    }
}
