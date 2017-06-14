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
package org.xwiki.eventstream.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.eventstream.EventStreamException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This is the default implementation of {@link ModelBridge}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class DefaultModelBridge implements ModelBridge
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private DocumentReferenceResolver documentReferenceResolver;

    @Override
    public Map<String, Object> getEventDescriptorProperties(BaseObject untypedEventObject)
            throws EventStreamException
    {
        Map<String, Object> eventDescriptorProperties = new HashMap<>();
        eventDescriptorProperties.put(UNTYPED_EVENT_DESCRIPTOR_DESCRIPTION,
                untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_DESCRIPTION));
        eventDescriptorProperties.put(UNTYPED_EVENT_DESCRIPTOR_EVENT_TRIGGERS,
                untypedEventObject.getListValue(UNTYPED_EVENT_DESCRIPTOR_EVENT_TRIGGERS));
        eventDescriptorProperties.put(UNTYPED_EVENT_DESCRIPTOR_OBJECT_TYPE,
                untypedEventObject.getListValue(UNTYPED_EVENT_DESCRIPTOR_OBJECT_TYPE));
        eventDescriptorProperties.put(UNTYPED_EVENT_DESCRIPTOR_VALIDATION_EXPRESSION,
                untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_VALIDATION_EXPRESSION));
        eventDescriptorProperties.put(UNTYPED_EVENT_DESCRIPTOR_APPLICATION_NAME,
                untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_APPLICATION_NAME));
        eventDescriptorProperties.put(UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ICON,
                untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ICON));
        eventDescriptorProperties.put(UNTYPED_EVENT_EVENT_TYPE,
                untypedEventObject.getStringValue(UNTYPED_EVENT_EVENT_TYPE));

        return eventDescriptorProperties;
    }

    @Override
    public DocumentReference getAuthorReference(EntityReference entityReference) throws EventStreamException
    {
        return this.findDocument(entityReference).getAuthorReference();
    }

    @Override
    public void checkRights(EntityReference entityReference, DocumentReference authorReference)
            throws EventStreamException
    {
        if (!this.authorizationManager.hasAccess(Right.ADMIN, authorReference, entityReference))
        {
            throw new EventStreamException("Registering Untyped Events requires wiki administration rights.");
        }
    }

    @Override
    public boolean checkXObjectPresence(List<String> xObjectTypes, Object source)
    {
        if (xObjectTypes.isEmpty()) {
            return true;
        } else if (source instanceof XWikiDocument) {
            XWikiDocument document = (XWikiDocument) source;
            Map<DocumentReference, List<BaseObject>> documentXObjects = document.getXObjects();

            for (String objectType : xObjectTypes) {
                LocalDocumentReference localXObjectReference =
                        documentReferenceResolver.resolve(objectType).getLocalDocumentReference();

                /*  We can’t create a DocumentReference when only the objectType, so we will have to
                    iterate through the map */
                for (DocumentReference documentReference : documentXObjects.keySet())  {
                    if (documentReference.getLocalDocumentReference().equals(localXObjectReference)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private XWikiDocument findDocument(EntityReference entityReference) throws EventStreamException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        try {
            return xwiki.getDocument(entityReference, context);
        } catch (XWikiException e) {
            throw new EventStreamException(
                    String.format("Unable to retrieve the given document [%s] in the current context.",
                            entityReference), e);
        }
    }
}
