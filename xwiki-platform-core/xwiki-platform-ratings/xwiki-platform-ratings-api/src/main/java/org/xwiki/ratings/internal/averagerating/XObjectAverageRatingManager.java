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
package org.xwiki.ratings.internal.averagerating;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.converter.Converter;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.events.UpdateAverageRatingFailedEvent;
import org.xwiki.ratings.events.UpdatedAverageRatingEvent;
import org.xwiki.ratings.events.UpdatingAverageRatingEvent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Implementation of {@link AverageRatingManager} that stores the average rating in an xobject.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Named("xobject")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XObjectAverageRatingManager extends AbstractAverageRatingManager
{
    private static final String MOVE_ERROR_MESSAGE = "Error while moving the average ratings from [%s] to [%s]";

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /*
     * We rely on a converter here to follow the same implementation than SolrAverageRatingManager, where the usage
     * of the converter is actually hidden under SolrUtils.
     */
    @Inject
    private Converter<EntityReference> entityReferenceConverter;

    @Inject
    private EntityReferenceSerializer<String> stringEntityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Override
    public AverageRating getAverageRating(EntityReference entityReference) throws RatingsException
    {
        try {
            BaseObject baseObject = this.retrieveAverageRatingXObject(entityReference);

            if (baseObject == null) {
                return this.createAverageRating(entityReference, this.computeAverageRatingId(entityReference));
            } else {
                return this.transformXObjectToAverageRating(baseObject, entityReference);
            }
        } catch (Exception e) {
            throw new RatingsException(
                String.format("Error while trying to get average rating from reference [%s]", entityReference), e);
        }
    }

    @Override
    public long removeAverageRatings(EntityReference entityReference) throws RatingsException
    {
        long result = 0;
        // if the reference is a wiki, then everything's already deleted we don't need to do anything here
        // and we avoid trying to load document to avoid errors.
        if (entityReference.getType() != EntityType.WIKI) {
            try {
                BaseObject baseObject = retrieveAverageRatingXObject(entityReference);
                if (baseObject != null) {
                    XWikiDocument ownerDocument = baseObject.getOwnerDocument();
                    ownerDocument.removeXObject(baseObject);
                    XWikiContext context = this.contextProvider.get();
                    String comment = this.contextualLocalizationManager
                        .getTranslationPlain("ratings.averagerating.manager.remove.comment");
                    context.getWiki().saveDocument(ownerDocument, comment, true, context);
                    result = 1;
                }
            } catch (Exception e) {
                throw new RatingsException(String.format(
                    "Error while trying to remove average ratings related to [%s]", entityReference), e);
            }
        }
        return result;
    }

    @Override
    public long moveAverageRatings(EntityReference oldReference, EntityReference newReference)
        throws RatingsException
    {
        // Checks that we are in the presence of entity types that can be handled by the move method.
        if (oldReference == null
            || newReference == null
            || oldReference.extractReference(EntityType.DOCUMENT) == null
            || newReference.extractReference(EntityType.DOCUMENT) == null)
        {
            throw new RatingsException(
                "Impossible to move the average ratings from [" + oldReference + "] to [" + newReference + "].");
        }

        // The list of the average ratings updated during the move.
        List<AverageRating> changedAverageRatings = new ArrayList<>();

        try {
            XWikiDocument actualDoc = (XWikiDocument) this.documentAccessBridge.getDocumentInstance(newReference);

            // We must inspect all the XObject to see of they need to be updated, some of them can be attached to
            // sub-elements of the page.
            for (BaseObject ratingsObject : actualDoc
                .getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE)) {
                if (ratingsObject != null) {
                    moveAverageRatingsObject(oldReference, newReference, ratingsObject)
                        .ifPresent(changedAverageRatings::add);
                }
            }

            // Save the document if something has changed
            if (!changedAverageRatings.isEmpty()) {
                XWikiContext context = this.contextProvider.get();
                this.getObservationManager()
                    .notify(new UpdatingAverageRatingEvent(), this.getIdentifier(), changedAverageRatings);
                try {
                    String comment =
                        contextualLocalizationManager.getTranslationPlain("ratings.averagerating.manager.move.comment");
                    context.getWiki().saveDocument(actualDoc, comment, true, context);
                    this.getObservationManager()
                        .notify(new UpdatedAverageRatingEvent(), this.getIdentifier(), changedAverageRatings);
                } catch (XWikiException e) {
                    this.getObservationManager()
                        .notify(new UpdateAverageRatingFailedEvent(), this.getIdentifier(), changedAverageRatings);
                    throw new RatingsException(String.format(MOVE_ERROR_MESSAGE, oldReference, newReference), e);
                }
            }
        } catch (Exception e) {
            throw new RatingsException(String.format(MOVE_ERROR_MESSAGE, oldReference, newReference), e);
        }

        return changedAverageRatings.size();
    }

    /**
     * Handle the move of an average ratings XObject from {@code oldEntityReference} to {@code newEntityReference}.
     *
     * @param oldReference the entity reference of the document before the move
     * @param newReference the entity reference of the document after the move
     * @param averageRatingsObject an average ratings XObject of the document
     * @return {@link Optional#empty()} if the object does not need to be updated, {@link Optional#of(Object)} filled
     *     with the updated {@link AverageRating} otherwise
     */
    private Optional<AverageRating> moveAverageRatingsObject(EntityReference oldReference, EntityReference newReference,
        BaseObject averageRatingsObject)
    {
        Optional<AverageRating> averageRating;
        String xobjectManagerId =
            averageRatingsObject.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName());

        // Checks that the object is attached to the current manager
        if (Objects.equals(getIdentifier(), xobjectManagerId)) {
            String xObjectOldReference =
                averageRatingsObject.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName());
            EntityReference xObjectOldEntityReference =
                this.entityReferenceConverter.convert(EntityReference.class, xObjectOldReference);

            // Updates the entity reference to point to the matching entity after the move.
            EntityReference xObjectNewEntityReference;
            if (xObjectOldEntityReference.equals(oldReference)) {
                xObjectNewEntityReference = newReference;
            } else if (xObjectOldEntityReference.hasParent(oldReference)) {
                xObjectNewEntityReference = xObjectOldEntityReference.replaceParent(newReference);
            } else {
                xObjectNewEntityReference = null;
            }

            if (xObjectNewEntityReference != null) {
                String xObjectNewReference =
                    this.entityReferenceConverter.convert(String.class, xObjectNewEntityReference);

                averageRatingsObject
                    .setStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), xObjectNewReference);

                averageRating =
                    Optional.of(transformXObjectToAverageRating(averageRatingsObject, xObjectNewEntityReference));
            } else {
                averageRating = Optional.empty();
            }
        } else {
            averageRating = Optional.empty();
        }
        return averageRating;
    }

    private BaseObject retrieveAverageRatingXObject(EntityReference entityReference) throws Exception
    {
        DocumentModelBridge documentInstance = this.documentAccessBridge.getDocumentInstance(entityReference);
        XWikiDocument actualDoc = (XWikiDocument) documentInstance;
        String serializedReference = this.entityReferenceConverter.convert(String.class, entityReference);
        for (BaseObject xObject : actualDoc
            .getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE)) {
            if (xObject != null) {
                String xobjectReference =
                    xObject.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName());
                String xobjectManagerId = xObject.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName());

                if (StringUtils.isEmpty(xobjectReference) && StringUtils.isEmpty(xobjectManagerId)) {
                    return xObject;
                } else if (getIdentifier().equals(xobjectManagerId) && serializedReference.equals(xobjectReference)) {
                    return xObject;
                }
            }
        }
        return null;
    }

    private String computeAverageRatingId(EntityReference entityReference)
    {
        String serializedOwnerDocReference =
            this.stringEntityReferenceSerializer.serialize(entityReference.extractReference(EntityType.DOCUMENT));
        String managerId = this.getIdentifier();
        String serializedEntityReference = this.stringEntityReferenceSerializer.serialize(entityReference);
        String entityType = entityReference.getType().getLowerCase();

        return String.format("%s_%s", serializedOwnerDocReference,
            new HashCodeBuilder().append(managerId).append(serializedEntityReference).append(entityType).toHashCode());
    }

    private AverageRating transformXObjectToAverageRating(BaseObject baseObject, EntityReference entityReference)
    {
        String averageId = this.computeAverageRatingId(entityReference);
        String managerId = this.getIdentifier();
        int totalVote = baseObject.getIntValue(AverageRatingQueryField.TOTAL_VOTE.getFieldName());
        float averageVote = baseObject.getFloatValue(AverageRatingQueryField.AVERAGE_VOTE.getFieldName());
        Date updatedDate = baseObject.getDateValue(AverageRatingQueryField.UPDATED_AT.getFieldName());
        int scale = baseObject.getIntValue(AverageRatingQueryField.SCALE.getFieldName(), 5);

        return new DefaultAverageRating(averageId)
            .setManagerId(managerId)
            .setReference(entityReference)
            .setAverageVote(averageVote)
            .setTotalVote(totalVote)
            .setUpdatedAt(updatedDate)
            .setScaleUpperBound(scale);
    }

    @Override
    public void saveAverageRating(AverageRating averageRating) throws RatingsException
    {
        try {
            EntityReference entityReference = averageRating.getReference();
            BaseObject baseObject = this.retrieveAverageRatingXObject(entityReference);
            XWikiContext context = this.contextProvider.get();

            // if the base object is null, we create it.
            if (baseObject == null) {
                DocumentModelBridge documentInstance = this.documentAccessBridge.getDocumentInstance(entityReference);
                XWikiDocument actualDoc = (XWikiDocument) documentInstance;
                int xObjectNumber =
                    actualDoc.createXObject(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE,
                        context);
                baseObject = actualDoc.getXObject(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE,
                    xObjectNumber);
            }
            String serializedEntityReference = this.entityReferenceConverter.convert(String.class, entityReference);
            baseObject.setStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName(), this.getIdentifier());
            baseObject.setStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(),
                serializedEntityReference);
            baseObject.setIntValue(AverageRatingQueryField.TOTAL_VOTE.getFieldName(), averageRating.getNbVotes());
            baseObject.setFloatValue(AverageRatingQueryField.AVERAGE_VOTE.getFieldName(),
                averageRating.getAverageVote());
            baseObject.setIntValue(AverageRatingQueryField.SCALE.getFieldName(), this.getScale());
            baseObject.setDateValue(AverageRatingQueryField.UPDATED_AT.getFieldName(), averageRating.getUpdatedAt());

            String comment =
                this.contextualLocalizationManager.getTranslationPlain("ratings.averagerating.manager.update.comment");
            context.getWiki().saveDocument(baseObject.getOwnerDocument(), comment, true, context);
        } catch (Exception e) {
            throw new RatingsException(String.format("Error while saving Average Rating [%s].", averageRating), e);
        }
    }
}
