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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.WikiBaseObjectComponentBuilder;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This implementation of {@link WikiBaseObjectComponentBuilder} allows to dynamically register an
 * {@link DefaultUntypedRecordableEventDescriptor} against the Component Manager.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Named(UntypedRecordableEventDescriptorWikiComponentBuilder.BOUNDED_XOBJECT_CLASS)
@Singleton
public class UntypedRecordableEventDescriptorWikiComponentBuilder implements WikiBaseObjectComponentBuilder
{
    /**
     * The class name of the XObject that should be bounded to this builder.
     */
    public static final String BOUNDED_XOBJECT_CLASS = "XWiki.EventStream.Code.EventClass";

    @Inject
    private AuthorizationManager authorizationManager;

    @Override
    public EntityReference getClassReference()
    {
        return new EntityReference(
                UntypedRecordableEventDescriptorWikiComponentBuilder.BOUNDED_XOBJECT_CLASS,
                EntityType.OBJECT);
    }

    @Override
    public List<WikiComponent> buildComponents(BaseObject baseObject) throws WikiComponentException
    {
        try {
            XWikiDocument parentDocument = baseObject.getOwnerDocument();
            this.checkRights(parentDocument.getDocumentReference(), parentDocument.getAuthorReference());

            // Get the parameters of the XObject we’re working
            Map<String, Object> parameters = this.getEventDescriptorProperties(baseObject);
            return Arrays.asList(
                    new DefaultUntypedRecordableEventDescriptor(
                            baseObject.getReference(), parentDocument.getAuthorReference(), parameters));
        } catch (Exception e) {
            throw new WikiComponentException(String.format(
                    "Unable to build the UntypedRecordableEvent wiki component "
                            + "for [%s].", baseObject), e);
        }
    }

    /**
     * Get a map of every properties of the given XObject.
     *
     * @param untypedEventObject the XObject
     * @return a map of the descriptor properties
     * @throws EventStreamException if the descriptor could not be found or if the properties could not be extracted
     */
    private Map<String, Object> getEventDescriptorProperties(BaseObject untypedEventObject)
            throws EventStreamException
    {
        Map<String, Object> eventDescriptorProperties = new HashMap<>();
        eventDescriptorProperties.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_DESCRIPTION,
                untypedEventObject.getStringValue(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_DESCRIPTION));
        eventDescriptorProperties.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_EVENT_TRIGGERS,
                untypedEventObject.getListValue(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_EVENT_TRIGGERS));
        eventDescriptorProperties.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_OBJECT_TYPE,
                untypedEventObject.getListValue(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_OBJECT_TYPE));
        eventDescriptorProperties.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_VALIDATION_EXPRESSION,
                untypedEventObject.getStringValue(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_VALIDATION_EXPRESSION));
        eventDescriptorProperties.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_APPLICATION_NAME,
                untypedEventObject.getStringValue(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_APPLICATION_NAME));
        eventDescriptorProperties.put(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ICON,
                untypedEventObject.getStringValue(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ICON));
        eventDescriptorProperties.put(ModelBridge.UNTYPED_EVENT_EVENT_TYPE,
                untypedEventObject.getStringValue(ModelBridge.UNTYPED_EVENT_EVENT_TYPE));

        return eventDescriptorProperties;
    }

    /**
     * Ensure that the given author has the administrative rights in the current context.
     *
     * @param entityReference the working entity
     * @param authorReference the author that should have its rights checked
     * @throws EventStreamException if the author rights are not sufficient
     */
    private void checkRights(EntityReference entityReference, DocumentReference authorReference)
            throws EventStreamException
    {
        if (!this.authorizationManager.hasAccess(Right.ADMIN, authorReference, entityReference))
        {
            throw new EventStreamException("Registering Untyped Events requires wiki administration rights.");
        }
    }
}
