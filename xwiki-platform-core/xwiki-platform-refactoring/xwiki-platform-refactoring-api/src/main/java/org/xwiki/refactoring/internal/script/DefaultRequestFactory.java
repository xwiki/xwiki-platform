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
package org.xwiki.refactoring.internal.script;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.job.DeleteRequest;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.PermanentlyDeleteRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.ReplaceUserRequest;
import org.xwiki.refactoring.job.RestoreRequest;
import org.xwiki.refactoring.script.RequestFactory;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Default implementation of the {@link org.xwiki.refactoring.script.RequestFactory}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@Component
@Singleton
public class DefaultRequestFactory implements RequestFactory
{
    @Inject
    private Logger logger;

    /**
     * Needed for getting the current user reference.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * @param type the type of refactoring
     * @return an id for a job to perform the specified type of refactoring
     */
    private List<String> generateJobId(String type)
    {
        String suffix = new Date().getTime() + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
        return getJobId(type, suffix);
    }

    /**
     * @param type the type of refactoring
     * @param suffix uniquely identifies the job among those of the specified type
     * @return an id for a job to perform the specified type of refactoring
     */
    private List<String> getJobId(String type, String suffix)
    {
        return Arrays
            .asList(RefactoringJobs.GROUP, StringUtils.removeStart(type, RefactoringJobs.GROUP_PREFIX), suffix);
    }

    private void setRightsProperties(AbstractCheckRightsRequest request)
    {
        request.setCheckRights(true);
        request.setUserReference(this.documentAccessBridge.getCurrentUserReference());
        request.setAuthorReference(this.documentAccessBridge.getCurrentAuthorReference());
    }

    @Override
    public MoveRequest createMoveRequest(Collection<EntityReference> sources, EntityReference destination)
    {
        return createMoveRequest(RefactoringJobs.MOVE, sources, destination);
    }

    @Override
    public MoveRequest createMoveRequest(EntityReference source, EntityReference destination)
    {
        return createMoveRequest(Arrays.asList(source), destination);
    }

    @Override
    public MoveRequest createRenameRequest(EntityReference oldReference, EntityReference newReference)
    {
        return createMoveRequest(RefactoringJobs.RENAME, Collections.singletonList(oldReference), newReference);
    }

    @Override
    public MoveRequest createRenameRequest(EntityReference reference, String newName)
    {
        return createRenameRequest(reference, new EntityReference(newName, reference.getType(), reference.getParent()));
    }

    @Override
    public CopyRequest createCopyRequest(Collection<EntityReference> sources, EntityReference destination)
    {
        CopyRequest request = new CopyRequest();
        this.initEntityRequest(request, RefactoringJobs.COPY, sources);

        EntityReference dest;

        // in case of a simple copy request, we only want it to be placed under the given destination document.
        if (destination.getType().equals(EntityType.DOCUMENT)) {
            DocumentReference documentReference = new DocumentReference(destination);
            dest = documentReference.getLastSpaceReference();
        } else {
            dest = destination;
        }
        request.setDestination(dest);
        return request;
    }

    @Override
    public CopyRequest createCopyRequest(EntityReference source, EntityReference destination)
    {
        return createCopyRequest(Arrays.asList(source), destination);
    }

    @Override
    public CopyRequest createCopyAsRequest(EntityReference sourceReference, EntityReference copyReference)
    {
        CopyRequest request = new CopyRequest();
        this.initEntityRequest(request, RefactoringJobs.COPY_AS, Arrays.asList(sourceReference));
        request.setDestination(copyReference);
        return request;
    }

    @Override
    public CopyRequest createCopyAsRequest(EntityReference reference, String copyName)
    {
        EntityReference copyReference = new EntityReference(copyName, reference.getType(), reference.getParent());
        return createCopyAsRequest(reference, copyReference);
    }

    @Override
    public EntityRequest createDeleteRequest(Collection<EntityReference> entityReferences)
    {
        DeleteRequest request = new DeleteRequest();
        initEntityRequest(request, RefactoringJobs.DELETE, entityReferences);
        return request;
    }

    @Override
    public CreateRequest createCreateRequest(Collection<EntityReference> entityReferences)
    {
        CreateRequest request = new CreateRequest();
        initEntityRequest(request, RefactoringJobs.CREATE, entityReferences);
        // Set deep create by default, to copy (if possible) any existing hierarchy of a specified template document.
        // TODO: expose this in the create UI to advanced users to allow them to opt-out?
        request.setDeep(true);
        return request;
    }

