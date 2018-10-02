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
package org.xwiki.notifications.rest.internal;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * @version $Id$
 * @since 10.9RC1
 */
@Component
@Singleton
@Named(TagNotificationFilter.NAME)
public class TagNotificationFilter implements NotificationFilter
{
    /**
     * Name of the filter.
     */
    public static final String NAME = "Tag Notification Filter";

    @Inject
    private QueryManager queryManager;

    @Inject
    private Logger logger;

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format)
    {
        return FilterPolicy.NO_EFFECT;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return false;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationPreference preference)
    {
        return null;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilterType type,
            NotificationFormat format)
    {
        if (type == NotificationFilterType.INCLUSIVE) {
            return null;
        }

        List<String> enabledTags = filterPreferences.stream()
                .filter(nfp -> nfp instanceof TagNotificationFilterPreference && nfp.isEnabled())
                .map(nfp -> ((TagNotificationFilterPreference) nfp).getTag().toLowerCase())
                .collect(Collectors.toList());

        if (enabledTags.isEmpty()) {
            return null;
        }

        try {
            Query query = queryManager.createQuery(
                    "SELECT DISTINCT doc.fullName FROM XWikiDocument doc, BaseObject obj, "
                            + "DBStringListProperty tags JOIN tags.list AS item "
                            + "WHERE obj.name = doc.fullName AND obj.className = 'XWiki.TagClass' "
                            + "AND obj.id = tags.id.id AND tags.id.name = 'tags' AND lower(item) IN (:tagList)",
                    Query.HQL);
            query.bindValue("tagList", enabledTags);
            query.setWiki(findCurrentWiki(filterPreferences));
            List<String> pagesHoldingTags = query.execute();
            return value(EventProperty.PAGE).inStrings(pagesHoldingTags);
        } catch (QueryException e) {
            logger.warn("Failed to get the list of documents holding some tags.", e);
            return null;
        }
    }

    private String findCurrentWiki(Collection<NotificationFilterPreference> filterPreferences)
    {
        for (NotificationFilterPreference nfp : filterPreferences) {
            if (nfp.isEnabled() && nfp instanceof TagNotificationFilterPreference) {
                TagNotificationFilterPreference pref = (TagNotificationFilterPreference) nfp;
                return pref.getCurrentWiki();
            }
        }
        // Should never happen
        return null;
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
