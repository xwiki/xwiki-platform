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
package org.xwiki.internal.web;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequest;

/**
 * Sets the currently authenticated user as the effective author of the request, if the effective author has not been
 * set already by another component.
 *
 * @version $Id$
 * @since 15.10.11
 * @since 16.4.1
 * @since 16.5.0
 */
@Component
@Singleton
@Named(EffectiveAuthorSetterListener.HINT)
public class EffectiveAuthorSetterListener extends AbstractLocalEventListener
{
    /**
     * The hint of the listener component.
     */
    public static final String HINT = "com.xpn.xwiki.internal.web.EffectiveAuthorSetterListener";

    @Inject
    private Logger logger;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Default constructor.
     */
    public EffectiveAuthorSetterListener()
    {
        super(HINT, new ActionExecutingEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiRequest request = xcontext.getRequest();
        if (request.getAttribute(XWikiServletRequest.ATTRIBUTE_EFFECTIVE_AUTHOR) == null) {
            UserReference authenticatedUser = this.currentUserResolver.resolve(CurrentUserReference.INSTANCE);
            this.logger.debug("Setting the effective author of the request to the currently authenticated user [{}].",
                authenticatedUser);
            request.setAttribute(XWikiServletRequest.ATTRIBUTE_EFFECTIVE_AUTHOR, authenticatedUser);
        }
    }
}
