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
package org.xwiki.refactoring.internal.listener;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.MoveRequest;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Updates the xobjects of an xclass after the xclass has been renamed.
 * 
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Named(UpdateObjectsOnClassRenameListener.NAME)
@Singleton
public class UpdateObjectsOnClassRenameListener extends AbstractLocalEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "refactoring.updateObjectsOnClassRename";

    @Inject
    private Logger logger;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("unique")
    private QueryFilter uniqueFilter;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private JobProgressManager progressManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Default constructor.
     */
    public UpdateObjectsOnClassRenameListener()
    {
        super(NAME, new DocumentRenamedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        boolean updateLinks = true;
        if (data instanceof MoveRequest) {
            updateLinks = ((MoveRequest) data).isUpdateLinks();
        }
        if (updateLinks) {
            DocumentRenamedEvent documentRenamedEvent = (DocumentRenamedEvent) event;
            updateObjects(documentRenamedEvent.getSourceReference(), documentRenamedEvent.getTargetReference());
        }
    }

    private void updateObjects(DocumentReference oldClassReference, DocumentReference newClassReference)
    {
        if (!newClassReference.getWikiReference().equals(oldClassReference.getWikiReference())) {
            // We can't update the xobjects if the xclass has been moved to a different wiki because xclasses have local
            // scope (we can't use the xclass in a different wiki than the one where the xclass is stored).
            return;
        }

        try {
            Query query = this.queryManager.createQuery(
                ", BaseObject as obj where doc.fullName = obj.name and obj.className = :className", Query.HQL);
            query.addFilter(this.uniqueFilter);
            query.setWiki(oldClassReference.getWikiReference().getName());
            query.bindValue("className", this.localEntityReferenceSerializer.serialize(oldClassReference));
            List<DocumentReference> documentsToUpdate = query.<String>execute().stream()
                .map(fullName -> this.documentReferenceResolver.resolve(fullName, oldClassReference))
                .collect(Collectors.toList());
            if (!documentsToUpdate.isEmpty()) {
                updateObjects(documentsToUpdate, oldClassReference, newClassReference);
            }
        } catch (QueryException e) {
            this.logger.error("Failed to update the xobjects of type [{}] after the xclass has been renamed to [{}].",
                oldClassReference, newClassReference, e);
        }
    }

    private void updateObjects(List<DocumentReference> documentsToUpdate, DocumentReference oldClassReference,
        DocumentReference newClassReference)
    {
        this.logger.info("Updating the xobjects of type [{}] after the xclass has been renamed to [{}].",
            oldClassReference, newClassReference);
        this.progressManager.pushLevelProgress(documentsToUpdate.size(), this);

        try {
            for (DocumentReference documentReference : documentsToUpdate) {
                this.progressManager.startStep(this);
                try {
                    updateObjects(documentReference, oldClassReference, newClassReference);
                } catch (XWikiException e) {
                    this.logger.error(
                        "Failed to update the xobjects from [{}] after the xclass [{}] has been renamed to [{}].",
                        documentReference, oldClassReference, newClassReference, e);
                }
                this.progressManager.endStep(this);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void updateObjects(DocumentReference documentReference, DocumentReference oldClassReference,
        DocumentReference newClassReference) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        for (BaseObject oldObject : document.getXObjects(oldClassReference)) {
            if (oldObject != null) {
                BaseObject newObject = document.newXObject(newClassReference, xcontext);
                for (Object property : oldObject.getProperties()) {
                    if (property instanceof BaseProperty) {
                        BaseProperty<?> baseProperty = (BaseProperty<?>) property;
                        // Clone the property because this instance is going to be deleted.
                        newObject.safeput(baseProperty.getName(), baseProperty.clone());
                    }
                }
                document.removeXObject(oldObject);
            }
        }
        // Note that we haven't checked if the current user has edit right on the document we're about to save because
        // we consider the operation of updating the objects on class rename to be a low level system operation. Also
        // note that we haven't changed the author of the document because we don't want to change the way the code from
        // the updated objects is evaluated.
        xcontext.getWiki().saveDocument(document,
            String.format("Rename [%s] objects into [%s]", oldClassReference, newClassReference), xcontext);
    }
}
