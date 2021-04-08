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
import org.xwiki.model.reference.DocumentReference;

/**
 * Provides configuration from the {@code Mail.MailConfig} document in the current wiki.
 * If the {@code Mail.SendMailConfigClass} xobject exists in the {@code Mail.MailConfig} document then always use
 * configuration values from it and if it doesn't then use the passed default values (if a default value is passed).
 *
 * @version $Id$
 * @since 6.4M2
 */
@Component
@Named("mailsend")
@Singleton
public class SendMailConfigClassDocumentConfigurationSource
    extends AbstractSendMailConfigClassDocumentConfigurationSource
{
    @Override
    protected String getCacheId()
    {
        return "configuration.document.mail.send";
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(MAILCONFIG_REFERENCE, getCurrentWikiReference());
    }
}
