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

import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiCopiedEvent;
import org.xwiki.bridge.event.WikiCreateFailedEvent;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiCreatingEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.provisioning.WikiCopier;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;

/**
 * Default implementation for {@link WikiManager}.
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Singleton
public class DefaultWikiManager implements WikiManager
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private Logger logger;

    @Inject
    private WikiCreator wikiCreator;

    @Inject
    private WikiCopier wikiCopier;

    @Inject
    private WikiDeleter wikiDeleter;

    @Override
    public WikiDescriptor create(String wikiId, String wikiAlias, boolean failOnExist) throws WikiManagerException
    {
        // Check that the wiki Id is available
        if (failOnExist && !idAvailable(wikiId)) {
            throw new WikiManagerException(String.format("wiki id [%s] is already used and is thus not available",
                wikiId));
        }

        XWikiContext context = xcontextProvider.get();
        WikiDescriptor descriptor;

        try {
            // Send the begin event
            observationManager.notify(new WikiCreatingEvent(wikiId), wikiId, context);

            // Create the wiki
            descriptor = wikiCreator.create(wikiId, wikiAlias);

            // Send the end event
            observationManager.notify(new WikiCreatedEvent(wikiId), wikiId, context);

        } catch (WikiManagerException e) {
            // Send the failed event
            observationManager.notify(new WikiCreateFailedEvent(wikiId), wikiId, context);

            // Throw the exception
            throw e;
        }

        return descriptor;
    }

    @Override
    public WikiDescriptor copy(String fromWikiId, String newWikiId, String newWikiAlias, boolean copyHistory,
            boolean copyRecycleBin, boolean failOnExist) throws WikiManagerException
    {
        WikiDescriptor newWiki = create(newWikiId, newWikiAlias, failOnExist);
        wikiCopier.copyDocuments(fromWikiId, newWikiId, copyHistory);
        if (copyRecycleBin) {
            wikiCopier.copyDeletedDocuments(fromWikiId, newWikiId);
        }
        observationManager.notify(new WikiCopiedEvent(fromWikiId, newWikiId), fromWikiId, xcontextProvider.get());
        return newWiki;
    }

    @Override
    public WikiDescriptor rename(String wikiId, String newWikiId) throws WikiManagerException
    {
        throw new WikiManagerException("This method is not implemented yet");
    }

    @Override
    public void delete(String wikiId) throws WikiManagerException
    {
        // Delete the wiki
        wikiDeleter.delete(wikiId);

        // Send an event
        observationManager.notify(new WikiDeletedEvent(wikiId), wikiId);
    }

    @Override
    public boolean idAvailable(String wikiId) throws WikiManagerException {
        // Get the store
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        // Get the forbidden list
        String wikiForbiddenList = xcontextProvider.get().getWiki().Param("xwiki.virtual.reserved_wikis");
        try {
            return !wikiDescriptorManager.exists(wikiId) && !Util.contains(wikiId, wikiForbiddenList, ", ")
                    && xwiki.getStore().isWikiNameAvailable(wikiId, xcontext);
        } catch (XWikiException e) {
            throw new WikiManagerException("Fail to look at the databases.");
        }
    }

    private XWikiDocument getDocument(DocumentReference reference) throws WikiManagerException
    {
        XWikiContext context = xcontextProvider.get();
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        try {
            return xwiki.getDocument(reference, context);
        } catch (XWikiException e) {
            throw new WikiManagerException(String.format(
                    "Failed to get document [%s] containing a XWiki.XWikiServerClass object", reference), e);
        }
    }

}
