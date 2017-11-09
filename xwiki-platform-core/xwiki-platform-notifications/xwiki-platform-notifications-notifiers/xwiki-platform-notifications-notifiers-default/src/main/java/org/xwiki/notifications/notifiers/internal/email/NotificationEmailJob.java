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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.Arrays;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.notifiers.email.NotificationEmailInterval;
import org.xwiki.notifications.notifiers.internal.ModelBridge;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.plugin.scheduler.AbstractJob;
import com.xpn.xwiki.web.Utils;

/**
 * Scheduler job that send emails about notifications.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class NotificationEmailJob extends AbstractJob implements Job
{
    private static final String LAST_FIRE_TIME = "lastFireTime";

    private static final String XWIKI_SPACE = "XWiki";

    private static final String NOTIFICATIONS_SPACE = "Notifications";

    @Override
    protected void executeJob(JobExecutionContext jobContext) throws JobExecutionException
    {
        NotificationConfiguration configuration = Utils.getComponent(NotificationConfiguration.class);
        if (!configuration.isEnabled() || !configuration.areEmailsEnabled()) {
            // do nothing
            return;
        }

        DocumentReference schedulerJobDocument = getSchedulerJobDocument(jobContext);
        BaseObjectReference emailJobObjectReference = getNotificationEmailJobObjectReference(schedulerJobDocument);

        NotificationUserIterator userIterator = Utils.getComponent(NotificationUserIterator.class);
        userIterator.initialize(getJobInterval(schedulerJobDocument));

        NotificationEmailSender mailSender = Utils.getComponent(NotificationEmailSender.class);
        mailSender.sendEmails(getPreviousFireTime(emailJobObjectReference), userIterator);

        setPreviousFireTime(emailJobObjectReference);
    }

    private DocumentReference getSchedulerJobDocument(JobExecutionContext jobContext)
    {
        JobDataMap data = jobContext.getJobDetail().getJobDataMap();

        BaseObject schedulerJobObject = (BaseObject) data.get("xjob");
        BaseObjectReference schedulerJobObjectReference = schedulerJobObject.getReference();
        return (DocumentReference) schedulerJobObjectReference.getParent();
    }

    private BaseObjectReference getNotificationEmailJobObjectReference(DocumentReference schedulerJobDocument)
    {
        DocumentReference emailJobDocument = new DocumentReference(
                schedulerJobDocument.getWikiReference().getName(),
                Arrays.asList(XWIKI_SPACE, NOTIFICATIONS_SPACE),
                schedulerJobDocument.getName()
        );

        return new BaseObjectReference(
                new DocumentReference(schedulerJobDocument.getWikiReference().getName(),
                        Arrays.asList(XWIKI_SPACE, NOTIFICATIONS_SPACE, "Code"), "EmailJobClass"),
                0,
                emailJobDocument
        );
    }

    private NotificationEmailInterval getJobInterval(DocumentReference schedulerJobDocument)
    {
        if (schedulerJobDocument.getName().contains("Hourly")) {
            return NotificationEmailInterval.HOURLY;
        } else if (schedulerJobDocument.getName().contains("Weekly")) {
            return NotificationEmailInterval.WEEKLY;
        }
        return NotificationEmailInterval.DAILY;
    }

    private Date getPreviousFireTime(BaseObjectReference emailJobObject)
    {
        Object previousFireTime = getDocumentAccessBridge().getProperty(emailJobObject, LAST_FIRE_TIME);
        if (previousFireTime != null) {
            return (Date) previousFireTime;
        }

        return new Date(0L);
    }

    private void setPreviousFireTime(BaseObjectReference emailJobObject) throws JobExecutionException
    {
        try {
            getModelBridge().savePropertyInHiddenDocument(emailJobObject, LAST_FIRE_TIME, new Date());
        } catch (Exception e) {
            throw new JobExecutionException(
                    String.format("Failed to update the last fire time property of [{}].", emailJobObject), e);
        }
    }

    private DocumentAccessBridge getDocumentAccessBridge()
    {
        return Utils.getComponent(DocumentAccessBridge.class);
    }

    private ModelBridge getModelBridge()
    {
        return Utils.getComponent(ModelBridge.class);
    }
}
