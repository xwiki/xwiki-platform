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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.guidedtour.api.dtos.StepDTO;
import org.xwiki.guidedtour.api.enums.TourProperty;
import org.xwiki.guidedtour.api.exceptions.DuplicatedIdException;
import org.xwiki.guidedtour.api.exceptions.InvalidIdException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.guidedtour.internal.util.GuidedTourConstants.STEP_CLASS;

/**
 * Manages the steps of a task. It provides methods to create, retrieve, update and delete steps. Steps are stored as
 * objects in the task document.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component(roles = StepsManager.class)
@Singleton
public class StepsManager
{
    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Creates a new step in the given task. The step is added at the end of the list of steps.
     *
     * @param tourId the id of the tour to which the task belongs
     * @param taskId the id of the task to which the step will be added
     * @param stepDTO the {@link StepDTO} containing the step data
     * @throws XWikiException if there is an error while retrieving or saving the task document
     * @throws DuplicatedIdException if a step with the same order already exists in the task
     */
    public void createStep(String tourId, String taskId, StepDTO stepDTO)
        throws XWikiException, DuplicatedIdException, InvalidIdException
    {
        int highestOrder = getHighestOrder(tourId, taskId, stepDTO.getOrder());
        DocumentReference taskDocRef =
            this.documentReferenceResolver.resolve(taskId, this.documentReferenceResolver.resolve(tourId));
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        XWikiDocument taskDoc = wiki.getDocument(taskDocRef, wikiContext);
        stepDTO.setOrder(++highestOrder);
        BaseObject stepClassObject = taskDoc.newXObject(STEP_CLASS, wikiContext);
        populateStepObject(stepDTO, stepClassObject);
        wiki.saveDocument(taskDoc, "Added new step.", wikiContext);
    }

    /**
     * Retrieves all the steps for the given task.
     *
     * @param tourId the id of the tour to which the task belongs
     * @param taskId the id of the task for which the steps will be retrieved
     * @return a list of {@link StepDTO} containing the step data
     * @throws XWikiException if there is an error while retrieving the task document
     * @throws InvalidIdException if the tour or the task with the given id does not exist
     */
    public List<StepDTO> getAllSteps(String tourId, String taskId) throws XWikiException, InvalidIdException
    {
        List<BaseObject> stepObjects = getStepObjects(tourId, taskId);
        List<StepDTO> steps = new ArrayList<>(stepObjects.size());
        for (BaseObject stepObject : stepObjects) {
            steps.add(getStepDTO(stepObject));
        }
        return steps;
    }

    /**
     * Updates the step with the given id. If the order of the step is updated, the order of the other steps is also
     * updated accordingly.
     *
     * @param tourId the id of the tour to which the task belongs
     * @param taskId the id of the task to which the step belongs
     * @param stepId the id of the step to be updated represented by the order of the step in the list of steps
     * @param newDTO the {@link StepDTO} containing the updated step data
     * @throws XWikiException if there is an error while retrieving or saving the task document
     * @throws InvalidIdException if the tour, the task or the step with the given id does not exist
     */
    public void updateStep(String tourId, String taskId, int stepId, StepDTO newDTO)
        throws XWikiException, InvalidIdException
    {
        List<BaseObject> existingSteps = getStepObjects(tourId, taskId);
        BaseObject stepObject = getStepObjectFromList(stepId, existingSteps);
        if (stepId != newDTO.getOrder()) {
            updateStepsOrder(stepId, newDTO.getOrder(), existingSteps);
        }
        populateStepObject(newDTO, stepObject);
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        wiki.saveDocument(stepObject.getOwnerDocument(), "Updated step.", wikiContext);
    }

    /**
     * Deletes the step with the given id. The order of the other steps is updated accordingly.
     *
     * @param tourId the id of the tour to which the task belongs
     * @param taskId the id of the task to which the step belongs
     * @param stepId the id of the step to be deleted represented by the order of the step in the list of steps
     * @throws XWikiException if there is an error while retrieving or saving the task document
     * @throws InvalidIdException if the tour, the task or the step with the given id does not exist
     */
    public void deleteStep(String tourId, String taskId, int stepId) throws XWikiException, InvalidIdException
    {
        List<BaseObject> existingSteps = getStepObjects(tourId, taskId);
        BaseObject stepObject = getStepObjectFromList(stepId, existingSteps);
        updateStepsOrder(stepId, Integer.MAX_VALUE, existingSteps);
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        XWikiDocument taskDoc = stepObject.getOwnerDocument();
        taskDoc.removeXObject(stepObject);
        wiki.saveDocument(taskDoc, String.format("Removed step %s.", stepId), wikiContext);
    }

    private void updateStepsOrder(int originalOrder, int newOrder, List<BaseObject> existingSteps) throws XWikiException
    {
        for (BaseObject task : existingSteps) {
            int order = task.getIntValue(TourProperty.ORDER.getBaseKey());
            if (order > originalOrder && order <= newOrder) {
                StepDTO taskDTO = getStepDTO(task);
                taskDTO.setOrder(order - 1);
                populateStepObject(taskDTO, task);
            } else if (order < originalOrder && order >= newOrder) {
                StepDTO taskDTO = getStepDTO(task);
                taskDTO.setOrder(order + 1);
                populateStepObject(taskDTO, task);
            }
        }
    }

    private static StepDTO getStepDTO(BaseObject stepObject)
    {
        StepDTO stepDTO = new StepDTO();
        stepDTO.setOrder(stepObject.getIntValue(TourProperty.ORDER.getBaseKey()));
        stepDTO.setElement(stepObject.getStringValue(TourProperty.ELEMENT.getBaseKey()));
        stepDTO.setContent(stepObject.getStringValue(TourProperty.CONTENT.getBaseKey()));
        stepDTO.setPlacement(stepObject.getStringValue(TourProperty.PLACEMENT.getBaseKey()));
        stepDTO.setBackdrop(stepObject.getIntValue(TourProperty.BACKDROP.getBaseKey()) == 1);
        stepDTO.setReflex(stepObject.getIntValue(TourProperty.REFLEX.getBaseKey()) == 1);
        stepDTO.setTargetPage(stepObject.getStringValue(TourProperty.TARGET_PAGE.getBaseKey()));
        stepDTO.setTargetAction(stepObject.getStringValue(TourProperty.TARGET_ACTION.getBaseKey()));
        stepDTO.setQueryParameters(stepObject.getStringValue(TourProperty.QUERY_PARAMETERS.getBaseKey()));
        return stepDTO;
    }

    private BaseObject getStepObjectFromList(int stepId, List<BaseObject> existingSteps) throws InvalidIdException
    {
        return existingSteps.stream().filter(step -> step.getIntValue(TourProperty.ORDER.getBaseKey()) == stepId)
            .findFirst()
            .orElseThrow(() -> new InvalidIdException("No step was found on the given order position [%d].", stepId));
    }

    private List<BaseObject> getStepObjects(String tourId, String taskId) throws XWikiException, InvalidIdException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        DocumentReference tourDocRef = documentReferenceResolver.resolve(tourId);
        if (wiki.exists(tourDocRef, wikiContext)) {
            DocumentReference taskDocRef = documentReferenceResolver.resolve(taskId, tourDocRef);
            if (wiki.exists(taskDocRef, wikiContext)) {
                XWikiDocument taskDoc = wiki.getDocument(taskDocRef, wikiContext);
                return taskDoc.getXObjects(STEP_CLASS).stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(step -> step.getIntValue(TourProperty.ORDER.getBaseKey())))
                    .collect(Collectors.toList());
            } else {
                throw new InvalidIdException("Task with the given id [%s] does not exists.", taskId);
            }
        } else {
            throw new InvalidIdException("Tour with the given id [%s] does not exists.", tourId);
        }
    }

    private int getHighestOrder(String tourId, String taskId, int stepId)
        throws XWikiException, DuplicatedIdException, InvalidIdException
    {
        List<BaseObject> existingSteps = getStepObjects(tourId, taskId);
        int highestOrder = 0;
        if (!existingSteps.isEmpty()) {
            if (existingSteps.stream()
                .anyMatch(step -> step.getIntValue(TourProperty.ORDER.getBaseKey()) == stepId))
            {
                throw new DuplicatedIdException("A step with the given order [%d] already exists.", stepId);
            }
            highestOrder =
                existingSteps.stream().mapToInt(step -> step.getIntValue(TourProperty.ORDER.getBaseKey())).max()
                    .orElse(0);
        }
        return highestOrder;
    }

    private void populateStepObject(StepDTO stepDTO, BaseObject taskClassObject) throws XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        taskClassObject.set(TourProperty.ORDER.getBaseKey(), stepDTO.getOrder(), wikiContext);
        taskClassObject.set(TourProperty.ELEMENT.getBaseKey(), stepDTO.getElement(), wikiContext);
        taskClassObject.set(TourProperty.CONTENT.getBaseKey(), stepDTO.getContent(), wikiContext);
        taskClassObject.set(TourProperty.PLACEMENT.getBaseKey(), stepDTO.getPlacement(), wikiContext);
        taskClassObject.set(TourProperty.BACKDROP.getBaseKey(), stepDTO.isBackdrop() ? 1 : 0, wikiContext);
        taskClassObject.set(TourProperty.REFLEX.getBaseKey(), stepDTO.isReflex() ? 1 : 0, wikiContext);
        taskClassObject.set(TourProperty.TARGET_PAGE.getBaseKey(), stepDTO.getTargetPage(), wikiContext);
        taskClassObject.set(TourProperty.TARGET_ACTION.getBaseKey(), stepDTO.getTargetAction(), wikiContext);
        taskClassObject.set(TourProperty.QUERY_PARAMETERS.getBaseKey(), stepDTO.getQueryParameters(), wikiContext);
    }
}
