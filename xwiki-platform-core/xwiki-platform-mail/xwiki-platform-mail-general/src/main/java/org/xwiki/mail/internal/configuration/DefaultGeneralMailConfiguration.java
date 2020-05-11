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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Gets the General Mail configuration. The configuration is checked in the following order:
 * <ul>
 *   <li>Look in {@code Mail.MailConfig} in the current wiki</li>
 *   <li>Look in {@code Mail.MailConfig} in the main wiki</li>
 *   <li>Look in the xwiki properties file</li>
 * </ul>
 *
 * @version $Id$
 * @since 12.4RC1
 */
@Component
@Singleton
public class DefaultGeneralMailConfiguration implements GeneralMailConfiguration
{
    private static final String OBFUSCATE_EMAIL_ADDRESSES = "obfuscate";

    /**
     * By default email addresses are not obfuscated.
     */
    private static final boolean DEFAULT_OBFUSCATE_EMAIL_ADDRESSES = false;

    /**
     * Prefix for configuration keys for the General Mail module.
     */
    private static final String PREFIX = "mail.general.";

    @Inject
    @Named("mailgeneral")
    private ConfigurationSource currentWikiMailConfigSource;

    @Inject
    @Named("mailgeneralmainwiki")
    private ConfigurationSource mainWikiMailConfigSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public boolean shouldObfuscateEmailAddresses()
    {
        Boolean obfuscateEmailAddresses =
            this.currentWikiMailConfigSource.getProperty(OBFUSCATE_EMAIL_ADDRESSES, Boolean.class);
        if (obfuscateEmailAddresses == null && !isMainWiki()) {
            obfuscateEmailAddresses =
                this.mainWikiMailConfigSource.getProperty(OBFUSCATE_EMAIL_ADDRESSES, Boolean.class);
        }

        if (obfuscateEmailAddresses == null) {
            obfuscateEmailAddresses = this.xwikiPropertiesSource.getProperty(PREFIX + OBFUSCATE_EMAIL_ADDRESSES,
                DEFAULT_OBFUSCATE_EMAIL_ADDRESSES);
        }

        return obfuscateEmailAddresses;
    }

    private boolean isMainWiki()
    {
        return this.wikiDescriptorManager.isMainWiki(this.wikiDescriptorManager.getCurrentWikiId());
    }
}
