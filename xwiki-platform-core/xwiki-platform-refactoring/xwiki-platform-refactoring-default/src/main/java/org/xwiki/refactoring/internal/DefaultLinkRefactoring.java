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
package org.xwiki.refactoring.internal;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Default implementation of {@link LinkRefactoring}.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Singleton
public class DefaultLinkRefactoring implements LinkRefactoring
{
    @Inject
    private Logger logger;

    /**
     * Used to perform the low level operations on entities.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * The component used to record the progress.
     */
    @Inject
    private JobProgressManager progressManager;

    /**
     * Used to get a {@link BlockRenderer} dynamically.
     *
     * @see #updateRelativeLinks(XWikiDocument, DocumentReference)
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    private ContentParser contentParser;

    @Inject
    private ReferenceRenamer renamer;

    @Override
    public void renameLinks(DocumentReference documentReference, DocumentReference oldLinkTarget,
        DocumentReference newLinkTarget)
    {
        boolean popLevelProgress = false;
        XWikiContext xcontext = this.xcontextProvider.get();
        String previousWikiId = xcontext.getWikiId();
        try {
            xcontext.setWikiId(documentReference.getWikiReference().getName());
            XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
            List<Locale> locales = document.getTranslationLocales(xcontext);

            this.progressManager.pushLevelProgress(1 + locales.size(), this);
            popLevelProgress = true;

            // Update the default locale instance.
            this.progressManager.startStep(this);
            renameLinks(document, oldLinkTarget, newLinkTarget, xcontext, false);
            this.progressManager.endStep(this);

            // Update the translations.
            for (Locale locale : locales) {
                this.progressManager.startStep(this);
                renameLinks(document.getTranslatedDocument(locale, xcontext), oldLinkTarget, newLinkTarget, xcontext,
                    false);
                this.progressManager.endStep(this);
            }
        } catch (XWikiException e) {
            this.logger.error("Failed to rename the links that target [{}] from [{}].", oldLinkTarget,
                documentReference, e);
        } finally {
            if (popLevelProgress) {
                this.progressManager.popLevelProgress(this);
            }
            xcontext.setWikiId(previousWikiId);
        }
    }

    private void renameLinks(XWikiDocument document, DocumentReference oldTarget, DocumentReference newTarget,
        XWikiContext xcontext, boolean relative) throws XWikiException
    {
        DocumentReference currentDocumentReference = document.getDocumentReference();

        ComponentManager componentManager = this.contextComponentManagerProvider.get();

        // We support only the syntaxes for which there is an available renderer.
        if (!componentManager.hasComponent(BlockRenderer.class, document.getSyntax().toIdString())) {
            this.logger.warn(
                "We can't rename the links from [{}] because there is no renderer available for its syntax [{}].",
                currentDocumentReference, document.getSyntax());

            return;
        }

        // Load the renderer
        BlockRenderer renderer;
        try {
            renderer = componentManager.getInstance(BlockRenderer.class, document.getSyntax().toIdString());
        } catch (ComponentLookupException e) {
            this.logger.error(
                "We can't rename the links from [{}] because the renderer for syntax [{}] cannot be loaded.",
                currentDocumentReference, document.getSyntax(), e);

            return;
        }

        // Document content
        boolean modified = renameLinks(document, oldTarget, newTarget, relative);

        // XObjects properties
        for (List<BaseObject> xobjects : document.getXObjects().values()) {
            for (BaseObject xobject : xobjects) {
                modified |= renameLinks(xobject, document, oldTarget, newTarget, renderer, xcontext, relative);
            }
        }

        if (modified) {
            if (relative) {
                saveDocumentPreservingContentAuthor(document, "Updated the relative links.", true);

                this.logger.info("Updated the relative links from [{}].", currentDocumentReference);
            } else {
                saveDocumentPreservingContentAuthor(document, "Renamed back-links.", false);

                this.logger.info("The links from [{}] that were targeting [{}] have been updated to target [{}].",
                    document.getDocumentReferenceWithLocale(), oldTarget, newTarget);
            }
        } else {
            if (relative) {
                this.logger.info("No relative links to update in [{}].", currentDocumentReference);
            } else {
                this.logger.info("No back-links to update in [{}].", currentDocumentReference);
            }
        }
    }

    private boolean renameLinks(XWikiDocument document, DocumentReference oldTarget, DocumentReference newTarget,
        boolean relative) throws XWikiException
    {
        XDOM xdom = document.getXDOM();

        if (renameLinks(xdom, document.getDocumentReference(), oldTarget, newTarget, relative)) {
            document.setContent(xdom);

            return true;
        }

        return false;
    }

    private boolean renameLinks(XDOM xdom, DocumentReference currentDocumentReference, DocumentReference oldTarget,
        DocumentReference newTarget, boolean relative)
    {
        if (relative) {
            return this.renamer.updateRelativeReferences(xdom, oldTarget, newTarget);
        }

        return this.renamer.renameReferences(xdom, currentDocumentReference, oldTarget, newTarget);
    }

    private boolean renameLinks(BaseObject xobject, XWikiDocument document, DocumentReference oldTarget,
        DocumentReference newTarget, BlockRenderer renderer, XWikiContext xcontext, boolean relative)
    {
        boolean modified = false;

        BaseClass xclass = xobject.getXClass(xcontext);

        for (Object fieldClass : xclass.getProperties()) {
            // Wiki content stored in xobjects
            if (fieldClass instanceof TextAreaClass && ((TextAreaClass) fieldClass).isWikiContent()) {
                TextAreaClass textAreaClass = (TextAreaClass) fieldClass;
                PropertyInterface field = xobject.getField(textAreaClass.getName());

                // Make sure the field is the right type (might happen while a document is being migrated)
                if (field instanceof LargeStringProperty) {
                    LargeStringProperty largeField = (LargeStringProperty) field;

                    try {
                        // Parse property content
                        XDOM xdom = this.contentParser.parse(largeField.getValue(), document.getSyntax(),
                            document.getDocumentReference());

                        // Rename references
                        if (renameLinks(xdom, document.getDocumentReference(), oldTarget, newTarget, relative)) {
                            // Serialize property content
                            largeField.setValue(renderXDOM(xdom, renderer));

                            modified = true;
                        }
                    } catch (Exception e) {
                        this.logger.warn("Failed to rename links from xobject property [{}], skipping it. Error: {}",
                            largeField.getReference(), ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            }
        }

        return modified;
    }

    private String renderXDOM(XDOM content, BlockRenderer renderer)
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(content, printer);

        return printer.toString();
    }

    @Override
    public void updateRelativeLinks(DocumentReference oldReference, DocumentReference newReference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            XWikiDocument document = xcontext.getWiki().getDocument(newReference, xcontext);
            renameLinks(document, oldReference, document.getDocumentReference(), xcontext, true);
        } catch (XWikiException e) {
            this.logger.error("Failed to update the relative links from [{}].", newReference, e);
        }
    }

    /**
     * HACK: Save the given document without changing the content author because the document may loose or win
     * programming and script rights as a consequence and this is not the intent of this operation. Even though the
     * document content field was modified, the change is purely syntactic; the semantic is not affected so it's not
     * clear whether the content author deserves to be updated or not (even without the side effects).
     * 
     * @param document the document to be saved
     * @param comment the revision comment
     * @param minorEdit whether it's a minor edit or not
     * @throws XWikiException if saving the document fails
     */
    private void saveDocumentPreservingContentAuthor(XWikiDocument document, String comment, boolean minorEdit)
        throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        // Preserve the content author.
        document.setContentDirty(false);
        // Make sure the version is incremented.
        document.setMetaDataDirty(true);
        document.setAuthorReference(xcontext.getUserReference());
        xcontext.getWiki().saveDocument(document, comment, minorEdit, xcontext);
    }
}