    private void initEntityRequest(EntityRequest request, String type, Collection<EntityReference> entityReferences)
    {
        request.setId(generateJobId(type));
        request.setJobType(type);
        request.setEntityReferences(entityReferences);
        setRightsProperties(request);
    }

    private MoveRequest createMoveRequest(String type, Collection<EntityReference> sources, EntityReference destination)
    {
        MoveRequest request = new MoveRequest();
        initEntityRequest(request, type, sources);
        request.setDestination(destination);
        request.setUpdateLinks(true);
        request.setAutoRedirect(true);
        request.setUpdateParentField(true);
        return request;
    }

    @Override
    public PermanentlyDeleteRequest createPermanentlyDeleteRequest(String batchId)
    {
        PermanentlyDeleteRequest request = initializePermanentlyDeleteRequest();
        request.setBatchId(batchId);
        return request;
    }

    @Override
    public PermanentlyDeleteRequest createPermanentlyDeleteRequest(List<Long> deletedDocumentIds)
    {
        PermanentlyDeleteRequest request = initializePermanentlyDeleteRequest();
        request.setDeletedDocumentIds(deletedDocumentIds);
        return request;
    }

    private PermanentlyDeleteRequest initializePermanentlyDeleteRequest()
    {
        PermanentlyDeleteRequest request = new PermanentlyDeleteRequest();

        request.setId(generateJobId(RefactoringJobs.PERMANENTLY_DELETE));
        request.setCheckRights(true);
        request.setUserReference(this.documentAccessBridge.getCurrentUserReference());
        request.setWikiReference(getCurrentWikiReference());

        return request;
    }

    @Override
    public RestoreRequest createRestoreRequest(String batchId)
    {
        RestoreRequest request = initializeRestoreRequest();
        request.setBatchId(batchId);
        return request;
    }

    @Override
    public RestoreRequest createRestoreRequest(List<Long> deletedDocumentIds)
    {
        RestoreRequest request = initializeRestoreRequest();
        request.setDeletedDocumentIds(deletedDocumentIds);
        return request;
    }

    private RestoreRequest initializeRestoreRequest()
    {
        RestoreRequest request = new RestoreRequest();

        request.setId(generateJobId(RefactoringJobs.RESTORE));
        request.setCheckRights(true);
        request.setUserReference(this.documentAccessBridge.getCurrentUserReference());
        request.setWikiReference(getCurrentWikiReference());

        return request;
    }

    private WikiReference getCurrentWikiReference()
    {
        String currentWikiId = this.wikiDescriptorManager.getCurrentWikiId();
        return currentWikiId != null ? new WikiReference(currentWikiId) : null;
    }

    @Override
    public ReplaceUserRequest createReplaceUserRequest(DocumentReference oldUserReference,
        DocumentReference newUserReference)
    {
        ReplaceUserRequest request = new ReplaceUserRequest();
        request.setId(generateJobId(RefactoringJobs.REPLACE_USER));
        request.setOldUserReference(oldUserReference);
        request.setNewUserReference(newUserReference);
        setRightsProperties(request);

        Collection<String> targetWikis = null;
        if (oldUserReference != null) {
            targetWikis = getReplaceUserTargetWikis(oldUserReference);
        } else if (newUserReference != null) {
            targetWikis = getReplaceUserTargetWikis(newUserReference);
        }
        if (targetWikis != null) {
            request.setEntityReferences(targetWikis.stream().map(WikiReference::new).collect(Collectors.toSet()));
        }

        return request;
    }

    private Collection<String> getReplaceUserTargetWikis(DocumentReference userReference)
    {
        if (userReference.getWikiReference().getName().equals(this.wikiDescriptorManager.getMainWikiId())) {
            // Global user, replace globally.
            try {
                return this.wikiDescriptorManager.getAllIds();
            } catch (WikiManagerException e) {
                this.logger.warn("Failed to get the list of wikis. Root cause is [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
                return Collections.emptySet();
            }
        } else {
            // Local user, replace locally.
            return Collections.singleton(userReference.getWikiReference().getName());
        }
    }
}
