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
package org.xwiki.notifications.filters.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Define the NotificationPreferenceScopeClass XClass.
 * This XClass is used to store filters information in the user profile.
 *
 * @version $Id$
 * @since 9.7R1
 */
@Component
@Named("XWiki.Notifications.Code.NotificationPreferenceScopeClass")
@Singleton
public class NotificationPreferenceScopeDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The path to the class parent document.
     */
    private static final List<String> PARENT_PATH = Arrays.asList("XWiki", "Notifications", "Code");

    private static final String SELECT = "select";

    private static final String INPUT = "input";

    private static final String SEPARATORS = "|, ";

    /**
     * Default constructor.
     */
    public NotificationPreferenceScopeDocumentInitializer()
    {
        super(new LocalDocumentReference(PARENT_PATH, "NotificationPreferenceScopeClass"));
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField("eventType", "Event Type", 64);
        xclass.addTextField("applicationId", "Application ID", 64);
        xclass.addStaticListField("format", "Format", 64, false,
                "alert=Alert|email=E-mail", INPUT, SEPARATORS);
        xclass.addStaticListField("scope", "Filter scope", 64, false,
                "pageOnly=Page|pageAndChildren=Page &amp; Children|wiki=Wiki", INPUT, SEPARATORS);
        xclass.addStaticListField("scopeFilterType", "Filter type", 64, false,
                "inclusive=Inclusive|exclusive=Exclusive", INPUT, SEPARATORS);
        xclass.addTextField("scopeReference", "Scope reference", 64);
        xclass.addBooleanField("isWatchList", "Is part of the WatchList ?", SELECT, false);
    }
}
