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
package org.xwiki.notifications.notifiers.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.WikiBaseObjectComponentBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This component allows the definition of a notification templates in wiki pages.
 * It uses {@link org.xwiki.eventstream.UntypedRecordableEvent#getEventType} to be bound to a specific event type.
 *
 * @version $Id$
 * @since 9.11.1
 */
public abstract class AbstractWikiNotificationRendererComponentBuilder implements WikiBaseObjectComponentBuilder
{
    @Inject
    protected AuthorizationManager authorizationManager;

    @Override
    public List<WikiComponent> buildComponents(BaseObject baseObject) throws WikiComponentException
    {
        try {
            // Check that the document owner is allowed to build the components
            XWikiDocument parentDocument = baseObject.getOwnerDocument();
            this.checkRights(parentDocument.getDocumentReference(), parentDocument.getAuthorReference());

            // Instantiate the component
            return Arrays.asList(instantiateComponent(parentDocument.getAuthorReference(), baseObject));
        } catch (Exception e) {
            throw new WikiComponentException(String.format(
                    "Unable to build the WikiNotificationDisplayer wiki component "
                            + "for [%s].", baseObject), e);
        }
    }

    /**
     * Instantiate the wiki component.
     * @param authorReference reference of the author of the component
     * @param baseObject object holding the component
     * @return the corresponding wiki component
     * @throws NotificationException if an error occurs
     */
    protected abstract WikiComponent instantiateComponent(DocumentReference authorReference, BaseObject baseObject)
            throws NotificationException;

    /**
     * Ensure that the given author has the administrative rights in the current context.
     *
     * @param documentReference the working entity
     * @param authorReference the author that should have its rights checked
     * @throws NotificationException if the author rights are not sufficient
     */
    protected void checkRights(DocumentReference documentReference, DocumentReference authorReference)
            throws NotificationException
    {
        if (!this.authorizationManager.hasAccess(Right.ADMIN, authorReference, documentReference.getWikiReference())) {
            throw new NotificationException(
                    "Registering custom Notification Displayers requires wiki administration rights.");
        }
    }
}
