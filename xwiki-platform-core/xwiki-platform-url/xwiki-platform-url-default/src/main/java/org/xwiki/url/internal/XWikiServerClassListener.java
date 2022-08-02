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
package org.xwiki.url.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.url.URLSecurityManager;

import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;

/**
 * Listener for changes on XWikiServerClass xobjects to ensure the {@link URLSecurityManager} cache is invalidated
 * in case of change on XWikiServerClass objects.
 *
 * @version $Id$
 * @since 13.3RC1
 * @since 12.10.7
 */
@Component
@Singleton
@Named(XWikiServerClassListener.NAME)
public class XWikiServerClassListener extends AbstractEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "org.xwiki.url.internal.XWikiServerClassListener";

    private static final LocalDocumentReference XWIKISERVER_CLASS =
        new LocalDocumentReference("XWiki", "XWikiServerClass");

    private static final List<Event> EVENTS = Arrays.asList(
        new XObjectAddedEvent(XWIKISERVER_CLASS),
        new XObjectDeletedEvent(XWIKISERVER_CLASS),
        new XObjectUpdatedEvent(XWIKISERVER_CLASS)
    );

    @Inject
    private URLSecurityManager securityManager;

    /**
     * Default constructor.
     */
    public XWikiServerClassListener()
    {
        super(NAME, EVENTS);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.securityManager instanceof DefaultURLSecurityManager) {
            ((DefaultURLSecurityManager) this.securityManager).invalidateCache();
        }
    }
}
