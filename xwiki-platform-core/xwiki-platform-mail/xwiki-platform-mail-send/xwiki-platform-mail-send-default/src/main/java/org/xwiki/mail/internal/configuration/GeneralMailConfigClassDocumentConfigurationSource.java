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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.mail.MailGeneralConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Provides configuration from the Mail.GeneralMailConfigClass document in the current wiki.
 * If the Mail.GeneralMailConfigClass xobject exists in the Mail.MailConfig document
 * then use configuration values from it and if it doesn't then look up similar values
 * in the main preferences.
 *
 * @version $Id$
 * @since 11.1
 * @since 10.11.4
 */
@Component(roles = {MailGeneralConfiguration.class})
@Singleton
public class GeneralMailConfigClassDocumentConfigurationSource extends AbstractDocumentConfigurationSource
    implements MailGeneralConfiguration
{
    private static final String MAIL_SPACE = "Mail";

    private static final String PROPERTY_OBFUSCATE_NAME = "obfuscate";

    /**
     * The local reference of the Mail.GeneralMailConfigClass xclass.
     */
    private static final LocalDocumentReference CLASS_REFERENCE = new LocalDocumentReference(MAIL_SPACE,
        "GeneralMailConfigClass");

    /**
     * The local reference of the Mail.MailConfig document.
     */
    private static final LocalDocumentReference DOC_REFERENCE = new LocalDocumentReference(MAIL_SPACE, "MailConfig");

    @Inject
    @Named("wiki")
    private ConfigurationSource wikiConfig;

    @Override
    protected String getCacheId()
    {
        return "configuration.document.mail.general";
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
    protected <T> T getPropertyValue(String key, Class<T> valueClass)
    {
        T value = super.getPropertyValue(key, valueClass);
        if (value == null && PROPERTY_OBFUSCATE_NAME.equals(key)) {
            value = wikiConfig.getProperty("obfuscateEmailAddresses", valueClass);
        }
        return value;
    }

    @Override
    public boolean isObfuscateEmails()
    {
        return getProperty(PROPERTY_OBFUSCATE_NAME, 0) != 0;
    }

    protected List<Event> getCacheCleanupEvents()
    {
        List<Event> events = new ArrayList<>();
        events.addAll(super.getCacheCleanupEvents());

        // cannot access this:
        // events.addAll(wikiConfig.getCacheCleanupEvents());
        // so instead:
        RegexEntityReference classMatcher = BaseObjectReference.any("XWiki.XWikiPreferences");
        events.addAll(Arrays.<Event>asList(new XObjectAddedEvent(classMatcher), new XObjectDeletedEvent(classMatcher),
            new XObjectUpdatedEvent(classMatcher)));

        return events;
    }
}
