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
package org.xwiki.security.authorization.internal;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.internal.requiredrights.DocumentRequiredRightsReader;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.internal.XWikiConstants;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserCreatingDocumentEvent;
import com.xpn.xwiki.internal.event.UserDeletingDocumentEvent;
import com.xpn.xwiki.internal.event.UserEvent;
import com.xpn.xwiki.internal.event.UserUpdatingDocumentEvent;
import com.xpn.xwiki.internal.mandatory.XWikiGlobalRightsDocumentInitializer;
import com.xpn.xwiki.internal.mandatory.XWikiRightsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.LevelsClass;

/**
 * Cancel any modification the user does not have the right to do.
 * 
 * @version $Id$
 * @since 11.6
 * @since 10.11.10
 */
@Component
@Singleton
@Named(RightsFilterListener.NAME)
public class RightsFilterListener extends AbstractEventListener
{
    /**
     * The unique name of the listener.
     */
    public static final String NAME = "org.xwiki.security.authorization.internal.RightsFilterListener";

    @Inject
    private AuthorizationManager authorization;

    @Inject
    private DocumentRequiredRightsReader documentRequiredRightsReader;

    /**
     * The default constructor.
     */
    public RightsFilterListener()
    {
        super(NAME, new UserUpdatingDocumentEvent(), new UserCreatingDocumentEvent(), new UserDeletingDocumentEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        UserEvent userEvent = (UserEvent) event;

        // Check local rights
        checkModifiedRights(userEvent.getUserReference(), document, XWikiRightsDocumentInitializer.CLASS_REFERENCE,
            null);

        // Check global rights
        checkModifiedRights(userEvent.getUserReference(), document,
            XWikiGlobalRightsDocumentInitializer.CLASS_REFERENCE, (CancelableEvent) event);

        // Check the required rights.
        checkModifiedRequiredRights(userEvent.getUserReference(), document, (CancelableEvent) event);
    }

    private void checkModifiedRequiredRights(DocumentReference user, XWikiDocument document,
        CancelableEvent event)
    {
        XWikiDocument originalDocument = document.getOriginalDocument();
        DocumentRequiredRights originalRequiredRights =
            this.documentRequiredRightsReader.readRequiredRights(originalDocument);
        DocumentRequiredRights requiredRights = this.documentRequiredRightsReader.readRequiredRights(document);

        if (!originalRequiredRights.equals(requiredRights) && requiredRights.enforce()) {
            // We can assume that the current user has all existing required rights as otherwise editing would be
            // denied.
            // Therefore, only check if the user has all rights specified in the updated document that could either
            // have changed required rights or where enforcing has just been enabled.
            for (DocumentRequiredRight requiredRight : requiredRights.rights()) {
                try {
                    this.authorization.checkAccess(requiredRight.right(), user,
                        document.getDocumentReference().extractReference(requiredRight.scope()));
                } catch (AccessDeniedException e) {
                    event.cancel(
                        "The author doesn't have the right [%s] on the [%s] level that has been specified as required."
                            .formatted(requiredRight.right().getName(), requiredRight.scope().getLowerCase()));
                    break;
                }
            }
        }
    }

    private void checkModifiedRights(DocumentReference user, XWikiDocument document,
        LocalDocumentReference classReference, CancelableEvent event)
    {
        XWikiDocument originalDocument = document.getOriginalDocument();

        List<BaseObject> originalRights = originalDocument.getXObjects(classReference);
        List<BaseObject> rights = document.getXObjects(classReference);
        try {
            checkModifiedRights(user, document.getDocumentReference(), originalRights, rights);
        } catch (AccessDeniedException e) {
            if (event instanceof UserDeletingDocumentEvent) {
                // Cancel the delete because it might have an impact on other documents
                event.cancel("Deleting the document have an impact on rights the author does not have");
            } else {
                // Cancel all the right modifications
                cancel(document, originalRights, rights);
            }
        }
    }

    private void cancel(XWikiDocument document, List<BaseObject> originalRights, List<BaseObject> rights)
    {
        for (int i = 0; i < originalRights.size() || i < rights.size(); ++i) {
            BaseObject originalRightObject = i < originalRights.size() ? originalRights.get(i) : null;
            BaseObject rightObject = i < rights.size() ? rights.get(i) : null;

            if (originalRightObject != null) {
                document.getXObjectsToRemove().remove(originalRightObject);
                if (rightObject != null) {
                    rightObject.apply(originalRightObject, true);
                } else {
                    document.setXObject(originalRightObject.getNumber(), originalRightObject.clone());
                }
            } else if (rightObject != null) {
                document.removeXObject(rightObject);
            }
        }
    }

    private void checkModifiedRights(BaseObject rightObject, DocumentReference user, DocumentReference document)
        throws AccessDeniedException
    {
        if (rightObject != null) {
            for (String level : LevelsClass
                .getListFromString(rightObject.getStringValue(XWikiConstants.LEVELS_FIELD_NAME))) {
                Right right = Right.toRight(level);

                // If the right does not exist test programming right instead
                if (right == Right.ILLEGAL) {
                    right = Right.PROGRAM;
                }

                // Check if the user is allowed to manipulate this right
                if (rightObject.getXClassReference().getName().equals(XWikiRightsDocumentInitializer.CLASS_NAME)) {
                    // Check document right for local rights
                    this.authorization.checkAccess(right, user, document);
                } else {
                    if (document.getLocalDocumentReference().equals(XWikiConstants.WIKI_DOC_REFERENCE)) {
                        // Check wiki right for global wiki right
                        this.authorization.checkAccess(right, user, document.getWikiReference());
                    } else if (document.getName().equals(XWikiConstants.SPACE_DOC)) {
                        // Check parent right for global space rights
                        this.authorization.checkAccess(right, user, document.getParent());
                    }
                }
            }
        }
    }

    private void checkModifiedRights(DocumentReference user, DocumentReference document,
        List<BaseObject> originalRightObjects, List<BaseObject> rightObjects) throws AccessDeniedException
    {
        for (int i = 0; i < originalRightObjects.size() || i < rightObjects.size(); ++i) {
            BaseObject originalRightObject = i < originalRightObjects.size() ? originalRightObjects.get(i) : null;
            BaseObject rightObject = i < rightObjects.size() ? rightObjects.get(i) : null;

            if (!Objects.equals(originalRightObject, rightObject)) {
                checkModifiedRights(originalRightObject, user, document);
                checkModifiedRights(rightObject, user, document);
            }
        }
    }
}
