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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.namespace.Namespace;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.UntypedRecordableEventDescriptor;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.objects.BaseObject;

/**
 * This is the default implementation of {@link UntypedRecordableEventDescriptor}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
public class DefaultUntypedRecordableEventDescriptor implements UntypedRecordableEventDescriptor, WikiComponent
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUntypedRecordableEventDescriptor.class);

    /**
     * The event type field name in the XObject.
     */
    private static final String UNTYPED_EVENT_EVENT_TYPE = "eventType";

    /**
     * The event descriptor description field name in the XObject.
     */
    private static final String UNTYPED_EVENT_DESCRIPTOR_DESCRIPTION = "eventDescription";

    /**
     * The event descriptor validation expression field name in the XObject.
     */
    private static final String UNTYPED_EVENT_DESCRIPTOR_VALIDATION_EXPRESSION = "validationExpression";

    /**
     * The event descriptor target expression field name in the XObject.
     */
    private static final String UNTYPED_EVENT_DESCRIPTOR_TARGET = "target";

    /**
     * The event descriptor triggers field name in the XObject.
     */
    private static final String UNTYPED_EVENT_DESCRIPTOR_EVENT_TRIGGERS = "listenTo";

    /**
     * The event descriptor object type field name in the XObject.
     */
    private static final String UNTYPED_EVENT_DESCRIPTOR_OBJECT_TYPE = "objectType";

    /**
     * The event descriptor application name field name in the XObject.
     */
    private static final String UNTYPED_EVENT_DESCRIPTOR_APPLICATION_NAME = "applicationName";

    /**
     * The event descriptor application identifier field name in the XObject.
     */
    private static final String UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ID = "applicationId";

    /**
     * The event descriptor application icon field name in the XObject.
     */
    private static final String UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ICON = "applicationIcon";

    /**
     * The event descriptor event type icon field name in the XObject.
     */
    private static final String UNTYPED_EVENT_DESCRIPTOR_EVENT_TYPE_ICON = "eventTypeIcon";

    private String eventType;

    private String validationExpression;

    private String target;

    private List<String> objectTypes;

    private String eventDescription;

    private String applicationName;

    private String applicationId;

    private String applicationIcon;

    private String eventTypeIcon;

    private List<String> eventTriggers;

    private EntityReference entityReference;

    private DocumentReference authorReference;

    private ContextualLocalizationManager contextualLocalizationManager;

    private NamespaceContextExecutor namespaceContextExecutor;

    /**
     * Construct a DefaultUntypedRecordableEventDescriptor.
     * @param reference reference of the document holding the descriptor
     * @param authorReference reference of the author of the document
     * @param baseObject object holding the descriptor
     * @param contextualLocalizationManager an instance of the component ContextualLocalizationManager
     * @param namespaceContextExecutor an instance of the component NamespaceContextExecutor
     * @throws EventStreamException if an error occurs
     */
    public DefaultUntypedRecordableEventDescriptor(EntityReference reference, DocumentReference authorReference,
        BaseObject baseObject, ContextualLocalizationManager contextualLocalizationManager,
            NamespaceContextExecutor namespaceContextExecutor)
            throws EventStreamException
    {
        this.entityReference = reference;
        this.authorReference = authorReference;
        this.setProperties(baseObject);
        this.contextualLocalizationManager = contextualLocalizationManager;
        this.namespaceContextExecutor = namespaceContextExecutor;
    }

    /**
     * Set the object attributes by extracting their values from the given BaseObject.
     *
     * @param untypedEventObject the XObject that should contain the desired values
     * @throws EventStreamException if the properties could not be extracted
     */
    private void setProperties(BaseObject untypedEventObject)
            throws EventStreamException
    {
        try {
            this.eventType = untypedEventObject.getStringValue(UNTYPED_EVENT_EVENT_TYPE);
            this.validationExpression =
                    untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_VALIDATION_EXPRESSION);
            this.target = untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_TARGET);
            this.objectTypes = untypedEventObject.getListValue(UNTYPED_EVENT_DESCRIPTOR_OBJECT_TYPE);
            this.eventDescription = untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_DESCRIPTION);
            this.eventTriggers = untypedEventObject.getListValue(UNTYPED_EVENT_DESCRIPTOR_EVENT_TRIGGERS);
            this.applicationName = untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_APPLICATION_NAME);

            // If the applicationId property is not defined, fallback on the applicationName
            String rawApplicationIdProperty =
                    untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ID);
            this.applicationId = (rawApplicationIdProperty != null && StringUtils.isNotBlank(rawApplicationIdProperty))
                    ? rawApplicationIdProperty : this.applicationName;

            this.applicationIcon = untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_APPLICATION_ICON);
            this.eventTypeIcon = untypedEventObject.getStringValue(UNTYPED_EVENT_DESCRIPTOR_EVENT_TYPE_ICON);
        } catch (Exception e) {
            throw new EventStreamException(
                    String.format("Unable to extract the parameters of the [%s] EventClass.",
                            untypedEventObject), e);
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
        return getLocalizedMessage(applicationName);
    }

    @Override
    public String getApplicationId()
    {
        return this.applicationId;
    }

    @Override
    public String getDescription()
    {
        return getLocalizedMessage(this.eventDescription);
    }

    @Override
    public String getApplicationIcon()
    {
        return this.applicationIcon;
    }

    @Override
    public String getEventTypeIcon()
    {
        return this.eventTypeIcon;
    }

    @Override
    public String getTargetExpression() {
        return this.target;
    }

    /**
     * Render a translation key in the context of the namespace (e.g. the current wiki) where the component has been
     * loaded.
     *
     * Use-case: an event descriptor coming from the sub wiki is loaded and displayed in the main wiki. If the
     * translation resource is located in the sub wiki with the "WIKI" scope, the translation could not be rendered in
     * the main wiki. That's why we need to execute the localization in the context of the sub wiki.
     *
     * @param key the key to render
     * @return the rendered localization.
     *
     * @since 10.6RC1
     * @since 10.5
     * @since 9.11.6
     */
    protected String getLocalizedMessage(String key)
    {
        String wikiWhereTheDescriptorIs = this.entityReference.extractReference(EntityType.WIKI).getName();
        Namespace namespaceOfTheDescriptor = new WikiNamespace(wikiWhereTheDescriptorIs);

        try {
            return namespaceContextExecutor.execute(namespaceOfTheDescriptor,
                () -> contextualLocalizationManager.getTranslationPlain(key)
            );
        } catch (Exception e) {
            LOGGER.warn("Failed to render the translation key [{}] in the namespace [{}] for the event "
                    + "descriptor of [{}].", key, namespaceOfTheDescriptor, getEventType(), e);
            return contextualLocalizationManager.getTranslationPlain(key);
        }
    }
}
