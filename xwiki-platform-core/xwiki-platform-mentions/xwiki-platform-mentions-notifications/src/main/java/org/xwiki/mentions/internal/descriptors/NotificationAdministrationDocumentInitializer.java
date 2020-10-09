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
package org.xwiki.mentions.internal.descriptors;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.events.MentionEvent;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * A Mandatory Document Intializer that aims at providing global preferences for Mentions Notifications.
 * In the future, this should be replaced by a proper API in Notification to allow enabling such preferences globally.
 *
 * @version $Id$
 * @since 12.6.3
 * @since 12.8RC1
 */
@Component
@Singleton
@Named("XWiki.Notifications.Code.NotificationAdministration")
public class NotificationAdministrationDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * The path to the class parent document.
     */
    private static final List<String> PARENT_PATH = Arrays.asList("XWiki", "Notifications", "Code");

    private static final String EVENT_TYPE_FIELD = "eventType";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public NotificationAdministrationDocumentInitializer()
    {
        super(new LocalDocumentReference(PARENT_PATH, "NotificationAdministration"));
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needUpdate = super.updateDocument(document);

        LocalDocumentReference notificationPreferenceClass =
            new LocalDocumentReference(PARENT_PATH, "NotificationPreferenceClass");
        List<BaseObject> notificationPreferenceObjects =
            document.getXObjects(notificationPreferenceClass);

        boolean mentionEventPreferenceAlreadySaved = false;
        for (BaseObject notificationPreferenceObject : notificationPreferenceObjects) {
            if (MentionEvent.EVENT_TYPE.equals(notificationPreferenceObject.getStringValue(EVENT_TYPE_FIELD))) {
                mentionEventPreferenceAlreadySaved = true;
                break;
            }
        }

        if (!mentionEventPreferenceAlreadySaved) {
            try {
                int newObject = document.createXObject(notificationPreferenceClass, this.contextProvider.get());
                BaseObject xObject = document.getXObject(notificationPreferenceClass, newObject);
                xObject.setStringValue(EVENT_TYPE_FIELD, MentionEvent.EVENT_TYPE);
                xObject.setStringValue("format", "alert");
                xObject.setIntValue("notificationEnabled", 1);
                xObject.setDateValue("startDate", new Date());
            } catch (XWikiException e) {
                this.logger.error("Error while trying to set the global Notification Administration preferences "
                    + "for mentions notifications", e);
            }
        }

        return needUpdate || !mentionEventPreferenceAlreadySaved;
    }
}
