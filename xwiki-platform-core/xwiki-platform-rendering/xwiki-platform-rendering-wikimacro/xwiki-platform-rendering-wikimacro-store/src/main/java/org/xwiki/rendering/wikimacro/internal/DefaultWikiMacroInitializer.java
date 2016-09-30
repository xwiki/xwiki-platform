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
package org.xwiki.rendering.wikimacro.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.macro.wikibridge.InsufficientPrivilegesException;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroFactory;
import org.xwiki.rendering.macro.wikibridge.WikiMacroInitializer;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A {@link DefaultWikiMacroInitializer} providing wiki macros.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
@Singleton
public class DefaultWikiMacroInitializer implements WikiMacroInitializer, WikiMacroConstants
{
    /**
     * The {@link org.xwiki.rendering.macro.wikibridge.WikiMacroFactory} component.
     */
    @Inject
    private WikiMacroFactory wikiMacroFactory;

    /**
     * The {@link WikiMacroManager} component.
     */
    @Inject
    private WikiMacroManager wikiMacroManager;

    /**
     * The {@link Execution} component used for accessing XWikiContext.
     */
    @Inject
    private Execution execution;

    @Inject
    @Named(WIKI_MACRO_CLASS)
    private MandatoryDocumentInitializer wikiMacroInitializer;

    @Inject
    @Named(WIKI_MACRO_PARAMETER_CLASS)
    private MandatoryDocumentInitializer wikiMacroParameterInitializer;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    private SpaceReferenceResolver<String> spaceReferenceResolver;

