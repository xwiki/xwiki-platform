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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.UntypedRecordableEventDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * This is the default implementation of {@link UntypedRecordableEventDescriptor}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class DefaultUntypedRecordableEventDescriptor implements UntypedRecordableEventDescriptor, WikiComponent
{
    private String eventType;

    private String validationExpression;

    private List<String> objectTypes;

    private String eventDescription;

    private String applicationName;

    private String applicationIcon;

    private List<String> eventTriggers;

    private EntityReference entityReference;

    private DocumentReference authorReference;

    DefaultUntypedRecordableEventDescriptor(EntityReference reference, DocumentReference authorReference,
            Map<String, Object> parameters) throws EventStreamException
    {
        try {
            this.entityReference = reference;
            this.authorReference = authorReference;

            // Cast options into private attributes
            this.eventType = (String) parameters.get(ModelBridge.UNTYPED_EVENT_EVENT_TYPE);
            this.validationExpression =
                    (String) parameters.get(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_VALIDATION_EXPRESSION);
            this.objectTypes = (List<String>) parameters.get(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_OBJECT_TYPE);
            this.eventDescription = (String) parameters.get(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_DESCRIPTION);
            this.eventTriggers = (List<String>) parameters.get(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_EVENT_TRIGGERS);
            this.applicationName = (String) parameters.get(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_APPLICATION_NAME);
            this.applicationIcon = (String) parameters.get(ModelBridge.UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ICON);
        } catch (Exception e) {
            throw new EventStreamException(
                    String.format("Unable to instanciate a DefaultUntypedRecordableEventDescriptor using [%s].",
                            reference), e);
        }
    }

    @Override
    public String getValidationExpression()
    {
        return this.validationExpression;
    }

    @Override
    public List<String> getEventTriggers()
    {
        return this.eventTriggers;
    }

    @Override
    public List<String> getObjectTypes()
    {
        return this.objectTypes;
    }

    @Override
    public EntityReference getEntityReference()
    {
        return this.entityReference;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return (DocumentReference) this.entityReference.getParent();
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    @Override
    public Type getRoleType()
    {
        return UntypedRecordableEventDescriptor.class;
    }

    @Override
    public String getRoleHint()
    {
        return this.eventType;
    }

    @Override
    public WikiComponentScope getScope()
    {
        return WikiComponentScope.WIKI;
    }

    @Override
    public String getEventType()
    {
        return this.eventType;
    }

    @Override
    public String getApplicationName()
    {
        return this.applicationName;
    }

    @Override
    public String getDescription()
    {
        return this.eventDescription;
    }

    @Override
    public String getApplicationIcon()
    {
        return this.applicationIcon;
    }
}
