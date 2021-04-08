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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailResender;
import org.xwiki.mail.MailStorageConfiguration;
import org.xwiki.mail.MailStoreException;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * At XWiki startup, resends mail that were prepared but not sent and mail that failed to be sent previously.
 *
 * @version $Id$
 * @since 9.3RC1
 */
@Component
@Named(MailResenderListener.NAME)
@Singleton
public class MailResenderListener implements EventListener
{
    /**
     * The name of the event listener.
     */
    static final String NAME = "mailStartupResender";

    private static final String STATE_FIELD = "state";

    @Inject
    private Logger logger;

    @Inject
    private MailStorageConfiguration configuration;

    /**
     * We use a provider since a Listener cannot get injected any component that requires the Database to be ready to
     * initialize them, since Listeners are loaded very early during startup. In this case MailResender "database"
     * depends on DatabaseMailStatusStore which depends on XWikiHibernateStore which depends on
     * HibernateDataMigrationManager which requires the DB to be ready...
     */
    @Inject
    @Named("database")
    private Provider<MailResender> mailResenderProvider;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.asList(new ApplicationReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.configuration.resendAutomaticallyAtStartup()) {
            // TODO: wrap in a job and run asynchronously in order to not block XWiki startup
            MailResender resender = this.mailResenderProvider.get();

            // Resend all mails that in some prepare state since they've not been sent
            resendAllMatching(resender, Collections.singletonMap(STATE_FIELD, "prepare_success"));
        }
    }

    private void resendAllMatching(MailResender resender, Map<String, Object> filterMap)
    {
        try {
            resender.resendAsynchronously(filterMap, 0, 0);
        } catch (MailStoreException e) {
            // There's an important problem in the mail subsystem but don't stop XWiki since it's important but not
            // vital.
            this.logger.warn("Failed to resend unsent mails at startup for filter [{}]. Root error: [{}]",
                filterMap, ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