    /**
     * Utility method for accessing XWikiContext.
     * 
     * @return the XWikiContext.
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    @Override
    public void registerExistingWikiMacros() throws Exception
    {
        registerExistingWikiMacros(false, null);
    }

    @Override
    public void registerExistingWikiMacros(String wiki) throws Exception
    {
        registerExistingWikiMacros(true, wiki);
    }

    /**
     * Registers the wiki macros for all the wikis or a specific wiki, according to the passed parameter. <br>
     * FIXME: I don't like this way of passing params, but it's kinda the best I can do for the moment without
     * duplicating at least the logic inside this function, if not some code as well.
     * 
     * @param local false if only macros in a specified wiki are to be registered, in which case, the name of the wiki
     *            should be specified in the second parameter, false if the macros in all the wikis should be
     *            registered, in which case the value of the second parameter is ignored
     * @param wiki the name of the wiki to register macros for, if local is true
     * @throws Exception if xwiki classes required for defining wiki macros are missing or if an error occurs while
     *             searching for existing wiki macros.
     */
    private void registerExistingWikiMacros(boolean local, String wiki) throws Exception
    {
        XWikiContext xcontext = getContext();

        // Register the wiki macros that exist
        String originalWiki = xcontext.getWikiId();
        try {
            if (!local) {
                Set<String> wikiNames = new HashSet<String>();
                // Add the list of all subwikis
                wikiNames.addAll(xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext));

                for (String wikiName : wikiNames) {
                    registerMacrosForWiki(wikiName, xcontext);
                }
            } else {
                registerMacrosForWiki(wiki, xcontext);
            }
        } finally {
            xcontext.setWikiId(originalWiki);
        }
    }

    /**
     * Search and register all the macros from the given wiki.
     * 
     * @param wikiName the name of the wiki to process, lowercase database name
     * @param xcontext the current request context
     */
    private void registerMacrosForWiki(String wikiName, XWikiContext xcontext)
    {
        try {
            this.logger.debug("Registering all wiki macros found in wiki [{}]", wikiName);

            // Set the context to be in that wiki so that both the search for XWikiMacro class objects and the
            // registration of macros registered for the current wiki will work.
            // TODO: In the future when we have APIs for it, move the code to set the current wiki and the current user
            // (see below) to the WikiMacroManager's implementation.
            xcontext.setWikiId(wikiName);

            // Make sure classes exists and are up to date in this wiki
            installOrUpgradeWikiMacroClasses();

            // Search for all those documents with macro definitions and for each register the macro
            for (Object[] wikiMacroDocumentData : getWikiMacroDocumentData(xcontext)) {
                // In the database the space and page names are always specified for a document. However the wiki
                // part isn't, so we need to replace the wiki reference with the current wiki.
                // Note that the space part can contain one or more spaces since XWiki 7.2 and the introduction of
                // Nested Spaces.
                SpaceReference spaceReference =
                    this.spaceReferenceResolver.resolve((String) wikiMacroDocumentData[0], new WikiReference(wikiName));
                DocumentReference wikiMacroDocumentReference =
                    new DocumentReference((String) wikiMacroDocumentData[1], spaceReference);

                registerMacro(wikiMacroDocumentReference, (String) wikiMacroDocumentData[2], xcontext);
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to register macros for wiki [{}]: {}", wikiName, ex.getMessage());
        }
    }

    /**
     * Search for all wiki macros in the current wiki.
     * 
     * @param xcontext the current request context
     * @return a list of documents containing wiki macros, each item as a List of 3 strings: space name, document name,
     *         last author of the document
     * @throws Exception if the database search fails
     */
    private List<Object[]> getWikiMacroDocumentData(XWikiContext xcontext) throws Exception
    {
        final QueryManager qm = xcontext.getWiki().getStore().getQueryManager();
        final Query q = qm.getNamedQuery("getWikiMacroDocuments");
        return (List<Object[]>) (List) q.execute();
    }

    /**
     * Register a wiki macro in the component manager, if the macro author has the required rights.
     * 
     * @param wikiMacroDocumentReference the document holding the macro definition
     * @param wikiMacroDocumentAuthor the author of the macro document
     * @param xcontext the current request context
     */
    private void registerMacro(DocumentReference wikiMacroDocumentReference, String wikiMacroDocumentAuthor,
        XWikiContext xcontext)
    {
        this.logger.debug("Registering macro in document [{}]...", wikiMacroDocumentReference);

        DocumentReference originalAuthor = xcontext.getUserReference();
        try {
            WikiMacro macro = this.wikiMacroFactory.createWikiMacro(wikiMacroDocumentReference);

            this.wikiMacroManager.registerWikiMacro(wikiMacroDocumentReference, macro);

            this.logger.debug("Macro [{}] from document [{}] is now registered.",
                macro.getDescriptor().getId().getId(), wikiMacroDocumentReference);
        } catch (InsufficientPrivilegesException ex) {
            // Just log the exception and skip to the next.
            // We only log at the debug level here as this is not really an error
            this.logger.debug(ex.getMessage(), ex);
        } catch (WikiMacroException ex) {
            // Just log the exception and skip to the next.
            this.logger.error(ex.getMessage(), ex);
        } finally {
            xcontext.setUserReference(originalAuthor);
        }
    }

    @Override
    public void installOrUpgradeWikiMacroClasses() throws Exception
    {
        XWikiContext xcontext = getContext();

        // Install or Upgrade XWiki.WikiMacroClass
        XWikiDocument doc = xcontext.getWiki().getDocument(this.wikiMacroInitializer.getDocumentReference(), xcontext);

        if (this.wikiMacroInitializer.updateDocument(doc)) {
            update(doc);
        }

        // Install or Upgrade XWiki.WikiMacroParameterClass
        doc = xcontext.getWiki().getDocument(WIKI_MACRO_PARAMETER_CLASS, xcontext);

        if (this.wikiMacroParameterInitializer.updateDocument(doc)) {
            update(doc);
        }
    }

    /**
     * Utility method for updating a wiki macro class definition document.
     * 
     * @param doc xwiki document containing the wiki macro class.
     * @throws XWikiException if an error occurs while saving the document.
     */
    private void update(XWikiDocument doc) throws Exception
    {
        XWikiContext xcontext = getContext();

        if (doc.isNew()) {
            doc.setParent("XWiki.WebHome");
        }

        xcontext.getWiki().saveDocument(doc, xcontext);
    }
}
