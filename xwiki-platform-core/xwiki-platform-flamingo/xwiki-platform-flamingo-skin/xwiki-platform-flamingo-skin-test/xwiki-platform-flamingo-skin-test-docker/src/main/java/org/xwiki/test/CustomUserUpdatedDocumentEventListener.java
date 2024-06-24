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
package org.xwiki.test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserUpdatingDocumentEvent;

/**
 * Listener dedicated to cancel a specific rolling back event triggered in VersionIT test.
 *
 * @version $Id$
 * @since 14.10.17
 * @since 15.5.3
 * @since 15.8RC1
 */
@Component
@Singleton
@Named(CustomUserUpdatedDocumentEventListener.NAME)
public class CustomUserUpdatedDocumentEventListener extends AbstractEventListener
{
    static final String NAME = "CustomUserUpdatedDocumentEventListener";

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public CustomUserUpdatedDocumentEventListener()
    {
        super(NAME, new UserUpdatingDocumentEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        UserUpdatingDocumentEvent userEvent = (UserUpdatingDocumentEvent) event;
        XWikiDocument sourceDoc = (XWikiDocument) source;
        DocumentReference expectedReference = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        if (userEvent.getUserReference() != null
            && StringUtils.equals(userEvent.getUserReference().getName(), "DeleteVersionTestUserCancelEvent")
            && sourceDoc.getDocumentReference().equals(expectedReference)) {
            logger.info("Cancelling user event on purpose");
            userEvent.cancel();
        }
    }
}
