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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.WikiBaseObjectComponentBuilder;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Allows the definition of {@link org.xwiki.notifications.filters.NotificationFilterDisplayer} in wiki pages.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Named(WikiNotificationFilterDisplayerDocumentInitializer.XCLASS_NAME)
@Singleton
public class WikiNotificationFilterDisplayerComponentBuilder implements WikiBaseObjectComponentBuilder
{
    @Inject
    private DocumentAuthorizationManager authorizationManager;

    @Inject
    private Provider<WikiNotificationFilterDisplayer> wikiNotificationFilterDisplayerProvider;

    @Override
    public List<WikiComponent> buildComponents(BaseObject baseObject) throws WikiComponentException
    {
        try {
            // Check that the document owner is allowed to build the components
            XWikiDocument parentDocument = baseObject.getOwnerDocument();
            this.checkRights(parentDocument.getDocumentReference(), parentDocument.getAuthorReference());

            // Instantiate the component
            WikiNotificationFilterDisplayer wikiNotificationFilterDisplayer =
                this.wikiNotificationFilterDisplayerProvider.get();
            wikiNotificationFilterDisplayer.initialize(parentDocument.getAuthorReference(), baseObject);

            return List.of(wikiNotificationFilterDisplayer);
        } catch (Exception e) {
            throw new WikiComponentException(String.format(
                    "Unable to build the WikiNotificationFilterDisplayer wiki component "
                            + "for [%s].", baseObject), e);
        }
    }

    @Override
    public EntityReference getClassReference()
    {
        return new EntityReference(
                WikiNotificationFilterDisplayerDocumentInitializer.XCLASS_NAME,
                EntityType.OBJECT);
    }

    /**
     * Ensure that the given author has the administrative rights in the current context.
     *
     * @param documentReference the working entity
     * @param authorReference the author that should have its rights checked
     * @throws NotificationException if the author rights are not sufficient
     */
    private void checkRights(DocumentReference documentReference, DocumentReference authorReference)
            throws NotificationException
    {
        if (!this.authorizationManager.hasAccess(Right.ADMIN, EntityType.WIKI, authorReference, documentReference)) {
            throw new NotificationException(
                    "Registering custom Notification Filter Displayers requires wiki administration rights.");
        }
    }
}
