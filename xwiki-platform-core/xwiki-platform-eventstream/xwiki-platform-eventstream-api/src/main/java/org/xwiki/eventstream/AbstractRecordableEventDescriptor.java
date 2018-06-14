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
package org.xwiki.eventstream;

import javax.inject.Inject;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.NamespacedComponentManager;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.component.namespace.NamespaceUtils;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Abstract implementation of {@link RecordableEventDescriptor}.
 *
 * @version $Id$
 * @since 9.4RC1
 */
public abstract class AbstractRecordableEventDescriptor implements RecordableEventDescriptor
{
    @Inject
    protected Logger logger;

    @Inject
    protected ComponentManager componentManager;

    @Inject
    protected ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    protected NamespaceContextExecutor namespaceContextExecutor;

    protected String descriptionTranslationKey;

    protected String applicationTranslationKey;

    /**
     * Construct an AbstractRecordableEventDescriptor.
     * @param descriptionTranslationKey the name of the translation key that describe the event
     * @param applicationTranslationKey the translation key of the name of the application that send this event
     */
    public AbstractRecordableEventDescriptor(String descriptionTranslationKey,
            String applicationTranslationKey)
    {
        this.descriptionTranslationKey = descriptionTranslationKey;
        this.applicationTranslationKey = applicationTranslationKey;
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
        if (componentManager instanceof NamespacedComponentManager) {
            NamespacedComponentManager namespacedComponentManager = (NamespacedComponentManager) componentManager;
            String namespaceOfTheDescriptor = namespacedComponentManager.getNamespace();

            if (namespaceOfTheDescriptor != null) {
                try {
                    return namespaceContextExecutor.execute(NamespaceUtils.toNamespace(namespaceOfTheDescriptor),
                        () -> contextualLocalizationManager.getTranslationPlain(key));
                } catch (Exception e) {
                    logger.warn("Failed to render the translation key [{}] in the namespace [{}] for the event "
                            + "descriptor of [{}].", key, namespaceOfTheDescriptor, getEventType(), e);
                }
            }
        }

        return contextualLocalizationManager.getTranslationPlain(key);
    }

    @Override
    public String getDescription()
    {
        return getLocalizedMessage(descriptionTranslationKey);
    }

    @Override
    public String getApplicationName()
    {
        return getLocalizedMessage(applicationTranslationKey);
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(getApplicationId()).append(getEventType()).toHashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }

        if (o instanceof RecordableEventDescriptor) {
            RecordableEventDescriptor other = (RecordableEventDescriptor) o;
            EqualsBuilder equalsBuilder = new EqualsBuilder();
            equalsBuilder.append(other.getApplicationId(), this.getApplicationId());
            equalsBuilder.append(other.getEventType(), this.getEventType());

            return equalsBuilder.isEquals();
        }

        return false;
    }
}
