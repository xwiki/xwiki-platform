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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Abstract implementation of {@link RecordableEventDescriptor}.
 *
 * @version $Id$
 * @since 9.4RC1
 */
public abstract class AbstractRecordableEventDescriptor implements RecordableEventDescriptor
{
    private String descriptionTranslationKey;

    private String applicationTranslationKey;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

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

    @Override
    public String getDescription()
    {
        return contextualLocalizationManager.getTranslationPlain(descriptionTranslationKey);
    }

    @Override
    public String getApplicationName()
    {
        return contextualLocalizationManager.getTranslationPlain(applicationTranslationKey);
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(getApplicationName()).append(getEventType()).toHashCode();
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
            equalsBuilder.append(other.getApplicationName(), this.getApplicationName());
            equalsBuilder.append(other.getEventType(), this.getEventType());

            return equalsBuilder.isEquals();
        }

        return false;
    }
}
