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
package org.xwiki.notifications.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationManager;

import com.xpn.xwiki.internal.plugin.rightsmanager.ReferenceUserIterator;

/**
 * @version $Id$
 */
@Component(roles = NotificationMimeMessageIterator.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class NotificationMimeMessageIterator implements Iterator<MimeMessage>, Iterable<MimeMessage>
{
    @Inject
    private NotificationManager notificationManager;

    @Inject
    @Named("template")
    private MimeMessageFactory<MimeMessage> factory;

    @Inject
    private Logger logger;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private NotificationEmailDisplayer defaultNotificationEmailDisplayer;

    private ReferenceUserIterator userIterator;

    private Map<String, Object> parameters;

    private Date lastTrigger;

    private DocumentReference templateReference;

    public void initialize(ReferenceUserIterator userIterator,
            Map<String, Object> parameters, Date lastTrigger, DocumentReference templateReference)
    {
        this.userIterator = userIterator;
        this.parameters = parameters;
        this.lastTrigger = lastTrigger;
        this.templateReference = templateReference;
    }

    @Override
    public boolean hasNext()
    {
        return this.userIterator.hasNext();
    }

    @Override
    public MimeMessage next()
    {
        MimeMessage message = null;
        DocumentReference user = userIterator.next();
        try {
            if (updateFactoryParameters(user)) {
                message = this.factory.createMessage(templateReference, parameters);
            }
        } catch (Exception e) {
            logger.error("Failed to generate an email for the user [{}].", user, e);
        }

        return message;
    }

    private boolean updateFactoryParameters(DocumentReference user) throws NotificationException, AddressException
    {
        List<String> events = new ArrayList<>();
        Map<String, Object> velocityVariables = (Map<String, Object>) parameters.get("velocityVariables");
        velocityVariables.put("events", events);

        for (CompositeEvent event : notificationManager.getEvents(user.toString(), false,
                Integer.MAX_VALUE / 4, null, lastTrigger, Collections.emptyList())) {
            try {
                NotificationEmailDisplayer displayer =
                        componentManager.getInstance(NotificationEmailDisplayer.class, event.getType());
                events.add(displayer.display(event));
            } catch (ComponentLookupException e) {
                events.add(defaultNotificationEmailDisplayer.display(event));
            }
        }

        String email = (String) documentAccessBridge.getProperty(user,
                new DocumentReference("xwiki", "XWiki", "XWikiUsers"),
                0, "email");

        parameters.put("from", new InternetAddress("gdelhumeau@xwiki.com"));

        try {
            parameters.put("to", new InternetAddress(email));
        } catch (AddressException e) {
            // The user has not written a valid email
            return false;
        }

        return true;
    }

    @Override
    public Iterator<MimeMessage> iterator()
    {
        return this;
    }
}
