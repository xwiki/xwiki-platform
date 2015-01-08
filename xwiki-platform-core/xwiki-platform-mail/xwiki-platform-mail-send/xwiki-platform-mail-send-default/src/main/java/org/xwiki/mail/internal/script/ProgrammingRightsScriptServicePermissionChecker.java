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
package org.xwiki.mail.internal.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.script.ScriptServicePermissionChecker;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * The current document must have Programming Rights for emails to be authorized for sending.
 *
 * @version $Id$
 * @since 6.4M2
 */
@Component
@Named("programmingrights")
@Singleton
public class ProgrammingRightsScriptServicePermissionChecker implements ScriptServicePermissionChecker
{
    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Override
    public void check() throws MessagingException
    {
        // If the current document doesn't have Programming Rights then do not authorize the mail to be sent.
        try {
            this.authorizationManager.checkAccess(Right.PROGRAM);
        } catch (AccessDeniedException e) {
            throw new MessagingException("The Current document doesn't have the Programming Rights", e);
        }
    }
}
