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
package org.xwiki.yjs.websocket.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.yjs.websocket.internal.ScriptAuthorChange.ScriptLevel;
import org.xwiki.yjs.websocket.internal.event.RoomScriptAuthorChangeEvent;

/**
 * Keeps track of the script author associated with each Yjs collaboration room. This is important because the users
 * that join a collaboration room can have different script righs and the content synchronized through a Yjs room can
 * contain scripts that must be evaluated server-side with the rights of the room script author (i.e. with the least
 * script rights amoung the users that have made changes).
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component(roles = RoomScriptAuthorTracker.class)
@Singleton
public class RoomScriptAuthorTracker
{
    @Inject
    private Logger logger;

    @Inject
    private DocumentAuthorizationManager authorizationManager;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserReferenceSerializer;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @Inject
    private ObservationManager observation;

    private final Map<DocumentReference, ScriptAuthorChange> scriptAuthors = new ConcurrentHashMap<>();

    /**
     * @param roomReference the reference of the collaboration room for which to get the script author
     * @return the script author associated with the specified collaboration room if any; empty optional if there is no
     *         script author associated with the specified collaboration room or if the collaboration room script author
     *         has more script rights than the current user (in this case we consider that the current user is the
     *         effective script author of the collaboration room)
     */
    public Optional<ScriptAuthorChange> getScriptAuthor(DocumentReference roomReference)
    {
        Optional<ScriptAuthorChange> returnValue = Optional.empty();
        ScriptAuthorChange scriptAuthorChange = this.scriptAuthors.get(roomReference);
        if (scriptAuthorChange != null) {
            UserReference currentUserReference = this.currentUserResolver.resolve(CurrentUserReference.INSTANCE);
            ScriptLevel currentUserScriptLevel = getUserScriptLevel(currentUserReference, roomReference);
            if (currentUserScriptLevel.compareTo(scriptAuthorChange.getScriptLevel()) > 0) {
                // We take into account the script author from the collaboration room only if it has less script rights
                // than the current user. Otherwise the current user will be used as script author.
                this.logger.debug("The script author associated with the collaboration room [{}] is [{}].",
                    roomReference, scriptAuthorChange);
                returnValue = Optional.of(scriptAuthorChange);
            }
        }

        return returnValue;
    }

    /**
     * Updates the script author associated with the specified collaboration room.
     * 
     * @param roomReference the reference of the collaboration room for which to update the script author
     * @param scriptAuthorChange the new script author and script level for the collaboration room
     */
    public void setScriptAuthor(DocumentReference roomReference, ScriptAuthorChange scriptAuthorChange)
    {
        if (scriptAuthorChange != null) {
            this.scriptAuthors.put(roomReference, scriptAuthorChange);
            this.logger.debug("Updated the script author associated with the collaboration room [{}] to [{}].",
                roomReference, scriptAuthorChange);
        } else {
            this.scriptAuthors.remove(roomReference);
            this.logger.debug("Removed the script author associated with the collaboration room [{}].", roomReference);
        }
    }

    /**
     * Updates the script author associated with the specified collaboration room if the specified author has less or
     * equal script rights.
     * 
     * @param roomReference the reference of the collaboration room for which to update the script author
     * @param author the user that made a change in the collaboration room and that can be the new script author if it
     *            has less or equal script rights than the current script author
     */
    public void maybeSetScriptAuthor(DocumentReference roomReference, UserReference author)
    {
        ScriptAuthorChange scriptAuthorChange = this.scriptAuthors.get(roomReference);
        ScriptLevel authorScriptLevel = getUserScriptLevel(author, roomReference);
        if (scriptAuthorChange == null || (authorScriptLevel.compareTo(scriptAuthorChange.getScriptLevel()) <= 0
            && !author.equals(scriptAuthorChange.getAuthor()))) {
            // The user making the change has a lower or equal script level than the room script level. We need to
            // update the room script level in order to prevent privilege escalation.
            scriptAuthorChange = new ScriptAuthorChange(author, authorScriptLevel);

            // Update the script author through an event listener to better cover clustering use case where we don't
            // have access to the WebSocket session of remote clients (connected to other cluster nodes) so we can only
            // initialize the XWiki context and assess the access rights for local clients.
            this.observation.notify(new RoomScriptAuthorChangeEvent(roomReference), scriptAuthorChange);
        }
    }

    private ScriptLevel getUserScriptLevel(UserReference userReference, DocumentReference documentReference)
    {
        DocumentReference documentUserReference = this.documentUserReferenceSerializer.serialize(userReference);
        if (this.authorizationManager.hasAccess(Right.PROGRAM, null, documentUserReference, documentReference)) {
            return ScriptLevel.PROGRAMMING;
        } else if (this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, documentUserReference,
            documentReference)) {
            return ScriptLevel.SCRIPT;
        } else {
            return ScriptLevel.NO_SCRIPT;
        }
    }
}
