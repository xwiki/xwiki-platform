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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.AndNode;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.LikeNode;
import org.xwiki.notifications.filters.expression.NotNode;
import org.xwiki.notifications.filters.expression.OrNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StringValueNode;
import org.xwiki.notifications.filters.expression.EmptyNode;
import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

/**
 * Define a notification filter based on a scope in the wiki.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Named(ScopeNotificationFilter.FILTER_NAME)
@Singleton
public class ScopeNotificationFilter extends AbstractNotificationFilter
{
    static final String FILTER_NAME = "scopeNotificationFilter";

    private static final String ERROR = "Failed to filter the notifications.";

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public boolean filterEventByFilterType(Event event, DocumentReference user, NotificationFormat format,
            NotificationFilterType filterType)
    {
        // Indicate if a restriction exist concerning this type of event
        boolean hasRestriction = false;
        // Indicate if a restriction matches the document of the event
        boolean matchRestriction = false;

        try {
            for (NotificationFilterPreference preference
                    : notificationFilterManager.getFilterPreferences(user, this, filterType, format)) {

                // Wrap the current NotificationFilterPreference in a ScopeNotificationFilterPreference in order to
                // access #getScopeReference
                ScopeNotificationFilterPreference scopePreference =
                        new ScopeNotificationFilterPreference(preference, entityReferenceResolver);

                if (scopePreference.getProperties(NotificationFilterProperty.EVENT_TYPE).contains(event.getType())) {
                    hasRestriction = true;

                    if (event.getDocument().equals(scopePreference.getScopeReference())
                            || event.getDocument().hasParent(scopePreference.getScopeReference())) {

                        // If we have a match on an EXCLUSIVE filter, we don’t need to go any further
                        if (filterType.equals(NotificationFilterType.EXCLUSIVE)) {
                            return true;
                        }

                        matchRestriction = true;
                        break;
                    }
                }
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }

        /**
         * In case we have an INCLUSIVE filter, we check if we had a restriction that was not satisfied.
         * In the case of an EXCLUSIVE filter, if a restriction has been found, then the function should have already
         * returned true.
         */
        return (filterType.equals(NotificationFilterType.INCLUSIVE)
                && hasRestriction && !matchRestriction);
    }

    @Override
    public AbstractNode generateFilterExpression(DocumentReference user, NotificationPreference preference,
            NotificationFilterType filterType)
    {
        AbstractNode syntaxNode = new EmptyNode();
        boolean isFirstPass = true;

        try {
            for (NotificationFilterPreference filterPreference
                    : notificationFilterManager.getFilterPreferences(user, this, filterType,
                            preference.getFormat())) {

                ScopeNotificationFilterPreference filterPreferenceScope =
                        new ScopeNotificationFilterPreference(filterPreference, entityReferenceResolver);

                if (!scopeMatchesFilteringContext(filterPreference, preference)) {
                    continue;
                }

                AbstractNode tmpNode;

                String wiki = filterPreferenceScope.getScopeReference().extractReference(EntityType.WIKI).getName();
                String space = serializer.serialize(filterPreferenceScope.getScopeReference());
                String page = serializer.serialize(filterPreferenceScope.getScopeReference());

                switch (filterPreferenceScope.getScopeReference().getType()) {
                    case DOCUMENT:
                        tmpNode = new AndNode(
                                new EqualsNode(
                                        new PropertyValueNode(NotificationFilterProperty.WIKI),
                                        new StringValueNode(wiki)),
                                new EqualsNode(
                                        new PropertyValueNode(NotificationFilterProperty.PAGE),
                                        new StringValueNode(page)));
                        break;
                    case SPACE:
                        tmpNode = new AndNode(
                                new EqualsNode(
                                        new PropertyValueNode(NotificationFilterProperty.WIKI),
                                        new StringValueNode(wiki)),
                                new LikeNode(
                                        new PropertyValueNode(NotificationFilterProperty.SPACE),
                                        new StringValueNode(space)));
                        break;
                    case WIKI:
                        tmpNode = new EqualsNode(
                                new PropertyValueNode(NotificationFilterProperty.WIKI),
                                new StringValueNode(wiki));
                        break;
                    default:
                        continue;
                }

                // If we have an EXCLUSIVE filter, negate the filter node
                if (filterType.equals(NotificationFilterType.EXCLUSIVE)) {
                    tmpNode = new NotNode(tmpNode);
                }

                // Wrap the freshly created node in a AndNode or a OrNode depending on the filter type
                if (isFirstPass) {
                    isFirstPass = false;
                    syntaxNode = tmpNode;
                } else if (filterType.equals(NotificationFilterType.INCLUSIVE)) {
                    syntaxNode = new OrNode(syntaxNode, tmpNode);
                } else {
                    syntaxNode = new AndNode(syntaxNode, tmpNode);
                }
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }

        return syntaxNode;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return preference.getCategory().equals(NotificationPreferenceCategory.DEFAULT)
                && preference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE);
    }

    @Override
    public String getName()
    {
        return FILTER_NAME;
    }

    /**
     * Given a {@link NotificationFilterPreference} and the current filtering context (defined by a
     * {@link NotificationPreference}), determine if a the current filter should apply with the given scope.
     *
     * @param scope the reference scope
     * @param preference the related notification preference
     * @return true if the filter should be applied to the given scope.
     */
    private boolean scopeMatchesFilteringContext(NotificationFilterPreference scope,
            NotificationPreference preference)
    {
        // We apply the filter only on scopes having the correct eventType
        return (preference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)
                && scope.getProperties(NotificationFilterProperty.EVENT_TYPE).contains(
                preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE)));
    }
}
