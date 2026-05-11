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
package org.xwiki.guidedtour.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.xwiki.component.annotation.Component;
import org.xwiki.guidedtour.api.dtos.TaskDTO;
import org.xwiki.guidedtour.api.enums.TourProperty;
import org.xwiki.guidedtour.api.exceptions.DuplicatedIdException;
import org.xwiki.guidedtour.api.exceptions.InvalidIdException;
import org.xwiki.guidedtour.internal.util.SolrQueryUtil;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.query.QueryException;

import com.google.common.base.Splitter;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.guidedtour.internal.util.GuidedTourConstants.TASK_CLASS;

/**
 * Manages the tasks for the guided tour. It provides methods to create, retrieve, update and delete tasks. Tasks are
 * stored as XWiki documents with a TaskClass object. The document name is the task id and the parent document is the
 * tour document.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component(roles = TasksManager.class)
@Singleton
public class TasksManager
{
    private static final String QS = String.format("class:%s AND ", TASK_CLASS);

    private static final String CLASS_PREFIX = "property.XWiki.GuidedTour.TaskClass.%s";

    private static final List<String> FILTERED_LINES =
        List.of(TourProperty.DEPENDS_ON.formKey(CLASS_PREFIX), TourProperty.TITLE.formKey(CLASS_PREFIX),
            TourProperty.ORDER.formKey(CLASS_PREFIX), TourProperty.IS_ACTIVE.formKey(CLASS_PREFIX));

    private static final String TASK_NOT_FOUND_ERROR = "Task with the given id [%s] does not exists.";

    @Inject
    @Named("ReplaceCharacterEntityNameValidation")
    private EntityNameValidation nameValidator;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private SolrQueryUtil queryUtil;

    /**
     * Creates a new task based on the provided DTO. The task is stored as an {@link XWikiDocument} with a TaskClass
     * object. The order of the task is set to the highest order position available in the tour.
     *
     * @param tourId the id of the tour to which the task belongs
     * @param taskDTO the {@link TaskDTO} containing the task information
     * @throws XWikiException if there is an error while creating the task document
     * @throws QueryException if there is an error while querying for existing tasks to determine the order of the
     *     new task
     * @throws DuplicatedIdException if a task with the same id already exists in the tour
     * @throws InvalidIdException if the tour with the given id does not exist
     */
    public void createTask(String tourId, TaskDTO taskDTO)
        throws XWikiException, QueryException, DuplicatedIdException, InvalidIdException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        DocumentReference tourDocRef = getTourReference(tourId);
        String taskId = this.nameValidator.transform(taskDTO.getId());
        DocumentReference taskDocRef = this.documentReferenceResolver.resolve(taskId, tourDocRef);
        if (wiki.exists(taskDocRef, wikiContext)) {
            throw new DuplicatedIdException("Task page [%s] already exists.", taskDocRef);
        }
        int highestOrder = getHighestOrder(tourId, taskDTO);
        XWikiDocument taskDoc = wiki.getDocument(taskDocRef, wikiContext);
        taskDoc.setTitle(taskDTO.getTitle());
        BaseObject taskClassObject = taskDoc.newXObject(TASK_CLASS, wikiContext);
        taskDTO.setOrder(++highestOrder);
        populateTaskObject(taskDTO, taskClassObject);
        wiki.saveDocument(taskDoc, "Task created.", wikiContext);
    }

    /**
     * Retrieves a task based on the provided tour id and task id. It returns a {@link TaskDTO} containing the task
     * information.
     *
     * @param tourId the id of the tour to which the task belongs
     * @param taskId the id of the task to retrieve
     * @return a {@link TaskDTO} containing the task information
     * @throws XWikiException if there is an error while interacting with the XWiki API
     * @throws QueryException if there is an error while executing the Solr query to retrieve the task document
     * @throws InvalidIdException if the task with the given id does not exist in the tour
     */
    public TaskDTO getTask(String tourId, String taskId) throws XWikiException, QueryException, InvalidIdException
    {
        DocumentReference tourDocRef = getTourReference(tourId);
        String parentSpace = this.localSerializer.serialize(tourDocRef.getLastSpaceReference());
        String fq = String.format("{!q.op=AND} type:DOCUMENT AND space:%s AND name:%s", parentSpace, taskId);
        SolrDocumentList results = this.queryUtil.executeQuery(QS, fq, FILTERED_LINES);
        if (results.isEmpty()) {
            throw new InvalidIdException(TASK_NOT_FOUND_ERROR, taskId);
        }
        SolrDocument document = results.get(0);
        EntityReference documentReference = this.solrDocumentReferenceResolver.resolve(document, EntityType.DOCUMENT);
        return getTaskDTO(document, documentReference);
    }

    /**
     * Retrieves all tasks for a given tour id.
     *
     * @param tourId the id of the tour to which the tasks belong
     * @return a list of {@link TaskDTO} containing the tasks information
     * @throws QueryException if there is an error while executing the Solr query to retrieve the task documents
     * @throws XWikiException if there is an error while interacting with the XWiki API
     * @throws InvalidIdException if the tour with the given id does not exist
     */
    public List<TaskDTO> getAllTasks(String tourId) throws QueryException, XWikiException, InvalidIdException
    {
        DocumentReference tourDocRef = getTourReference(tourId);
        String parentSpace = this.localSerializer.serialize(tourDocRef.getLastSpaceReference());
        String fq = String.format("{!q.op=AND} type:DOCUMENT AND space:%s", parentSpace);
        SolrDocumentList solrDocuments = this.queryUtil.executeQuery(QS, fq, FILTERED_LINES);
        List<TaskDTO> tasks = new ArrayList<>(solrDocuments.size());
        for (SolrDocument document : solrDocuments) {
            EntityReference documentReference =
                this.solrDocumentReferenceResolver.resolve(document, EntityType.DOCUMENT);
            tasks.add(getTaskDTO(document, documentReference));
        }
        return tasks;
    }

    /**
     * Updates an existing task based on the provided DTO. If the order of the task is modified, it also updates the
     * order of the other tasks in the tour accordingly.
     *
     * @param tourId the id of the tour to which the task belongs
     * @param newDTO the {@link TaskDTO} containing the updated task information
     * @throws XWikiException if there is an error while interacting with the XWiki API
     * @throws QueryException if there is an error while querying for existing tasks to determine the order updates
     * @throws InvalidIdException if the task with the given id does not exist in the tour
     */
    public void updateTask(String tourId, TaskDTO newDTO) throws XWikiException, QueryException, InvalidIdException
    {
        List<TaskDTO> existingTasks = getAllTasks(tourId);
        TaskDTO oldTask = getTaskDTOFromList(newDTO.getId(), existingTasks);
        int oldOrder = oldTask.getOrder();
        DocumentReference tourDocRef = this.documentReferenceResolver.resolve(tourId);
        if (oldOrder != newDTO.getOrder()) {
            existingTasks.removeIf(task -> task.getId().equals(newDTO.getId()));
            updateTasksOrder(tourDocRef, newDTO.getOrder(), existingTasks, oldOrder);
        }
        updateTaskObject(newDTO, tourDocRef);
    }

    /**
     * Deletes a task based on the provided tour id and task id. It also updates the order of the other tasks in the
     * tour accordingly.
     *
     * @param tourId the id of the tour to which the task belongs
     * @param taskId the id of the task to delete
     * @throws XWikiException if there is an error while interacting with the XWiki API
     * @throws QueryException if there is an error while querying for existing tasks to determine the order updates
     * @throws InvalidIdException if the task with the given id does not exist in the tour
     */
    public void deleteTask(String tourId, String taskId) throws XWikiException, QueryException, InvalidIdException
    {
        List<TaskDTO> existingTasks = getAllTasks(tourId);
        TaskDTO targetTask = getTaskDTOFromList(taskId, existingTasks);
        existingTasks.remove(targetTask);
        // The existence of the tour document is already checked in the getAllTasks method.
        DocumentReference tourDocRef = this.documentReferenceResolver.resolve(tourId);
        updateTasksOrder(tourDocRef, Integer.MAX_VALUE, existingTasks, targetTask.getOrder());
        DocumentReference taskDocRef =
            this.documentReferenceResolver.resolve(taskId, this.documentReferenceResolver.resolve(tourId));
        XWikiContext wikiContext = wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        wiki.deleteAllDocuments(wiki.getDocument(taskDocRef, wikiContext), wikiContext);
    }

    private TaskDTO getTaskDTOFromList(String taskId, List<TaskDTO> existingTasks) throws InvalidIdException
    {
        return existingTasks.stream().filter(task -> task.getId().equals(taskId)).findFirst()
            .orElseThrow(() -> new InvalidIdException(TASK_NOT_FOUND_ERROR, taskId));
    }

    private DocumentReference getTourReference(String referenceString) throws XWikiException, InvalidIdException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        DocumentReference tourDocRef = this.documentReferenceResolver.resolve(referenceString);
        if (wikiContext.getWiki().exists(tourDocRef, wikiContext)) {
            return tourDocRef;
        } else {
            throw new InvalidIdException("Tour with the given id [%s] does not exists.", referenceString);
        }
    }

    private void updateTasksOrder(DocumentReference tourRef, int modifiedOrder, List<TaskDTO> existingTasks,
        int oldOrder) throws XWikiException
    {
        for (TaskDTO task : existingTasks) {
            if (task.getOrder() > oldOrder && task.getOrder() <= modifiedOrder) {
                task.setOrder(task.getOrder() - 1);
                updateTaskObject(task, tourRef);
            } else if (task.getOrder() < oldOrder && task.getOrder() >= modifiedOrder) {
                task.setOrder(task.getOrder() + 1);
                updateTaskObject(task, tourRef);
            }
        }
    }

    private TaskDTO getTaskDTO(SolrDocument document, EntityReference documentReference)
    {
        String title = (String) document.getFirstValue(TourProperty.TITLE.formKey(CLASS_PREFIX));
        String dependsOn = (String) document.getFirstValue(TourProperty.DEPENDS_ON.formKey(CLASS_PREFIX));
        long order = (Long) document.getFirstValue(TourProperty.ORDER.formKey(CLASS_PREFIX));
        boolean isActive = (Boolean) document.getFirstValue(TourProperty.IS_ACTIVE.formKey(CLASS_PREFIX));

        return new TaskDTO(documentReference.getName(), title, (int) order, isActive,
            Splitter.on(',').omitEmptyStrings().splitToList(dependsOn));
    }

    private void updateTaskObject(TaskDTO newDTO, DocumentReference tourDocRef) throws XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        DocumentReference taskDocRef = this.documentReferenceResolver.resolve(newDTO.getId(), tourDocRef);
        XWikiDocument taskDoc = wiki.getDocument(taskDocRef, wikiContext);
        taskDoc.setTitle(newDTO.getTitle());
        BaseObject taskClassObject = taskDoc.getXObject(TASK_CLASS);
        populateTaskObject(newDTO, taskClassObject);
        wiki.saveDocument(taskDoc, "Updated task.", wikiContext);
    }

    private void populateTaskObject(TaskDTO taskDTO, BaseObject taskClassObject) throws XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        taskClassObject.set("title", taskDTO.getTitle(), wikiContext);
        taskClassObject.set("dependsOn", taskDTO.getDependsOn(), wikiContext);
        taskClassObject.set("order", taskDTO.getOrder(), wikiContext);
        taskClassObject.set("isActive", taskDTO.isActive() ? 1 : 0, wikiContext);
    }

    private int getHighestOrder(String tourId, TaskDTO taskDTO)
        throws QueryException, XWikiException, InvalidIdException, DuplicatedIdException
    {
        List<TaskDTO> existingTasks = getAllTasks(tourId);
        int highestOrder = 0;
        if (!existingTasks.isEmpty()) {
            if (existingTasks.stream().anyMatch(task -> task.getOrder() == taskDTO.getOrder())) {
                throw new DuplicatedIdException("A task with the given order already exists.");
            }
            highestOrder = existingTasks.stream().mapToInt(TaskDTO::getOrder).max().orElse(0);
        }
        return highestOrder;
    }
}
