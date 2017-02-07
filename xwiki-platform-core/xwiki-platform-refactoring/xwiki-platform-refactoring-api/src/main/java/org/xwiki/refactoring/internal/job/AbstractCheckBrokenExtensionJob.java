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
package org.xwiki.refactoring.internal.job;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.question.extension.ExtensionBreakingQuestion;

/**
 * @version $Id$
 */
public abstract class AbstractCheckBrokenExtensionJob<R extends EntityRequest, S extends EntityJobStatus<? super R>> extends
    AbstractEntityJob<R, S>
{
    protected ExtensionBreakingQuestion extensionBreakingQuestion = new ExtensionBreakingQuestion();

    @Inject
    @Named("xar")
    protected InstalledExtensionRepository installedExtensionRepository;

    @Override
    protected void process(Collection<EntityReference> entityReferences)
    {
        this.progressManager.pushLevelProgress(2, this);

        try {
            this.progressManager.startStep(this);
            if (this.request.isInteractive()) {
                checkBrokenExtensions(entityReferences);
                if (this.status.isCanceled()) {
                    return;
                }
            }
            this.progressManager.startStep(this);
            super.process(entityReferences);

        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }


    protected void checkBrokenExtensions(Collection<EntityReference> entityReferences)
    {
        this.progressManager.pushLevelProgress(entityReferences.size(), this);

        try {
            for (EntityReference entityReference : entityReferences) {
                if (this.status.isCanceled()) {
                    break;
                } else {
                    this.progressManager.startStep(this);
                    checkBrokenExtensions(entityReference);
                    this.progressManager.endStep(this);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }

        // Ask question if there are some broken extensions
        if (!this.extensionBreakingQuestion.getExtensions().isEmpty()) {
            try {
                this.status.ask(this.extensionBreakingQuestion);
            } catch (InterruptedException e) {
                this.logger.warn("Confirm question has been interrupted.");
                this.status.cancel();
            }
        }
    }

    protected void checkBrokenExtensions(EntityReference entityReference)
    {
        // Dispatch the check operation based on the entity type.
        switch (entityReference.getType()) {
            case DOCUMENT:
                checkBrokenExtensions(new DocumentReference(entityReference));
                break;
            case SPACE:
                checkBrokenExtensions(new SpaceReference(entityReference));
                break;
            default:
                this.logger.error("Unsupported entity type [{}].", entityReference.getType());
        }
    }

    private void checkBrokenExtensions(DocumentReference documentReference)
    {
        if (this.request.isDeep() && isSpaceHomeReference(documentReference)) {
            checkBrokenExtensions(documentReference.getLastSpaceReference());
        } else {
            checkBrokenExtensionsForDocument(documentReference);
        }
    }

    private void checkBrokenExtensions(SpaceReference spaceReference)
    {
        visitDocuments(spaceReference, new Visitor<DocumentReference>()
        {
            @Override
            public void visit(DocumentReference documentReference)
            {
                checkBrokenExtensionsForDocument(documentReference);
            }
        });
    }

    private void checkBrokenExtensionsForDocument(DocumentReference documentReference)
    {
        XarInstalledExtensionRepository repository = (XarInstalledExtensionRepository) installedExtensionRepository;
        Collection<XarInstalledExtension> extensions = repository.getXarInstalledExtensions(documentReference);

        if (extensions.isEmpty()) {
            this.extensionBreakingQuestion.addFreePage(documentReference);
            return;
        }

        for (XarInstalledExtension extension : extensions) {
            this.extensionBreakingQuestion.addPageFromExtension(extension, documentReference);
        }
    }
}
