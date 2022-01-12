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
package org.xwiki.attachment.internal.job;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.attachment.MoveAttachmentRequest;
import org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.refactoring.internal.job.AbstractEntityJob;
import org.xwiki.refactoring.job.EntityJobStatus;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.SOURCE_NAME_FIELD;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.TARGET_LOCATION_FIELD;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.TARGET_NAME_FIELD;

/**
 * TODO: document me.
 *
 * @version $Id$
 * @since X.Y.X
 */
@Component
@Named(MoveAttachmentJob.HINT)
public class MoveAttachmentJob
    extends AbstractEntityJob<MoveAttachmentRequest, EntityJobStatus<MoveAttachmentRequest>>
{
    /**
     * The hint for this job.
     */
    public static final String HINT = "refactoring/attachment/move";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    protected EntityReferenceSerializer<String> referenceSerializer;

    @Override
    public String getType()
    {
        return HINT;
    }

    @Override
    protected void process(EntityReference source)
    {
        this.progressManager.pushLevelProgress(this);
        this.request.setId("refactoring", "moveAttachment",
            String.format("%d-%d", System.currentTimeMillis(), ThreadLocalRandom.current().nextInt(100, 1000)));

        AttachmentReference destination = this.request.getProperty(MoveAttachmentRequest.DESTINATION);
        boolean autoRedirect = this.request.getProperty(MoveAttachmentRequest.AUTO_REDIRECT);

        XWiki wiki = this.xcontextProvider.get().getWiki();
        try {
            XWikiDocument sourceDocument = wiki.getDocument(source.getParent(), this.xcontextProvider.get());
            XWikiDocument targetDocument = wiki.getDocument(destination.getParent(), this.xcontextProvider.get());
            XWikiAttachment sourceAttachment = sourceDocument.getAttachment(source.getName());

            // Remove the original attachment and create a new one with the same name.
            sourceDocument.removeAttachment(sourceAttachment);

            targetDocument.setAttachment(destination.getName(),
                sourceAttachment.getContentInputStream(this.xcontextProvider.get()), this.xcontextProvider.get());

            removeExistingRedirection(destination, targetDocument);

            if (autoRedirect) {
                initializeAutoRedirection(source, destination, sourceDocument);
            }

            // TODO: localize
            if (Objects.equals(source.getParent(), destination.getParent())) {
                wiki.saveDocument(sourceDocument,
                    this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.inPlace",
                        source.getName(), destination.getName()),
                    this.xcontextProvider.get());
            } else {
                // "Attachment moved to " + destination
                String historyMessageSource =
                    this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.source",
                        this.referenceSerializer.serialize(destination));
                String historyMessageTarget =
                    this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.target",
                        this.referenceSerializer.serialize(source));
                wiki.saveDocument(sourceDocument, historyMessageSource, this.xcontextProvider.get());
                wiki.saveDocument(targetDocument, historyMessageTarget, this.xcontextProvider.get());
            }
        } catch (XWikiException e) {
            // TODO: handle exception 
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void initializeAutoRedirection(EntityReference source, AttachmentReference destination,
        XWikiDocument sourceDocument)
        throws XWikiException
    {
        int idx = sourceDocument.createXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE,
            this.xcontextProvider.get());
        BaseObject xObject =
            sourceDocument.getXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE, idx);
        xObject.setStringValue(SOURCE_NAME_FIELD, source.getName());
        xObject.setStringValue(TARGET_LOCATION_FIELD,
            this.entityReferenceSerializer.serialize(destination.getParent()));
        xObject.setStringValue(TARGET_NAME_FIELD, destination.getName());
    }

    private void removeExistingRedirection(AttachmentReference destination, XWikiDocument targetDocument)
    {
        List<BaseObject> targetRedirections =
            targetDocument.getXObjects(RedirectAttachmentClassDocumentInitializer.REFERENCE);
        for (BaseObject targetRedirection : targetRedirections) {
            if (targetRedirection.getStringValue(SOURCE_NAME_FIELD).equals(destination.getName())) {
                targetDocument.removeXObject(targetRedirection);
            }
        }
    }
}
