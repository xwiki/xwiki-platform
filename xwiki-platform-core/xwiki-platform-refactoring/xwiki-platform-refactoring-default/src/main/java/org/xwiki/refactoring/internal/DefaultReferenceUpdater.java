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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.ReferenceRenamer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Update of the reference found in a {@link XWikiDocument}.
 * 
 * @version $Id$
 * @since 14.6RC1
 */
@Component
@Singleton
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class DefaultReferenceUpdater implements ReferenceUpdater
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ReferenceRenamer renamer;

    @Inject
    private ContentParser contentParser;

    /**
     * The component used to record the progress.
     */
    @Inject
    private JobProgressManager progressManager;

    @Inject
    private JobContext jobcontext;

    @Inject
    private Logger logger;

    /**
     * Used to get a {@link BlockRenderer} dynamically.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    private LocalizationManager localizationManager;

    @FunctionalInterface
    private interface RenameLambda
    {
        boolean call(XDOM xdom, DocumentReference currentDocumentReference, boolean relative);
    }

    private boolean isVerbose()
    {
        Job job = this.jobcontext.getCurrentJob();

        if (job != null) {
            return job.getRequest().isVerbose();
        }

        return false;
    }

    private String renderXDOM(XDOM content, BlockRenderer renderer)
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.render(content, printer);

        return printer.toString();
    }

    /**
     * HACK: Prepare the given document to be saved without changing the content author because the document may loose
     * or win programming and script rights as a consequence and this is not the intent of this operation. Even though
     * the document content field was modified, the change is purely syntactic; the semantic is not affected so it's
     * not clear whether the content author deserves to be updated or not (even without the side effects).
     * 
     * @param document the document to be saved
     */
    private void preserveAuthors(XWikiDocument document)
    {
        // Preserve the content author.
        document.setContentDirty(false);
        // Make sure the version is incremented.
        document.setMetaDataDirty(true);
        document.getAuthors().setOriginalMetadataAuthor(
            this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE));
    }

    private boolean renameLinks(XWikiDocument document, boolean relative, RenameLambda renameLambda)
        throws XWikiException
    {
        XDOM xdom = document.getXDOM();

        if (renameLambda.call(xdom, document.getDocumentReference(), relative)) {
            document.setContent(xdom);

            return true;
        }

        return false;
    }

    private boolean renameLinks(BaseObject xobject, XWikiDocument document, BlockRenderer renderer,
        XWikiContext xcontext, boolean relative, RenameLambda renameLambda)
    {
        boolean modified = false;

        BaseClass xclass = xobject.getXClass(xcontext);

        for (Object fieldClass : xclass.getProperties()) {
            // Wiki content stored in xobjects
            if (fieldClass instanceof TextAreaClass textAreaClass && textAreaClass.isWikiContent()) {
                PropertyInterface field = xobject.getField(textAreaClass.getName());

                // Make sure the field is the right type (might happen while a document is being migrated)
                if (field instanceof LargeStringProperty largeField) {
                    try {
                        // Parse property content
                        XDOM xdom = this.contentParser.parse(largeField.getValue(), document.getSyntax(),
                            document.getDocumentReference());

                        // Rename references
                        if (renameLambda.call(xdom, document.getDocumentReference(), relative)) {
                            // Serialize property content
                            largeField.setValue(renderXDOM(xdom, renderer));

                            modified = true;
                        }
                    } catch (Exception e) {
                        this.logger.warn("Failed to rename links from xobject property [{}], skipping it. Error: [{}]",
                            largeField.getReference(), ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            }
        }

        return modified;
    }

    private void info(String format, Object... arguments)
    {
        if (isVerbose()) {
            this.logger.info(format, arguments);
        } else {
            this.logger.debug(format, arguments);
        }
    }

    private void logUpdate(XWikiDocument document, boolean modified, boolean relative, EntityReference oldTarget,
        EntityReference newTarget)
    {
        DocumentReference currentDocumentReference = document.getDocumentReference();
        if (modified) {
            if (relative) {
                info("Updated the relative links from [{}].", currentDocumentReference);
            } else {
                info("The links from [{}] that were targeting [{}] have been updated to target [{}].",
                    document.getDocumentReferenceWithLocale(), oldTarget, newTarget);
            }
        } else {
            if (relative) {
                info("No relative links to update in [{}].", currentDocumentReference);
            } else {
                info("No back-links to update in [{}].", currentDocumentReference);
            }
        }
    }

    private void renameLinks(DocumentReference documentReference, EntityReference oldTarget,
        EntityReference newTarget, XWikiContext xcontext, boolean relative, RenameLambda renameLambda)
        throws XWikiException
    {
        String commentTranslationKey = relative ? "refactoring.referenceUpdater.saveMessage.relativeLink"
            : "refactoring.referenceUpdater.saveMessage.backlinks";
        Locale defaultLocale = this.localizationManager.getDefaultLocale();
        String comment = this.localizationManager.getTranslationPlain(commentTranslationKey, defaultLocale);

        MutableBoolean modified = new MutableBoolean();
        // Update the latest version of the document, so that the links are also renamed when the document is saved
        // concurrently, without losing the concurrent changes.
        XWikiDocument document = xcontext.getWiki().updateDocument(documentReference, documentToModify -> {
            modified.setValue(
                !documentToModify.isNew() && renameLinks(documentToModify, xcontext, relative, renameLambda));
            if (modified.isTrue()) {
                preserveAuthors(documentToModify);
            }
            return modified.booleanValue();
        }, comment, true, xcontext);

        logUpdate(document, modified.booleanValue(), relative, oldTarget, newTarget);
    }

    private boolean renameLinks(XWikiDocument document, XWikiContext xcontext, boolean relative,
        RenameLambda renameLambda) throws XWikiException
    {
        DocumentReference currentDocumentReference = document.getDocumentReference();

        ComponentManager componentManager = this.contextComponentManagerProvider.get();

        // We support only the syntaxes for which there is an available renderer.
        if (!componentManager.hasComponent(BlockRenderer.class, document.getSyntax().toIdString())) {
            this.logger.warn(
                "We can't rename the links from [{}] because there is no renderer available for its syntax [{}].",
                currentDocumentReference, document.getSyntax());

            return false;
        }

        // Load the renderer
        BlockRenderer renderer;
        try {
            renderer = componentManager.getInstance(BlockRenderer.class, document.getSyntax().toIdString());
        } catch (ComponentLookupException e) {
            this.logger.error(
                "We can't rename the links from [{}] because the renderer for syntax [{}] cannot be loaded.",
                currentDocumentReference, document.getSyntax(), e);

            return false;
        }

        // Document content
        boolean modified = renameLinks(document, relative, renameLambda);

        // XObjects properties
        for (List<BaseObject> xobjects : document.getXObjects().values()) {
            for (BaseObject xobject : xobjects) {
                if (xobject != null) {
                    modified |= renameLinks(xobject, document, renderer, xcontext, relative, renameLambda);
                }
            }
        }

        return modified;
    }

    private void renameLinks(DocumentReference documentReference, DocumentReference oldLinkTarget,
        DocumentReference newLinkTarget, boolean relative, Map<EntityReference, EntityReference> updatedEntities)
    {
        internalRenameLinks(documentReference, oldLinkTarget, newLinkTarget, relative, (xdom, currentDocumentReference,
            r) -> this.renamer.renameReferences(xdom, currentDocumentReference, oldLinkTarget, newLinkTarget, r,
            updatedEntities));
    }

    private void renameLinks(DocumentReference documentReference, AttachmentReference oldLinkTarget,
        AttachmentReference newLinkTarget, boolean relative, Map<EntityReference, EntityReference> updatedEntities)
    {
        internalRenameLinks(documentReference, oldLinkTarget, newLinkTarget, relative, (xdom, currentDocumentReference,
            r) ->
            this.renamer.renameReferences(xdom, currentDocumentReference, oldLinkTarget, newLinkTarget, r,
                updatedEntities));
    }

    private void internalRenameLinks(DocumentReference documentReference, EntityReference oldLinkTarget,
        EntityReference newLinkTarget, boolean relative, RenameLambda renameLambda)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        String previousWikiId = xcontext.getWikiId();
        try {
            xcontext.setWikiId(documentReference.getWikiReference().getName());
            List<Locale> locales;
            try {
                locales = xcontext.getWiki().getDocument(documentReference, xcontext).getTranslationLocales(xcontext);
            } catch (XWikiException e) {
                this.logger.error("Failed to load the document [{}] to rename the links that target [{}].",
                    documentReference, oldLinkTarget, e);
                return;
            }

            this.progressManager.pushLevelProgress(1 + locales.size(), this);
            try {
                // Update the default locale instance.
                renameLinksStep(documentReference, oldLinkTarget, newLinkTarget, xcontext, relative, renameLambda);

                // Update the translations.
                if (documentReference.getLocale() == null) {
                    for (Locale locale : locales) {
                        renameLinksStep(new DocumentReference(documentReference, locale), oldLinkTarget,
                            newLinkTarget, xcontext, relative, renameLambda);
                    }
                }
            } finally {
                this.progressManager.popLevelProgress(this);
            }
        } finally {
            xcontext.setWikiId(previousWikiId);
        }
    }

    private void renameLinksStep(DocumentReference documentReference, EntityReference oldLinkTarget,
        EntityReference newLinkTarget, XWikiContext xcontext, boolean relative, RenameLambda renameLambda)
    {
        this.progressManager.startStep(this);
        // Each locale is updated independently so that a locale that fails to be updated doesn't prevent the update
        // of the other locales.
        try {
            renameLinks(documentReference, oldLinkTarget, newLinkTarget, xcontext, relative, renameLambda);
        } catch (XWikiException e) {
            this.logger.error("Failed to rename the links that target [{}] from [{}].", oldLinkTarget,
                documentReference, e);
        }
        this.progressManager.endStep(this);
    }

    private DocumentReference toDocumentReference(EntityReference entityReference)
    {
        return entityReference instanceof DocumentReference documentReference ? documentReference
            : new DocumentReference(entityReference);
    }

    private AttachmentReference toAttachmentReference(EntityReference entityReference)
    {
        return entityReference instanceof AttachmentReference attachmentReference ? attachmentReference
            : new AttachmentReference(entityReference);
    }

    @Override
    public void update(DocumentReference documentReference, EntityReference oldTargetReference,
        EntityReference newTargetReference, Map<EntityReference, EntityReference> updatedEntities)
    {
        // If the current document is the moved entity the links should be serialized relative to it
        boolean relative = newTargetReference.equals(documentReference);

        // Old and new target must be of same type
        if (oldTargetReference.getType() != newTargetReference.getType()) {
            return;
        }

        // Only support documents and attachments targets
        if (oldTargetReference.getType() == EntityType.ATTACHMENT) {
            renameLinks(documentReference, toAttachmentReference(oldTargetReference),
                toAttachmentReference(newTargetReference), relative, updatedEntities);
        } else if (oldTargetReference.getType() == EntityType.DOCUMENT) {
            renameLinks(documentReference, toDocumentReference(oldTargetReference),
                toDocumentReference(newTargetReference), relative, updatedEntities);
        }
    }

    @Override
    public void update(DocumentReference documentReference, EntityReference oldTargetReference,
        EntityReference newTargetReference)
    {
        update(documentReference, oldTargetReference, newTargetReference,
            Map.of(oldTargetReference, newTargetReference));
    }
}
