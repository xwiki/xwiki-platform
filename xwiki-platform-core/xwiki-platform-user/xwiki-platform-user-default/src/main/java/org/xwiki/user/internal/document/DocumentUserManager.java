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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.user.UserException;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;

/**
 * Document-based implementation of {@link UserManager}.
 *
 * @version $Id$
 * @since 12.2
 */
@Component
@Named(DocumentUserManager.HINT)
@Singleton
public class DocumentUserManager implements UserManager
{
    /**
     * The role hint of the component.
     * 
     * @since 17.4.0RC1
     */
    public static final String HINT = "org.xwiki.user.internal.document.DocumentUserReference";

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    private UserCache userCache;

    @Override
    public boolean exists(UserReference userReference) throws UserException
    {
        boolean result;

        // For the reference to point to an existing user it needs to satisfy 3 conditions:
        // - the wiki of the document exists
        // - the document exists
        // - it contains an XWiki.XWikiUsers xobject
        XWikiContext xcontext = this.xwikiContextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        DocumentReference userDocumentReference = ((DocumentUserReference) userReference).getReference();
        String userWikiId = userDocumentReference.getWikiReference().getName();
        try {
            if (!this.wikiDescriptorManager.exists(userWikiId)) {
                result = false;
            } else {
                result = exists(xcontext, xwiki, userDocumentReference);
            }
        } catch (WikiManagerException e) {
            throw new UserException(String.format("Failed to determine if wiki [%s] exists.", userWikiId), e);
        }
        return result;
    }

    private boolean exists(XWikiContext xcontext, XWiki xwiki, DocumentReference userDocumentReference)
        throws UserException
    {
        boolean result;
        try {
            XWikiDocument document = xwiki.getDocument(userDocumentReference, xcontext);
            result = !document.isNew()
                && document.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE) != null;
        } catch (Exception e) {
            throw new UserException(
                String.format("Failed to check if document [%s] holds an XWiki user or not. ", userDocumentReference),
                e);
        }
        return result;
    }

    @Override
    public boolean hasUsers(WikiReference wiki) throws UserException
    {
        return this.userCache.computeIfAbsent(wiki, this::hasUserInternal);
    }

    private Boolean hasUserInternal(WikiReference wiki) throws UserException
    {
        try {
            Query query = this.queryManager
                .createQuery("select doc.id from Document doc, doc.object(XWiki.XWikiUsers) as user", Query.XWQL);
            query.setLimit(1);
            query.setWiki(wiki.getName());

            return !query.execute().isEmpty();
        } catch (QueryException e) {
            throw new UserException("Failed to query users", e);
        }
    }
}
