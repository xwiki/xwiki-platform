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
package org.xwiki.notifications.filters.watch.internal;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.filters.watch.AutomaticWatchMode;
import org.xwiki.notifications.filters.watch.WatchedEntitiesConfiguration;
import org.xwiki.text.StringUtils;

/**
 * Default implementation of {@link WatchedEntitiesConfiguration}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
public class DefaultWatchedEntitiesConfiguration implements WatchedEntitiesConfiguration
{
    private static final LocalDocumentReference CLASS_REFERENCE = new LocalDocumentReference(
            Arrays.asList("XWiki", "Notifications", "Code"), "AutomaticWatchModeClass");

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public AutomaticWatchMode getAutomaticWatchMode(DocumentReference user)
    {
        Object value = documentAccessBridge.getProperty(user, getAbsoluteClassReference(user),
                "automaticWatchMode");
        if (value == null || StringUtils.isBlank((String) value)) {
            // Fallback to some default value
            // TODO: make it configurable too by the administrator
            return AutomaticWatchMode.NONE;
        }

        return AutomaticWatchMode.valueOf((String) value);
    }

    private DocumentReference getAbsoluteClassReference(DocumentReference user)
    {
        return new DocumentReference(CLASS_REFERENCE.appendParent(user.getWikiReference()));
    }
}
