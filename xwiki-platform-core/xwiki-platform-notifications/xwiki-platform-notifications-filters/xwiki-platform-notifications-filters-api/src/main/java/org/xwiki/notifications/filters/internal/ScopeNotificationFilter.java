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

import java.util.Set;

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
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.not;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

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

                if (scopePreference.getProperties(NotificationFilterProperty.EVENT_TYPE).isEmpty()
                    || scopePreference.getProperties(NotificationFilterProperty.EVENT_TYPE).contains(event.getType())) {
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
        return (filterType.equals(NotificationFilterType.INCLUSIVE) && hasRestriction && !matchRestriction);
    }

    @Override
    public AbstractOperatorNode generateFilterExpression(DocumentReference user, NotificationPreference preference,
            NotificationFilterType filterType)
    {
        AbstractOperatorNode syntaxNode = null;
        boolean isFirstPass = true;

        try {
            // Get every filterPreference linked to the current filter
            Set<NotificationFilterPreference> notificationFilterPreferences;
            if (preference != null) {
                notificationFilterPreferences = notificationFilterManager.getFilterPreferences(
                        user, this, filterType, preference.getFormat());
            } else {
                notificationFilterPreferences = notificationFilterManager.getFilterPreferences(
                        user, this, filterType);
            }

            for (NotificationFilterPreference filterPreference : notificationFilterPreferences) {

                ScopeNotificationFilterPreference filterPreferenceScope =
                        new ScopeNotificationFilterPreference(filterPreference, entityReferenceResolver);

                if (!scopeMatchesFilteringContext(filterPreference, preference)) {
                    continue;
                }

                AbstractOperatorNode tmpNode = generateNode(filterPreferenceScope);

                // If we have an EXCLUSIVE filter, negate the filter node
                if (filterType.equals(NotificationFilterType.EXCLUSIVE)) {
                    tmpNode = not(tmpNode);
                }

                // Wrap the freshly created node in a AndNode or a OrNode depending on the filter type
                if (isFirstPass) {
                    isFirstPass = false;
                    syntaxNode = tmpNode;
                } else if (filterType.equals(NotificationFilterType.INCLUSIVE)) {
                    syntaxNode = syntaxNode.or(tmpNode);
                } else {
                    syntaxNode = syntaxNode.or(tmpNode);
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
     * Note that we allow the {@link NotificationPreference} to be null and the
     * {@link NotificationFilterPreference} to have an empty {@link NotificationPreferenceProperty#EVENT_TYPE} property.
     * This is done to allow the filter to be applied globally and match all events.
     *
     * @param filterPreference the reference scope
     * @param preference the related notification preference, can be null
     * @return true if the filter should be applied to the given scope.
     */
    private boolean scopeMatchesFilteringContext(NotificationFilterPreference filterPreference,
            NotificationPreference preference)
    {
        return ((preference == null && filterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE).isEmpty())
                || (preference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)
                    && filterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE).contains(
                            preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE))));
    }

    /**
     * Given {@link ScopeNotificationFilterPreference}, generate the associated scope restriction.
     *
     * @param filterPreferenceScope the preference to use
     * @return the generated node
     */
    AbstractOperatorNode generateNode(ScopeNotificationFilterPreference filterPreferenceScope)
    {
        String wiki = filterPreferenceScope.getScopeReference().extractReference(EntityType.WIKI).getName();
        String space = serializer.serialize(filterPreferenceScope.getScopeReference());
        String page = serializer.serialize(filterPreferenceScope.getScopeReference());

        switch (filterPreferenceScope.getScopeReference().getType()) {
            case DOCUMENT:
                return value(NotificationFilterProperty.WIKI).eq(value(wiki))
                        .and(value(NotificationFilterProperty.PAGE).eq(value(page)));

            case SPACE:
                return value(NotificationFilterProperty.WIKI).eq(value(wiki))
                        .and(value(NotificationFilterProperty.SPACE).like(value(space)));

            case WIKI:
                return value(NotificationFilterProperty.WIKI).eq(value(wiki));

            default:
                return null;
        }
    }
}
