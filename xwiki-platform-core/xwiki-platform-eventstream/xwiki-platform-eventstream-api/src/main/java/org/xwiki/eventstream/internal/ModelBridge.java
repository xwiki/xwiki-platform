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

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.objects.BaseObject;

/**
 * This is an internal role that allows requests to the model without having dependencies on xwiki-platform-oldcore.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Role
public interface ModelBridge
{
    /**
     * The event type field name in the XObject.
     */
    String UNTYPED_EVENT_EVENT_TYPE = "eventType";

    /**
     * The event descriptor description field name in the XObject.
     */
    String UNTYPED_EVENT_DESCRIPTOR_DESCRIPTION = "eventDescription";

    /**
     * The event descriptor validation expression field name in the XObject.
     */
    String UNTYPED_EVENT_DESCRIPTOR_VALIDATION_EXPRESSION = "validationExpression";

    /**
     * The event descriptor triggers field name in the XObject.
     */
    String UNTYPED_EVENT_DESCRIPTOR_EVENT_TRIGGERS = "listenTo";

    /**
     * The event descriptor object type field name in the XObject.
     */
    String UNTYPED_EVENT_DESCRIPTOR_OBJECT_TYPE = "objectType";

    /**
     * The event descriptor application name field name in the XObject.
     */
    String UNTYPED_EVENT_DESCRIPTOR_APPLICATION_NAME = "applicationName";

    /**
     * The event descriptor application icon field name in the XObject.
     */
    String UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ICON = "applicationIcon";

    /**
     * Get a map of every properties of the given XObject.
     *
     * @param untypedEventObject the XObject
     * @return a map of the descriptor properties
     * @throws EventStreamException if the descriptor could not be found or if the properties could not be extracted
     */
    Map<String, Object> getEventDescriptorProperties(BaseObject untypedEventObject)
            throws EventStreamException;

    /**
     * Get the author reference of the given entity.
     *
     * @param entityReference the document
     * @return the author reference
     * @throws EventStreamException if the author reference could not be retrieved
     */
    DocumentReference getAuthorReference(EntityReference entityReference) throws EventStreamException;

    /**
     * Ensure that the given author has the administrative rights in the current context.
     *
     * @param entityReference the working entity
     * @param authorReference the author that should have its rights checked
     * @throws EventStreamException if the author rights are not sufficient
     */
    void checkRights(EntityReference entityReference, DocumentReference authorReference)
            throws EventStreamException;

    /**
     * Check that the given source contains one of the XObject given in xObjectTypes. If the list of
     * xObjectTypes is empty, returns true.
     *
     * @param xObjectTypes a list of XObject types
     * @param source the source object
     * @return true if the source is an XWikiDocument and contains at least one of the given XObjects
     */
    boolean checkXObjectPresence(List<String> xObjectTypes, Object source);
}
