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

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.solr.common.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.converter.Converter;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;

import com.xpn.xwiki.XWikiContext;
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
                    context.getWiki().saveDocument(ownerDocument, "Remove average rating", true, context);
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
        // We don't need to do anything here: if a document reference has been moved then the xobject move with it.
        return 0;
    }

    private BaseObject retrieveAverageRatingXObject(EntityReference entityReference) throws Exception
    {
        DocumentModelBridge documentInstance = this.documentAccessBridge.getDocumentInstance(entityReference);
        XWikiDocument actualDoc = (XWikiDocument) documentInstance;
        String serializedReference = this.entityReferenceConverter.convert(String.class, entityReference);
        for (BaseObject xObject : actualDoc
            .getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE)) {
            String xobjectReference =
                xObject.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName());
            String xobjectManagerId = xObject.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName());

            if (StringUtils.isEmpty(xobjectReference) && StringUtils.isEmpty(xobjectManagerId)) {
                return xObject;
            } else if (getIdentifier().equals(xobjectManagerId) && serializedReference.equals(xobjectReference)) {
                return xObject;
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

            context.getWiki().saveDocument(baseObject.getOwnerDocument(), "Update average rating", true, context);
        } catch (Exception e) {
            throw new RatingsException(String.format("Error while saving Average Rating [%s].", averageRating), e);
        }
    }
}
