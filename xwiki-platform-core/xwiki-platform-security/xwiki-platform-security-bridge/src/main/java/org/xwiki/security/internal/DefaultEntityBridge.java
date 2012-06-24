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
package org.xwiki.security.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Temporary implementation of the (@link EntityBridge} interface to access entity information.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultEntityBridge implements EntityBridge
{
    /** Execution object. */
    @Inject
    private Execution execution;

    /**
     * @return the current {@code XWikiContext}
     */
    private XWikiContext getXWikiContext() {
        return ((XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY));
    }

    @Override
    public boolean isDocumentCreator(UserSecurityReference user, SecurityReference entity)
    {
        if (user == null || entity == null) {
            return false;
        }

        DocumentReference documentReference = entity.getOriginalDocumentReference();
        if (documentReference == null) {
            return false;
        }

        XWikiContext context = getXWikiContext();
        XWikiDocument document;
        try {
            document = context.getWiki().getDocument(documentReference, context);
        } catch (XWikiException e) {
            return false;
        }

        if (document == null) {
            return false;
        }

        DocumentReference creator = document.getCreatorReference();
        return user.getOriginalReference().equals(creator);
    }
}
