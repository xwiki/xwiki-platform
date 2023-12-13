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

import javax.inject.Inject;

import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Helper to build wiki components that render templates related to notifications.
 *
 * @version $Id$
 * @since 10.0
 * @since 9.11.1
 */
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public abstract class AbstractWikiNotificationRenderer implements WikiComponent
{
    protected static final String EVENT_BINDING_NAME = "event";

    @Inject
    protected TemplateManager templateManager;

    @Inject
    protected ScriptContextManager scriptContextManager;

    @Inject
    protected EntityReferenceSerializer<String> referenceSerializer;

    protected BaseObjectReference objectReference;

    protected DocumentReference authorReference;

    protected String eventType;

    /**
     * Constructs a new {@link AbstractWikiNotificationRenderer}.
     *
     * @param baseObject the XObject which has the required properties to instantiate the component
     * @throws NotificationException if the properties of the given BaseObject could not be loaded
     * @since 15.10RC1
     */
    public void initialize(BaseObject baseObject) throws NotificationException
    {
        this.objectReference = baseObject.getReference();
        this.authorReference = baseObject.getOwnerDocument().getAuthorReference();

        this.eventType = this.extractProperty(baseObject, WikiNotificationDisplayerDocumentInitializer.EVENT_TYPE);
    }

    protected Template extractTemplate(BaseObject baseObject, String propertyName) throws NotificationException
    {
        try {
            BaseProperty property = (BaseProperty) baseObject.get(propertyName);
            if (property != null && property.getValue() != null) {
                String xObjectTemplate = property.getValue().toString();
                if (StringUtils.isNotBlank(xObjectTemplate)) {
                    return this.templateManager.createStringTemplate(
                        this.referenceSerializer.serialize(property.getReference()), xObjectTemplate,
                        getAuthorReference(), baseObject.getDocumentReference());
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                String.format("Unable to render the template provided in the base object [%s]", baseObject), e);
        }

        return null;
    }

    /**
     * Extract the the given property value from the given XObject.
     *
     * @param baseObject the XObject that should contain the parameters
     * @param propertyName the value of the property that should be extracted
     * @throws NotificationException if an error occurred while extracting the parameter from the base object
     */
    protected String extractProperty(BaseObject baseObject, String propertyName) throws NotificationException
    {
        try {
            return baseObject.getStringValue(propertyName);
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Unable to extract the parameter [%s] from the [%s] NotificationDisplayerClass.",
                            propertyName, baseObject), e);
        }
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return (DocumentReference) this.objectReference.getParent();
    }

    @Override
    public EntityReference getEntityReference()
    {
        return this.objectReference;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
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
}
