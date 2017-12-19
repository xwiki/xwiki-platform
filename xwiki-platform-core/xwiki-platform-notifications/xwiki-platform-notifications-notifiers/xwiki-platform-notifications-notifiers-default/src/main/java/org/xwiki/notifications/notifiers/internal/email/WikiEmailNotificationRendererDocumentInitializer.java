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
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Define the NotificationEmailRendererClass XObjects.
 *
 * @version $Id$
 * @since 9.11.1
 */
@Component
@Named(WikiEmailNotificationRendererDocumentInitializer.XCLASS_NAME)
@Singleton
public class WikiEmailNotificationRendererDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the XObject class that should be bound used.
     */
    public static final String XCLASS_NAME = "XWiki.Notifications.Code.NotificationEmailRendererClass";

    /**
     * The name of the event type property in the XObject.
     */
    public static final String EVENT_TYPE = "eventType";

    /**
     * The name of the notification html template property in the XObject.
     */
    public static final String HTML_TEMPLATE = "htmlTemplate";

    /**
     * The name of the notification plain text template property in the XObject.
     */
    public static final String PLAIN_TEXT_TEMPLATE = "plainTextTemplate";

    /**
     * The name of the email sibject property in the XObject.
     */
    public static final String EMAIL_SUBJECT_TEMPLATE = "emailSubject";

    /**
     * The name of the space where the class is located.
     */
    private static final List<String> SPACE_PATH = Arrays.asList("XWiki", "Notifications", "Code");

    /**
     * Default constructor.
     */
    public WikiEmailNotificationRendererDocumentInitializer()
    {
        super(new LocalDocumentReference(SPACE_PATH, "NotificationEmailRendererClass"));
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(EVENT_TYPE, "Event type", 64);
        xclass.addTextAreaField(HTML_TEMPLATE, "HTML template",
                40, 3, TextAreaClass.ContentType.VELOCITY_CODE);
        xclass.addTextAreaField(PLAIN_TEXT_TEMPLATE, "Plain text template",
                40, 3, TextAreaClass.ContentType.VELOCITY_CODE);
        xclass.addTextAreaField(EMAIL_SUBJECT_TEMPLATE, "Email subject template",
                40, 3, TextAreaClass.ContentType.VELOCITY_CODE);
    }
}
