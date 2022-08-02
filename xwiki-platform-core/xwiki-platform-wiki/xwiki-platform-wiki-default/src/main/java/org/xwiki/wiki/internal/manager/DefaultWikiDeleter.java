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
package org.xwiki.wiki.internal.manager;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link WikiDeleter}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWikiDeleter implements WikiDeleter
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private WikiDescriptorDocumentHelper descriptorDocumentHelper;

    @Override
    public void delete(String wikiId) throws WikiManagerException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        // Check if we try to delete the main wiki
        if (wikiId.equals(wikiDescriptorManager.getMainWikiId())) {
            throw new WikiManagerException("It's not allowed to delete the main wiki!");
        }

        // Delete the descriptor document
        try {
            XWikiDocument descriptorDocument = descriptorDocumentHelper.getDocumentFromWikiId(wikiId);
            xwiki.deleteDocument(descriptorDocument, context);
        } catch (XWikiException e) {
            throw new WikiManagerException(
                String.format("Error deleting the Wiki Descriptor Document for database [%s]", wikiId), e);
        }

        // Delete the database
        try {
            xwiki.getStore().deleteWiki(wikiId, context);
        } catch (XWikiException e) {
            throw new WikiManagerException(String.format("Error deleting the database [%s]", wikiId), e);
        }
    }
}
