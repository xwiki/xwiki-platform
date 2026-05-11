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
import org.xwiki.guidedtour.api.dtos.TourDTO;
import org.xwiki.guidedtour.api.enums.TourProperty;
import org.xwiki.guidedtour.api.exceptions.DuplicatedIdException;
import org.xwiki.guidedtour.api.exceptions.InvalidIdException;
import org.xwiki.guidedtour.internal.util.SolrQueryUtil;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.Request;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryException;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.script.RequestFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.guidedtour.internal.util.GuidedTourConstants.TOUR_CLASS;

/**
 * Manages the instance tours. It provides methods to create, retrieve, update and delete tours. Tours are stored as
 * XWiki documents with a TourClass object.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component(roles = ToursManager.class)
@Singleton
public class ToursManager
{
    private static final String CLASS_PREFIX = "property.XWiki.GuidedTour.TourClass.%s";

    private static final String QS = String.format("class:%s", TOUR_CLASS);

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Inject
    private TasksManager tasksManager;

    @Inject
    private SolrQueryUtil queryUtil;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private RequestFactory requestFactory;

    /**
     * Creates a new tour based on the provided DTO. The tour is stored as an XWiki document with a TourClass object.
     *
     * @param tourDTO the DTO containing the tour information
     * @throws XWikiException if there is an error while interacting with the XWiki API
     * @throws DuplicatedIdException if a tour with the same ID already exists
     */
    public void createTour(TourDTO tourDTO) throws XWikiException, DuplicatedIdException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        DocumentReference targetDocRef = this.documentReferenceResolver.resolve(tourDTO.getId());
        XWikiDocument targetDoc = wiki.getDocument(targetDocRef, wikiContext);
        BaseObject tourClassObject = targetDoc.getXObject(TOUR_CLASS);
        if (tourClassObject == null) {
            tourClassObject = targetDoc.newXObject(TOUR_CLASS, wikiContext);
            tourClassObject.set(TourProperty.TITLE.getBaseKey(), tourDTO.getTitle(), wikiContext);
            tourClassObject.set(TourProperty.IS_ACTIVE.getBaseKey(), tourDTO.isActive() ? 1 : 0, wikiContext);
            targetDoc.addXObject(tourClassObject);
            wiki.saveDocument(targetDoc, "Tour created.", wikiContext);
        } else {
            throw new DuplicatedIdException("A tour with the same ID [%s] already exists.", tourDTO.getId());
        }
    }

    /**
     * Retrieves all tours. It executes a Solr query to get all documents with a TourClass object and maps the results
     * to a list of TourDTOs.
     *
     * @return a JSON string representing the list of tours
     * @throws QueryException if there is an error while executing the Solr query
     * @throws XWikiException if there is an error while interacting with the XWiki API
     */
    public List<TourDTO> getAllTours() throws QueryException, XWikiException, InvalidIdException
    {
        List<String> filteredLines = new ArrayList<>();
        filteredLines.add(TourProperty.TITLE.formKey(CLASS_PREFIX));
        filteredLines.add(TourProperty.IS_ACTIVE.formKey(CLASS_PREFIX));
        SolrDocumentList solrDocuments = this.queryUtil.executeQuery(QS, "type:DOCUMENT", filteredLines);
        List<TourDTO> tours = new ArrayList<>(solrDocuments.size());
        for (SolrDocument document : solrDocuments) {
            EntityReference documentReference =
                this.solrDocumentReferenceResolver.resolve(document, EntityType.DOCUMENT);
            String title = (String) document.getFirstValue(TourProperty.TITLE.formKey(CLASS_PREFIX));
            boolean isActive = (Boolean) document.getFirstValue(TourProperty.IS_ACTIVE.formKey(CLASS_PREFIX));
            TourDTO dto = new TourDTO(documentReference.toString(), title, isActive);
            dto.setTasks(this.tasksManager.getAllTasks(documentReference.toString()));
            tours.add(dto);
        }
        return tours;
    }

    /**
     * Updates an existing tour based on the provided DTO. It retrieves the corresponding XWiki document and updates the
     * TourClass object with the new information from the DTO.
     *
     * @param tourDTO the DTO containing the updated tour information
     * @throws XWikiException if there is an error while interacting with the XWiki API
     * @throws InvalidIdException if a tour with the given ID does not exist
     */
    public void updateTour(TourDTO tourDTO) throws XWikiException, InvalidIdException
    {
        BaseObject tourClassObject = getTourClassObject(tourDTO.getId());
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        tourClassObject.set("title", tourDTO.getTitle(), wikiContext);
        tourClassObject.set("isActive", tourDTO.isActive() ? 1 : 0, wikiContext);
        wiki.saveDocument(tourClassObject.getOwnerDocument(), "Updated tour object.", wikiContext);
    }

    /**
     * Deletes an existing tour based on the provided ID. It retrieves the corresponding XWiki document and deletes the
     * entire tour space using a Refactoring Job to ensure that all the related documents (tasks and steps) are part of
     * the same batch.
     *
     * @param tourId the ID of the tour to be deleted
     * @throws XWikiException if there is an error while interacting with the XWiki API
     * @throws JobException if there is an error while executing the Refactoring Job
     * @throws InvalidIdException if a tour with the given ID does not exist
     */
    public void deleteTour(String tourId) throws XWikiException, JobException, InvalidIdException
    {
        getTourClassObject(tourId);
        DocumentReference targetDocRef = this.documentReferenceResolver.resolve(tourId);
        Request deleteReq = this.requestFactory.createDeleteRequest(List.of(targetDocRef.getLastSpaceReference()));
        this.jobExecutor.execute(RefactoringJobs.DELETE, deleteReq);
    }

    private BaseObject getTourClassObject(String tourId) throws InvalidIdException, XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        DocumentReference targetDocRef = this.documentReferenceResolver.resolve(tourId);
        XWikiDocument targetDoc = wiki.getDocument(targetDocRef, wikiContext);
        BaseObject tourClassObject = targetDoc.getXObject(TOUR_CLASS);
        if (tourClassObject == null) {
            throw new InvalidIdException("Tour with the given id [%s] does not exist.", tourId);
        }
        return tourClassObject;
    }
}
