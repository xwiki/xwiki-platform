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
package org.xwiki.mail.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.mail.MailStorageConfiguration;

/**
 * Look for storage configuration first in {@code Mail.MailConfig} and then in {@code xwiki.properties}.
 *
 * @version $Id$
 * @since 6.4.1
 */
@Component
@Singleton
public class DefaultMailStorageConfiguration implements MailStorageConfiguration
{
    /**
     * Prefix for configuration keys for the Mail Sender Storage module.
     */
    private static final String PREFIX = "mail.sender.database.";

    private static final String DISCARD_SUCCESS_STATUSES = "discardSuccessStatuses";

    @Inject
    @Named("mailsend")
    private ConfigurationSource mailConfigSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Override
    public boolean discardSuccessStatuses()
    {
        // XWiki treats boolean as integers
        Integer discardSuccessStatuses = this.mailConfigSource.getProperty(DISCARD_SUCCESS_STATUSES);

        if (discardSuccessStatuses == null) {
            discardSuccessStatuses = this.xwikiPropertiesSource.getProperty(PREFIX + DISCARD_SUCCESS_STATUSES, 1);
        }

        return (discardSuccessStatuses == 1);
    }
}
