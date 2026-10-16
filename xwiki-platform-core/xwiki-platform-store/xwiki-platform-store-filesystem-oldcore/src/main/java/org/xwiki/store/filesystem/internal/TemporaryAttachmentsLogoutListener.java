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
package org.xwiki.store.filesystem.internal;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;

/**
 * Deletes all the temporary attachments of the browser at logout and forgets its scope, so that nothing uploaded
 * before the logout stays reachable or stored.
 * <p>
 * When the logout is performed, the user is already logged out when the logout action starts, so the whole scope of the
 * browser is deleted, whatever the user who uploaded. Nothing is done when the user is still authenticated (the
 * authenticator did not perform the logout) or when the logout was refused because of an invalid CSRF token. An
 * authenticator that logs out without the logout action leaves the uploads to the age-based cleanup.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Named(TemporaryAttachmentsLogoutListener.NAME)
@Singleton
public class TemporaryAttachmentsLogoutListener extends AbstractLocalEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.store.filesystem.internal.TemporaryAttachmentsLogoutListener";

    /**
     * Set in the context by the authenticator when the logout is refused because of an invalid CSRF token.
     */
    private static final String INVALID_CSRF_KEY = "core.logout.error.invalidCSRF";

    @Inject
    private TemporaryAttachmentScopeManager scopeManager;

    @Inject
    private TemporaryAttachmentStore store;

    /**
     * Default constructor.
     */
    public TemporaryAttachmentsLogoutListener()
    {
        super(NAME, new ActionExecutingEvent("logout"));
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        // The logout is not performed when the CSRF token is invalid, and the user is still authenticated when the
        // authenticator did not process the logout (for example a logout URL without a form token).
        if (!(data instanceof XWikiContext context) || context.get(INVALID_CSRF_KEY) != null
            || context.getUserReference() != null) {
            return;
        }

        Optional<String> scopeId = this.scopeManager.getScopeId();
        if (scopeId.isPresent()) {
            this.store.deleteAll(this.store.getScopePath(scopeId.get()));
            this.scopeManager.resetScope();
        }
    }
}
