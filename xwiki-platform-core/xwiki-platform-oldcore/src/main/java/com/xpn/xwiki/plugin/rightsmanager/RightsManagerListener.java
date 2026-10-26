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

package com.xpn.xwiki.plugin.rightsmanager;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.internal.wiki.WikiOwnerUpdater;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.refactoring.event.DocumentRenamingEvent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * Listener to user and groups events, to apply related and automatic tasks like cleaning groups and rights objects.
 *
 * @version $Id$
 * @since 2.2.3
 */
public final class RightsManagerListener implements EventListener
{
    /**
     * The logging tool.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RightsManagerListener.class);

    /**
     * The name of the listener.
     */
    private static final String NAME = "rightsmanager";

    /**
     * The events to match.
     */
    private static final List<Event> EVENTS = List.of(new DocumentDeletedEvent());

    private static final DocumentRenamingEvent DOCUMENT_RENAMING_EVENT = new DocumentRenamingEvent();

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Unique instance of RightsManager.
     */
    private static RightsManagerListener instance;

    /**
     * Hidden constructor of RightsManager only access via getInstance().
     */
    private RightsManagerListener()
    {
    }

    /**
     * @return a unique instance of RightsManager. Thread safe.
     */
    public static RightsManagerListener getInstance()
    {
        synchronized (RightsManagerListener.class) {
            if (instance == null) {
                instance = new RightsManagerListener();
            }
        }

        return instance;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Only take into account local events and ensure the deleted event is not triggered as part of a rename.
        if (!Utils.getComponent(RemoteObservationManagerContext.class).isRemoteState() && !isInRenamingEvent()) {
            XWikiDocument document = ((XWikiDocument) source).getOriginalDocument();
            XWikiContext context = (XWikiContext) data;

            if (document.getObject("XWiki.XWikiUsers") != null) {
                try {
                    cleanDeletedUserOrGroup(document, true, context);
                } catch (XWikiException e) {
                    LOGGER.warn("Failed to clean the rights of deleted user [{}]", document.getDocumentReference(), e);
                }
            } else if (document.getObject("XWiki.XWikiGroups") != null) {
                try {
                    cleanDeletedUserOrGroup(document, false, context);
                } catch (XWikiException e) {
                    LOGGER.warn("Failed to clean the rights of deleted group [{}]", document.getDocumentReference(),
                        e);
                }
            }
        }
    }

    private boolean isInRenamingEvent()
    {
        return (Utils.getComponent(ObservationContext.class).isIn(DOCUMENT_RENAMING_EVENT));
    }

    /**
     * Remove the reference to the provided user or group in all groups and rights, in the current wiki.
     *
     * @param userOrGroupWiki the wiki name of the group or user.
     * @param userOrGroupSpace the space name of the group or user.
     * @param userOrGroupName the name of the group or user.
     * @param user indicate if it is a user or a group.
     * @param context the XWiki context.
     * @throws XWikiException error when browsing groups or rights.
     */
    private void cleanDeletedUserOrGroupInLocalWiki(String userOrGroupWiki, String userOrGroupSpace,
        String userOrGroupName, boolean user, XWikiContext context) throws XWikiException
    {
        RightsManager.getInstance().removeUserOrGroupFromAllRights(userOrGroupWiki, userOrGroupSpace, userOrGroupName,
            user, context);
        context.getWiki().getGroupService(context).removeUserOrGroupFromAllGroups(userOrGroupWiki, userOrGroupSpace,
            userOrGroupName, context);
    }

    /**
     * Remove the reference to the provided user or group in all wiki owners, groups and rights, in all wikis. A
     * failure in a wiki is logged, and the other wikis are still cleaned.
     *
     * @param userOrGroupDocument the document of the deleted user or group
     * @param user indicate if it is a user or a group.
     * @param context the XWiki context.
     * @throws XWikiException error when getting the wikis.
     */
    private void cleanDeletedUserOrGroup(XWikiDocument userOrGroupDocument, boolean user, XWikiContext context)
        throws XWikiException
    {
        DocumentReference userOrGroupReference = userOrGroupDocument.getDocumentReference();

        Utils.getComponent(WikiOwnerUpdater.class).replaceOwner(userOrGroupReference, null);

        String userOrGroupSpace = userOrGroupDocument.getSpace();

        List<String> wikiList = context.getWiki().getVirtualWikisDatabaseNames(context);

        String database = context.getWikiId();
        try {
            for (String wikiName : wikiList) {
                context.setWikiId(wikiName);
                cleanDeletedUserOrGroupInWiki(userOrGroupReference, userOrGroupSpace, user, context);
            }
        } finally {
            context.setWikiId(database);
        }
    }

    /**
     * Remove the provided user or group from the rights and groups of the current wiki, logging a failure so that the
     * other wikis are still cleaned.
     *
     * @param userOrGroupReference the reference of the deleted user or group
     * @param userOrGroupSpace the space of the deleted user or group
     * @param user {@code true} for a user, {@code false} for a group
     * @param context the XWiki context
     */
    private void cleanDeletedUserOrGroupInWiki(DocumentReference userOrGroupReference, String userOrGroupSpace,
        boolean user, XWikiContext context)
    {
        try {
            cleanDeletedUserOrGroupInLocalWiki(userOrGroupReference.getWikiReference().getName(), userOrGroupSpace,
                userOrGroupReference.getName(), user, context);
        } catch (XWikiException e) {
            LOGGER.warn("Failed to clean the rights and groups of [{}] in the wiki [{}]", userOrGroupReference,
                context.getWikiId(), e);
        }
    }
}
