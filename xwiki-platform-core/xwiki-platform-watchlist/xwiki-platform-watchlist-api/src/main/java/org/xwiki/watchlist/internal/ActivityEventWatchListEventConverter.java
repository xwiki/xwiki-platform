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
package org.xwiki.watchlist.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.watchlist.internal.api.WatchListEvent;

import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;

/**
 * Converts from an {@link ActivityEvent}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class ActivityEventWatchListEventConverter implements WatchListEventConverter<ActivityEvent>
{
    /**
     * Explicit String to EntityReference resolver used to resolve relative string references.
     */
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<String> resolver;

    @Override
    public WatchListEvent convert(ActivityEvent from, Object... parameters)
    {
        DocumentReference documentReference =
            new DocumentReference(resolver.resolve(from.getPage(), EntityType.DOCUMENT,
                new WikiReference(from.getWiki())));

        String type = from.getType();

        DocumentReference userReference = null;
        // Watch out for unregistered user events since they have a null user.
        if (from.getUser() != null) {
            userReference =
                new DocumentReference(resolver.resolve(from.getUser(), EntityType.DOCUMENT,
                new WikiReference(from.getWiki())));
        }

        return new WatchListEvent(documentReference, type, userReference, from.getVersion(), from.getDate());
    }
}
