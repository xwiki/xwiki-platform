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
package org.xwiki.notifications.notifiers.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Define the NotificationDisplayerClass XObjects.
 *
 * @version $Id$
 * @since 9.6RC1
 */
@Component
@Named(WikiNotificationDisplayerDocumentInitializer.XCLASS_NAME)
@Singleton
public class WikiNotificationDisplayerDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the XObject class that should be bound used.
     */
    public static final String XCLASS_NAME = "XWiki.Notifications.Code.NotificationDisplayerClass";

    /**
     * The name of the event type property in the XObject.
     */
    public static final String EVENT_TYPE = "eventType";

    /**
     * The name of the notification template property in the XObject.
     */
    public static final String NOTIFICATION_TEMPLATE = "notificationTemplate";

    /**
     * The name of the space where the class is located.
     */
    private static final List<String> SPACE_PATH = Arrays.asList("XWiki", "Notifications", "Code");

    /**
     * Reference of the XClass to create.
     */
    public static final LocalDocumentReference CLASS_REFERENCE
            = new LocalDocumentReference(SPACE_PATH, "NotificationDisplayerClass");

    /**
     * Default constructor.
     */
    public WikiNotificationDisplayerDocumentInitializer()
    {
        super(CLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(EVENT_TYPE, "Event type", 64);
        xclass.addTextAreaField(NOTIFICATION_TEMPLATE, "Notification template",
                40, 3, TextAreaClass.ContentType.VELOCITY_CODE);
    }
}
