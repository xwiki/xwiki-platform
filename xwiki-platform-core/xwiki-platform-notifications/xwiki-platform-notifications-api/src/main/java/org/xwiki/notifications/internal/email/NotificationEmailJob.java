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
package org.xwiki.notifications.internal.email;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.mail.Session;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.context.Execution;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.SessionFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.internal.plugin.rightsmanager.ReferenceUserIterator;
import com.xpn.xwiki.plugin.scheduler.AbstractJob;

/**
 * @version $Id$
 */
@Component(roles = NotificationEmailJob.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class NotificationEmailJob extends AbstractJob
{
    @Inject
    private MailSender mailSender;

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    @Named("database")
    private Provider<MailListener> mailListenerProvider;

    @Inject
    private Provider<NotificationMimeMessageIterator> notificationMimeMessageIteratorProvider;

    @Inject
    private Execution execution;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    public void test() throws JobExecutionException
    {
        this.executeJob(null);
    }

    @Override
    protected void executeJob(JobExecutionContext jobContext) throws JobExecutionException
    {
        List<DocumentReference> list = new ArrayList<>();
        list.add(new DocumentReference(wikiDescriptorManager.getCurrentWikiId(), "XWiki", "XWikiAllGroup"));

        ReferenceUserIterator userIterator = new ReferenceUserIterator(list, null, documentReferenceResolver,
                execution);

        Map<String, Object> parameters = new HashMap<>();

        DocumentReference templateReference = new DocumentReference(wikiDescriptorManager.getCurrentWikiId(),
                Arrays.asList("XWiki", "Notifications"), "MailTemplate");

        NotificationMimeMessageIterator notificationMimeMessageIterator = notificationMimeMessageIteratorProvider.get();
        notificationMimeMessageIterator.initialize(userIterator, parameters, new Date(0L), templateReference);

        Session session = this.sessionFactory.create(Collections.<String, String>emptyMap());
        MailListener mailListener = mailListenerProvider.get();

        // Pass it to the message sender to send it asynchronously.
        mailSender.sendAsynchronously(notificationMimeMessageIterator, session, mailListener);
    }
}
