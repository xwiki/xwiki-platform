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
package org.xwiki.user.internal.document;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Invalidate the cache based on user events.
 * 
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component
@Named(UserListener.NAME)
@Singleton
public class UserListener extends AbstractEventListener
{
    /**
     * The hint of the component and the name of the listener.
     */
    public static final String NAME = "org.xwiki.user.internal.document.UserListener";

    private static final RegexEntityReference USER_REFERENCE =
        BaseObjectReference.any(XWikiUsersDocumentInitializer.CLASS_REFERENCE_STRING);

    @Inject
    private UserCache cache;

    /**
     * Listener to user creation/deleting.
     */
    public UserListener()
    {
        super(NAME, new XObjectAddedEvent(USER_REFERENCE), new XObjectDeletedEvent(USER_REFERENCE),
            new WikiDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof XObjectEvent) {
            XWikiDocument document = (XWikiDocument) source;
            WikiReference wiki = document.getDocumentReference().getWikiReference();
            if (event instanceof XObjectAddedEvent) {
                this.cache.set(wiki, true);
            } else if (event instanceof XObjectDeletedEvent) {
                this.cache.invalidate(wiki);
            }
        } else if (event instanceof WikiDeletedEvent wikiEvent) {
            this.cache.invalidate(new WikiReference(wikiEvent.getWikiId()));
        }
    }
}
