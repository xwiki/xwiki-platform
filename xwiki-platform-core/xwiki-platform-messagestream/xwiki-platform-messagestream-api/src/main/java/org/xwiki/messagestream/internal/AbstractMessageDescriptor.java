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
package org.xwiki.messagestream.internal;

import javax.inject.Inject;

import org.xwiki.eventstream.AbstractRecordableEventDescriptor;
import org.xwiki.messagestream.MessageStreamConfiguration;

/**
 * @version $Id$
 * @since 10.5RC1
 * @since 9.11.6
 */
public abstract class AbstractMessageDescriptor extends AbstractRecordableEventDescriptor
{
    @Inject
    private MessageStreamConfiguration messageStreamConfiguration;

    /**
     * Construct an AbstractRecordableEventDescriptor.
     *
     * @param descriptionTranslationKey the name of the translation key that describe the event
     */
    public AbstractMessageDescriptor(String descriptionTranslationKey)
    {
        super(descriptionTranslationKey, "messagestream.descriptors.applicationName");
    }

    @Override
    public String getApplicationIcon()
    {
        return "envelope";
    }

    @Override
    public String getApplicationId()
    {
        return "org.xwiki.platform:xwiki-platform-messagestream-api";
    }

    @Override
    public boolean isEnabled(String wikiId)
    {
        return messageStreamConfiguration.isEnabled(wikiId);
    }
}
