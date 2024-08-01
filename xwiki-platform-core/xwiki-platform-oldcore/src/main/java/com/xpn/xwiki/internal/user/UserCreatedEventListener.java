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
package com.xpn.xwiki.internal.user;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Automatically add new users to the configured groups.
 * 
 * @version $Id$
 * @since 16.7.0RC1
 */
@Component
@Singleton
@Named(UserCreatedEventListener.NAME)
// We want to execute it as early as possible so that other listeners end up with a user which have all its expected
// rights
@Priority(500)
public class UserCreatedEventListener extends AbstractEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "com.xpn.xwiki.internal.user.UserCreatedEventListener";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Default constructor.
     */
    public UserCreatedEventListener()
    {
        super(NAME,
            new XObjectAddedEvent(BaseObjectReference.any(XWikiUsersDocumentInitializer.CLASS_REFERENCE_STRING)));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        xwiki.setUserDefaultGroup(((XWikiDocument) source).getDocumentReference(), xcontext);
    }
}
