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
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilderException;
import org.xwiki.wiki.internal.descriptor.document.DefaultWikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;

/**
 * Default implementation for {@link WikiManager}.
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Singleton
public class DefaultWikiManager implements WikiManager
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DefaultWikiDescriptorDocumentHelper descriptorDocumentHelper;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private Logger logger;

    @Inject
    private WikiDescriptorBuilder wikiDescriptorBuilder;

    private WikiDescriptor createDescriptor(String wikiId, String wikiAlias) throws WikiManagerException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        // Create the descriptor
        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor(wikiId, wikiAlias);

        try {
            // Build the document
            XWikiDocument descriptorDocument = wikiDescriptorBuilder.buildDescriptorDocument(descriptor);
            // Save the document
            xwiki.getStore().saveXWikiDoc(descriptorDocument, context);
            // Add the document to the descriptor
            descriptor.setDocumentReference(descriptorDocument.getDocumentReference());
        } catch (WikiDescriptorBuilderException e) {
            throw new WikiManagerException("Failed to build the descriptor document.", e);
        } catch (XWikiException e) {
            throw new WikiManagerException("Failed to save the descriptor document.", e);
        }

        return descriptor;
    }

    @Override
    public WikiDescriptor create(String wikiId, String wikiAlias) throws WikiManagerException
    {
        // Check that the wiki Id is available
        if (!idAvailable(wikiId)) {
            throw new WikiManagerException("wiki id is not valid");
        }

        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        // Create database/schema
        try {
            xwiki.getStore().createWiki(wikiId, context);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new WikiManagerException(localizationManager.getTranslationPlain("wiki.databasecreation"));
        }

        // Init database/schema
        try {
            xwiki.updateDatabase(wikiId, true, true, context);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new WikiManagerException(localizationManager.getTranslationPlain("wiki.databaseupdate"));
        }

        return createDescriptor(wikiId, wikiAlias);
    }

    @Override
    public WikiDescriptor copy(String fromWikiId, String newWikiId, String newWikiAlias, boolean copyHistory,
            boolean copyRecycleBin) throws WikiManagerException
    {
        // Verify that the newId is valid
        if (!idAvailable(newWikiId)) {
            throw new WikiManagerException(String.format("Id [%s] is not avalaible.", newWikiId));
        }

        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        // Copy all the wiki
        try {
            xwiki.copyWiki(fromWikiId, newWikiId, null, context);
        } catch (XWikiException e) {
            throw new WikiManagerException("Failed to copy the wiki.", e);
        }

        return createDescriptor(newWikiId, newWikiAlias);
    }

    @Override
    public void delete(String wikiId) throws WikiManagerException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        // Check if we try to delete the main wiki
        if (wikiId.equals(wikiDescriptorManager.getMainWikiId())) {
            throw new WikiManagerException("can't delete main wiki");
        }

        // Delete the database
        try {
            xwiki.getStore().deleteWiki(wikiId, context);
        } catch (XWikiException e) {
            throw new WikiManagerException("can't delete database");
        }

        // Delete the descriptor document
        try {
            XWikiDocument descriptorDocument = descriptorDocumentHelper.getDocumentFromWikiId(wikiId);
            xwiki.deleteDocument(descriptorDocument, context);
        } catch (XWikiException e) {
            throw new WikiManagerException("can't delete descriptor document");
        }

        // Send an event
        observationManager.notify(new WikiDeletedEvent(wikiId), wikiId);
    }

    @Override
    public boolean idAvailable(String wikiId) throws WikiManagerException {
        //TODO: look if the id is valid and free (the database does not already exists, for example)
        String wikiForbiddenList = xcontextProvider.get().getWiki().Param("xwiki.virtual.reserved_wikis");
        return !wikiDescriptorManager.exists(wikiId) && !Util.contains(wikiId, wikiForbiddenList, ", ");
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
