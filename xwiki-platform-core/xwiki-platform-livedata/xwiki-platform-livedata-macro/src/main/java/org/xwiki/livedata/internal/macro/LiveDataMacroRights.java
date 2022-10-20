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
package org.xwiki.livedata.internal.macro;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;

import static org.xwiki.security.authorization.Right.SCRIPT;

/**
 * Provides the services related to rights for {@link LiveDataMacro}.
 *
 * @version $Id$
 * @since 14.9
 * @since 14.4.7
 * @since 13.10.10
 */
@Component(roles = LiveDataMacroRights.class)
@Singleton
public class LiveDataMacroRights
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * @return {@code true} if the last author of the document where the Live Data macro is executed has script rights
     *     on said document, {@code false} otherwise
     */
    public boolean authorHasScriptRight()
    {
        DocumentReference currentDocumentReference = this.documentAccessBridge.getCurrentDocumentReference();
        DocumentReference currentAuthorReference = this.documentAccessBridge.getCurrentAuthorReference();
        return this.authorizationManager.hasAccess(SCRIPT, currentAuthorReference, currentDocumentReference);
    }
}
