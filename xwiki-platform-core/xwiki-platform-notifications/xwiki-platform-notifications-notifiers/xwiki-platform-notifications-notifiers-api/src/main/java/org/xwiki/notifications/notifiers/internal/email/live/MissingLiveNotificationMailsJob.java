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
package org.xwiki.notifications.notifiers.internal.email.live;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.notifiers.internal.email.IntervalUsersManager;
import org.xwiki.notifications.preferences.NotificationEmailInterval;

/**
 * Find and process events that were not send to users who enabled live mail yet.
 * 
 * @version $Id$
 * @since 12.6
 */
@Component
@Named(MissingLiveNotificationMailsJob.JOBTYPE)
public class MissingLiveNotificationMailsJob
    extends AbstractJob<MissingLiveNotificationMailsRequest, DefaultJobStatus<MissingLiveNotificationMailsRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "notification.livemails.resume";

    @Inject
    private PrefilteringLiveNotificationEmailDispatcher dispatcher;

    @Inject
    private EventStore eventStore;

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private IntervalUsersManager intervals;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        List<DocumentReference> users = this.intervals.getUsers(NotificationEmailInterval.LIVE, getRequest().getWiki());

        for (DocumentReference user : users) {
            SimpleEventQuery query = new SimpleEventQuery();

            query.withMail(this.referenceSerializer.serialize(user));

            try {
                EventSearchResult result = this.eventStore.search(query);

                result.stream().forEach(event -> this.dispatcher.addEvent(event, user));
            } catch (EventStreamException e) {
                this.logger.error("Failed to get events to send bby mail to the user [{}]", user, e);
            }
        }
    }
}
