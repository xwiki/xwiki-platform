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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiException;

/**
 * Provides configuration from the Mail.SendMailConfigClass document in the current wiki.
 * If the Mail.SendMailConfigClass xobject exists in the Mail.MailConfig document then always use configuration
 * values from it and if it doesn't then use the passed default values (if a default value is passed).
 *
 * @version $Id$
 * @since 6.4M2
 */
@Component
@Named("mailsend")
@Singleton
public class SendMailConfigClassDocumentConfigurationSource extends AbstractDocumentConfigurationSource
{
    private static final String MAIL_SPACE = "Mail";

    /**
     * The local reference of the Mail.SendMailConfigClass xclass.
     */
    private static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(MAIL_SPACE, "SendMailConfigClass");

    /**
     * The local reference of the Mail.MailConfig document.
     */
    private static final LocalDocumentReference DOC_REFERENCE =
        new LocalDocumentReference(MAIL_SPACE, "MailConfig");

    @Override
    protected String getCacheId()
    {
        return "configuration.document.mail.send";
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(DOC_REFERENCE, getCurrentWikiReference());
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return CLASS_REFERENCE;
    }

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        T result;
        if (defaultValue != null) {
            try {
                if (getBaseObject() == null) {
                    // No Mail.SendMailConfigClass xobject in the Mail.MailConfig document, use the default value!
                    result = super.getProperty(key, defaultValue);
                } else {
                    // A Mail.SendMailConfigClass xobject exists in the Mail.MailConfig document, always use the
                    // value from it.
                    result = super.getProperty(key, (Class<? extends T>) defaultValue.getClass());
                }
            } catch (XWikiException e) {
                this.logger.error("Failed to access configuration property [{}]", key, e);
                result = null;
            }
        } else {
            result = super.getProperty(key);
        }
        return result;
    }
}
