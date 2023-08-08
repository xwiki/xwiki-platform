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

import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Provides configuration from the {@code Mail.MailConfig} document.
 * If the {@code Mail.GeneralMailConfigClass} xobject exists in the {@code Mail.MailConfig} document then always use
 * configuration values from it and if it doesn't then use the passed default values (if a default value is passed).
 *
 * @version $Id$
 * @since 12.4RC1
 */
public abstract class AbstractGeneralMailConfigClassDocumentConfigurationSource
    extends AbstractMailConfigClassDocumentConfigurationSource
{
    /**
     * The local reference of the Mail.SendMailConfigClass xclass.
     */
    public static final LocalDocumentReference GENERAL_MAILCONFIGCLASS_REFERENCE =
        new LocalDocumentReference(MAIL_SPACE, "GeneralMailConfigClass");

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return GENERAL_MAILCONFIGCLASS_REFERENCE;
    }

    /**
     * Clear the cache.
     *
     * @since 14.10.15
     * @since 15.5.2
     * @since 15.7RC1
     */
    void clearCache()
    {
        this.cache.removeAll();
    }
}
