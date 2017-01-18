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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.xwiki.refactoring.job.ExtensionBreakingQuestion;

/**
 * @version $Id$
 */
public abstract class AbstractCheckBrokenExtensionJob<R extends EntityRequest, S extends EntityJobStatus<? super R>> extends
    AbstractEntityJob<R, S>
{
    protected Map<XarInstalledExtension, List<DocumentReference>> brokenExtensions = new HashMap<>();

    @Inject
    @Named("xar")
    protected InstalledExtensionRepository installedExtensionRepository;

    @Override
    protected void process(Collection<EntityReference> entityReferences)
    {
        this.progressManager.pushLevelProgress(2, this);

        try {
            this.progressManager.startStep(this);
            if (this.request.isInteractive() && checkBrokenExtensions(entityReferences) && !confirmAction()) {
                return;
            }

            this.progressManager.startStep(this);
            super.process(entityReferences);

        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }


    protected boolean checkBrokenExtensions(Collection<EntityReference> entityReferences)
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

        return !this.brokenExtensions.isEmpty();
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
        // do something
        XarInstalledExtensionRepository repository = (XarInstalledExtensionRepository) installedExtensionRepository;

        for (XarInstalledExtension extension : repository.getXarInstalledExtensions(documentReference)) {
            List pages = this.brokenExtensions.get(extension);
            if (pages == null) {
                pages = new ArrayList();
                this.brokenExtensions.put(extension, pages);
            }
            pages.add(documentReference);
        }
    }

    private boolean confirmAction()
    {
        ExtensionBreakingQuestion question = new ExtensionBreakingQuestion(this.brokenExtensions);
        try {
            this.status.ask(question);
            return question.isConfirm();
        } catch (InterruptedException e) {
            this.logger.warn("Confirm question has been interrupted.");
            return false;
        }
    }
}
