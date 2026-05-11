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
package org.xwiki.guidedtour.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.guidedtour.api.dtos.UserTourStatusDTO;
import org.xwiki.guidedtour.api.enums.Status;
import org.xwiki.guidedtour.api.exceptions.DuplicatedIdException;
import org.xwiki.guidedtour.api.exceptions.InvalidIdException;
import org.xwiki.model.reference.DocumentReference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.guidedtour.internal.util.GuidedTourConstants.USER_TOUR_CLASS;

/**
 * Manages the user status for the guided tour. It provides methods to create, retrieve and update the user status.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component(roles = UserStatusManager.class)
@Singleton
public class UserStatusManager
{
    private static final String TASKS_STATUS_KEY = "tasksStatus";

    private static final String WIDGET_STATE_KEY = "widgetState";

    private static final String CALL_TO_ACTION_KEY = "callToAction";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    /**
     * Retrieves the user tour status for the current user. It returns a JSON string representing the user tour status.
     *
     * @return a JSON string representing the user tour status and preferences
     * @throws XWikiException if there is an error while retrieving the user document
     * @throws JsonProcessingException if there is an error while processing the JSON data
     * @throws InvalidIdException if the user tour status is not found for the current user
     */
    public UserTourStatusDTO getUserToursStatus() throws XWikiException, JsonProcessingException, InvalidIdException
    {
        BaseObject userTourStatusObject = getUserTourStatusObject();
        UserTourStatusDTO userTourStatus = new UserTourStatusDTO();
        String storedJson = userTourStatusObject.getStringValue(TASKS_STATUS_KEY);
        if (!storedJson.isEmpty()) {
            Map<String, Status> map = this.objectMapper.readValue(storedJson, new TypeReference<Map<String, Status>>()
            {
            });
            userTourStatus.setTasksStatus(map);
        }
        userTourStatus.setWidgetState((userTourStatusObject.getStringValue(WIDGET_STATE_KEY)));
        userTourStatus.setCallToAction(userTourStatusObject.getIntValue(CALL_TO_ACTION_KEY) == 1);
        return userTourStatus;
    }

    /**
     * Creates a user tour status object for the current user if it doesn't exist.
     *
     * @throws XWikiException if there is an error while interacting with the XWiki API
     * @throws DuplicatedIdException if a user tour status already exists for the current user
     */
    public void createUserTourStatus() throws XWikiException, DuplicatedIdException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        DocumentReference userDocRef = wikiContext.getUserReference();
        XWikiDocument userDoc = wikiContext.getWiki().getDocument(userDocRef, wikiContext);
        if (userDoc.getXObject(USER_TOUR_CLASS) == null) {
            userDoc.newXObject(USER_TOUR_CLASS, wikiContext);
            wikiContext.getWiki().saveDocument(userDoc, "Added guided tour user status object.", wikiContext);
        } else {
            throw new DuplicatedIdException("User tour status already exists for user [%s]",
                wikiContext.getUserReference());
        }
    }

    /**
     * Updates the user tour status for the current user based on the provided DTO.
     *
     * @param userTourStatus the DTO containing the updated user tour status information
     * @throws XWikiException if there is an error while interacting with the XWiki API
     * @throws JsonProcessingException if there is an error while processing the JSON data
     * @throws InvalidIdException if the user tour status is not found for the current user
     */
    public void updateUserTourStatus(UserTourStatusDTO userTourStatus)
        throws XWikiException, JsonProcessingException, InvalidIdException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        BaseObject userTourStatusObject = getUserTourStatusObject();
        String json = this.objectMapper.writeValueAsString(userTourStatus.getTasksStatus());
        userTourStatusObject.setLargeStringValue(TASKS_STATUS_KEY, json);
        userTourStatusObject.setStringValue(WIDGET_STATE_KEY, userTourStatus.getWidgetState().toString());
        userTourStatusObject.setIntValue(CALL_TO_ACTION_KEY, userTourStatus.isCallToAction() ? 1 : 0);
        wikiContext.getWiki()
            .saveDocument(userTourStatusObject.getOwnerDocument(), "Updated guided tour user status.", wikiContext);
    }

    private BaseObject getUserTourStatusObject() throws InvalidIdException, XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        DocumentReference userDocRef = wikiContext.getUserReference();
        XWikiDocument userDoc = wikiContext.getWiki().getDocument(userDocRef, wikiContext);
        BaseObject userTourStatusObject = userDoc.getXObject(USER_TOUR_CLASS);
        if (userTourStatusObject == null) {
            throw new InvalidIdException("User tour status not found for user [%s].", wikiContext.getUserReference());
        }
        return userTourStatusObject;
    }
}
