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
package org.xwiki.test.checker.internal;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.internal.BridgeAuthorizationManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Override {@link BridgeAuthorizationManager} to forbid programming right in scripts.
 * 
 * @version $Id$
 */
public class ProgrammingRightCheckerAuthorizationManager extends BridgeAuthorizationManager
{
    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private Logger logger;

    @Override
    public void checkAccess(Right right, DocumentReference userReference, EntityReference entityReference)
        throws AccessDeniedException
    {
        super.checkAccess(right, userReference, entityReference);

        if (!check(right)) {
            throw new AccessDeniedException(right, userReference, entityReference);
        }
    }

    @Override
    public boolean hasAccess(Right right, DocumentReference userReference, EntityReference entityReference)
    {
        boolean hasAccess = super.hasAccess(right, userReference, entityReference);

        if (hasAccess && !check(right)) {
            return false;
        }

        return hasAccess;
    }

    private boolean check(Right right)
    {
        if (right == Right.PROGRAM) {
            XWikiContext xcontext = this.xwikiContextProvider.get();
            if (xcontext != null) {
                DocumentReference document =
                    (DocumentReference) xcontext.get(ProgrammingRightCheckerListener.PRCHECK_KEY);

                this.logger.debug("PRChecker: Block programming right for page [{}]", document);

                return false;
            }
        }

        return true;
    }
}
