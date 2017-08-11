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

import org.slf4j.Logger;
import org.xwiki.eventstream.Event;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterProperty;
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

/**
 * Abstract class that helps defining notification filters based on a scope in the wiki.
 * The classes extending this abstract can then choose to filter either based on the eventType of the event,
 * or the applicationId of the event, for example.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public abstract class AbstractScopeNotificationFilter extends AbstractNotificationFilter
{
    private static final String ERROR = "Failed to filter the notifications.";

    @Inject
    @Named("cached")
    protected ModelBridge modelBridge;

    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> serializer;

    @Inject
    protected Logger logger;

    /**
     * Given a {@link NotificationPreferenceFilterScope} and the current filtering context (defined by a
     * {@link NotificationPreference}), determine if a the current filter should apply with the given scope.
     *
     * @param scope the reference scope
     * @param preference the related notification preference
     * @return true if the filter should be applied to the given scope.
     */
    protected abstract boolean scopeMatchesFilteringContext(NotificationPreferenceFilterScope scope,
            NotificationPreference preference);

    /**
     * Given a {@link NotificationPreferenceFilterScope} and the current filtering context (defined by a
     * {@link NotificationFormat} and an {@link Event}, determine if a the current filter should
     * apply with the given scope.
     *
     * @param scope the reference scope
     * @param format the format of the notification to filter
     * @param event the {@link Event} to use
     * @return true if the filter should be applied to the given scope.
     */
    protected abstract boolean scopeMatchesFilteringContext(NotificationPreferenceFilterScope scope,
            NotificationFormat format, Event event);


    @Override
    protected boolean filterEventByFilterType(Event event, DocumentReference user, NotificationFormat format,
            NotificationFilterType filterType)
    {
        // Indicate if a restriction exist concerning this type of event
        boolean hasRestriction = false;
        // Indicate if a restriction matches the document of the event
        boolean matchRestriction = false;

        try {
            for (NotificationPreferenceFilterScope scope : modelBridge.getNotificationPreferenceScopes(user, format,
                    filterType)) {
                if (scopeMatchesFilteringContext(scope, format, event)) {
                    hasRestriction = true;

                    if (event.getDocument().equals(scope.getScopeReference())
                            || event.getDocument().hasParent(scope.getScopeReference())) {

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
    protected AbstractNode generateFilterExpression(DocumentReference user, NotificationPreference preference,
            NotificationFilterType filterType)
    {
        AbstractNode syntaxNode = new EmptyNode();
        boolean isFirstPass = true;

        try {
            for (NotificationPreferenceFilterScope scope : modelBridge.getNotificationPreferenceScopes(user,
                    preference.getFormat(), filterType)) {
                if (!scopeMatchesFilteringContext(scope, preference)) {
                    continue;
                }

                AbstractNode tmpNode;

                String wiki = scope.getScopeReference().extractReference(EntityType.WIKI).getName();
                String space = serializer.serialize(scope.getScopeReference());
                String page = serializer.serialize(scope.getScopeReference());

                switch (scope.getScopeReference().getType()) {
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
}
