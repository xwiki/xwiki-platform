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
package org.xwiki.notifications.preferences.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.preferences.email.NotificationEmailDiffType;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Define the NotificationEmailPreferenceClass XClass.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Named(NotificationEmailPreferenceDocumentInitializer.REFERENCE_STRING)
@Singleton
public class NotificationEmailPreferenceDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The path to the class parent document.
     * 
     * @since 12.6
     */
    public static final List<String> PARENT_PATH = Arrays.asList("XWiki", "Notifications", "Code");

    /**
     * The reference of the class of the object holding the notification email preferences.
     * 
     * @since 12.6
     */
    public static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(PARENT_PATH, "NotificationEmailPreferenceClass");

    /**
     * The reference of the class of the object holding the notification email preferences.
     * 
     * @since 12.6
     */
    public static final String REFERENCE_STRING = "XWiki.Notifications.Code.NotificationEmailPreferenceClass";

    /**
     * The name of the field containing the notification interval.
     * 
     * @since 12.6
     */
    public static final String FIELD_INTERVAL = "interval";

    /**
     * The name of the field containing the diff type.
     *
     * @since 15.6RC1
     */
    public static final String FIELD_DIFF_TYPE = "diffType";

    private static final String SELECT = "select";

    private static final String SEPARATORS = "|, ";

    /**
     * Default constructor.
     */
    public NotificationEmailPreferenceDocumentInitializer()
    {
        super(REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addStaticListField(FIELD_INTERVAL, "Notification interval", 1, false,
            "hourly=Hourly|daily=Daily|weekly=Weekly|live=Live", SELECT, SEPARATORS);

        String values = Arrays.stream(NotificationEmailDiffType.values()).map(v -> v.name())
            .reduce((v1, v2) -> String.format("%s|%s", v1, v2)).get();

        xclass.addStaticListField(FIELD_DIFF_TYPE, "Diff Type", 1, false, values, SELECT, SEPARATORS);
    }
}
