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
package com.xpn.xwiki.events;

import org.xwiki.eventstream.AbstractRecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptor;

/**
 * Abstract implementation of {@link RecordableEventDescriptor} for all events sent by oldcore.
 *
 * @version $Id$
 * @since 9.6RC1
 */
public abstract class AbstractXWikiRecordableEventDescriptor extends AbstractRecordableEventDescriptor
{
    /**
     * Construct an AbstractRecordableEventDescriptor.
     *
     * @param descriptionTranslationKey the name of the translation key that describe the event
     * @param applicationTranslationKey the translation key of the name of the application that send this event
     */
    public AbstractXWikiRecordableEventDescriptor(String descriptionTranslationKey, String applicationTranslationKey)
    {
        super(descriptionTranslationKey, applicationTranslationKey);
    }

    @Override
    public String getApplicationId()
    {
        // A bit generic but I don't want to declare "oldcore".
        return "org.xwiki.platform";
    }

    @Override
    public String getApplicationIcon()
    {
        return "page";
    }
}
