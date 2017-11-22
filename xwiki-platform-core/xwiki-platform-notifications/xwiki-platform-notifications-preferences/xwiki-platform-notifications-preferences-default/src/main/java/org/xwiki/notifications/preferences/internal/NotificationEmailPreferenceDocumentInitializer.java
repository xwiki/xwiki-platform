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
@Named("XWiki.Notifications.Code.NotificationEmailPreferenceClass")
@Singleton
public class NotificationEmailPreferenceDocumentInitializer  extends AbstractMandatoryClassInitializer
{
    /**
     * The path to the class parent document.
     */
    private static final List<String> PARENT_PATH = Arrays.asList("XWiki", "Notifications", "Code");

    private static final String SELECT = "select";

    private static final String SEPARATORS = "|, ";

    /**
     * Default constructor.
     */
    public NotificationEmailPreferenceDocumentInitializer()
    {
        super(new LocalDocumentReference(PARENT_PATH, "NotificationEmailPreferenceClass"));
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addStaticListField("interval", "Notification interval", 1,
                false, "hourly=Hourly|daily=Daily|weekly=Weekly|live=Live",
                SELECT, SEPARATORS);

        String values = Arrays.stream(NotificationEmailDiffType.values()).map(v -> v.name())
            .reduce((v1, v2) -> String.format("%s|%s", v1, v2)).get();

        xclass.addStaticListField("diffType", "Diff Type", 1,
                false, values, SELECT, SEPARATORS);
    }
}
