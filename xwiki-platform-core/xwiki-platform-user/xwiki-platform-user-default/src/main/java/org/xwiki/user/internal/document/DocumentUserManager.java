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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Document-based implementation of {@link UserManager}.
 *
 * @version $Id$
 * @since 12.2
 */
@Component
@Named("org.xwiki.user.internal.document.DocumentUserReference")
@Singleton
public class DocumentUserManager implements UserManager
{
    private static final LocalDocumentReference USERS_XCLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "XWikiUsers");

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Override
    public boolean exists(UserReference userReference)
    {
        boolean result;

        // For the reference to point to an existing user it needs to satisfy 2 conditions:
        // - the document exists
        // - it contains an XWiki.XWikiUsers xobject
        XWikiContext xcontext = this.xwikiContextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        DocumentReference userDocumentReference = ((DocumentUserReference) userReference).getReference();
        try {
            XWikiDocument document = xwiki.getDocument(userDocumentReference, xcontext);
            result = !document.isNew() && document.getXObject(USERS_XCLASS_REFERENCE) != null;
        } catch (Exception e) {
            this.logger.warn(String.format("Failed to check if document [%s] holds an XWiki user or not. "
                    + "Considering it's not the case. Root error: [%s]", userDocumentReference,
                ExceptionUtils.getRootCauseMessage(e)));
            result = false;
        }
        return result;
    }
}
