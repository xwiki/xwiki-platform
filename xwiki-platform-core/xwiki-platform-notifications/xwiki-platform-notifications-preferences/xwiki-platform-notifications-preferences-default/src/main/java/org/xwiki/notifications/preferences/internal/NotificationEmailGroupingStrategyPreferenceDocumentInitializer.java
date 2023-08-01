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

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

/**
 * Define the NotificationEmailGroupingStrategyPreferenceClass XClass.
 *
 * @version $Id$
 * @since 15.6RC1
 */
@Component
@Named(NotificationEmailGroupingStrategyPreferenceDocumentInitializer.REFERENCE_STRING)
@Singleton
public class NotificationEmailGroupingStrategyPreferenceDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The path to the class parent document.
     */
    public static final List<String> PARENT_PATH = Arrays.asList("XWiki", "Notifications", "Code");

    /**
     * The reference of the class of the object holding the notification email preferences.
     */
    public static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(PARENT_PATH, "NotificationEmailGroupingStrategyPreferenceClass");

    /**
     * The reference of the class of the object holding the notification email preferences.
     */
    public static final String REFERENCE_STRING =
        "XWiki.Notifications.Code.NotificationEmailGroupingStrategyPreferenceClass";

    /**
     * Field containing the strategy hint.
     */
    public static final String FIELD_STRATEGY = "strategy";

    /**
     * Field containing the output target for which the strategy should be used (e.g. email or alert)
     */
    public static final String FIELD_EMAIL_INTERVAL = "interval";

    /**
     * Default constructor.
     */
    public NotificationEmailGroupingStrategyPreferenceDocumentInitializer()
    {
        super(REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addStaticListField(FIELD_EMAIL_INTERVAL, "Notification interval", 1, false,
                "hourly=Hourly|daily=Daily|weekly=Weekly|live=Live", "select", "|, ");
        xclass.addTextField(FIELD_STRATEGY, FIELD_STRATEGY, 100);
    }
}
