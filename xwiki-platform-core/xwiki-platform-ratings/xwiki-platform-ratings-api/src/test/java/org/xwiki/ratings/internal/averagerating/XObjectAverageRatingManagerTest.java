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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.properties.converter.Converter;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.events.UpdatedAverageRatingEvent;
import org.xwiki.ratings.events.UpdatingAverageRatingEvent;
import org.xwiki.ratings.internal.averagerating.AverageRatingManager.AverageRatingQueryField;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.model.EntityType.DOCUMENT;

/**
 * Tests for {@link XObjectAverageRatingManager}.
 *
 * @version $Id$
 */
@ComponentTest
class XObjectAverageRatingManagerTest
{
    @InjectMockComponents
    private XObjectAverageRatingManager averageRatingManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private Converter<EntityReference> entityReferenceConverter;

    @MockComponent
    private EntityReferenceSerializer<String> stringEntityReferenceSerializer;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @Mock
    private RatingsManager ratingsManager;

    @BeforeEach
    void setup()
    {
        averageRatingManager.setRatingsManager(this.ratingsManager);
    }

    @Test
    void getAverageRating() throws Exception
    {
        String managerId = "myRatings";
        when(this.ratingsManager.getIdentifier()).thenReturn(managerId);
        DocumentReference reference = new DocumentReference("xwiki","XWiki", "Foo");
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(xWikiDocument.clone()).thenReturn(xWikiDocument);
        when(xWikiDocument.getDocumentReference()).thenReturn(reference);
        when(this.documentAccessBridge.getDocumentInstance(new EntityReference(reference))).thenReturn(xWikiDocument);
        when(this.entityReferenceConverter.convert(String.class, reference)).thenReturn("document:xwiki:XWiki.Foo");
        BaseObject averageRating1 = mock(BaseObject.class);
        BaseObject averageRating2 = mock(BaseObject.class);

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(asList(averageRating1, averageRating2));
        when(averageRating1.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName()))
            .thenReturn("something");
        when(averageRating2.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName()))
            .thenReturn(managerId);
        when(averageRating2.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName()))
            .thenReturn("document:xwiki:XWiki.Foo");

        when(this.stringEntityReferenceSerializer.serialize(reference)).thenReturn("document:xwiki:XWiki.Foo");
        int hashCode = new HashCodeBuilder()
            .append(managerId)
            .append("document:xwiki:XWiki.Foo")
            .append("document")
            .toHashCode();
        String expectedId = String.format("document:xwiki:XWiki.Foo_%s", hashCode);
        int expectedTotalVote = 12;
        float expectedAverageVote = 5.67f;
        Date expectedUpdateDate = new Date(154);
        int expectedScale = 6;

        when(averageRating2.getIntValue(AverageRatingQueryField.TOTAL_VOTE.getFieldName()))
            .thenReturn(expectedTotalVote);
        when(averageRating2.getFloatValue(AverageRatingQueryField.AVERAGE_VOTE.getFieldName()))
            .thenReturn(expectedAverageVote);
        when(averageRating2.getDateValue(AverageRatingQueryField.UPDATED_AT.getFieldName()))
            .thenReturn(expectedUpdateDate);
        when(averageRating2.getIntValue(AverageRatingQueryField.SCALE.getFieldName(), 5)).thenReturn(expectedScale);

        AverageRating expectedAverageRating = new DefaultAverageRating(expectedId)
            .setManagerId(managerId)
            .setReference(reference)
            .setAverageVote(expectedAverageVote)
            .setTotalVote(expectedTotalVote)
            .setScaleUpperBound(expectedScale)
            .setUpdatedAt(expectedUpdateDate);
        assertEquals(expectedAverageRating, this.averageRatingManager.getAverageRating(reference));
    }

    @Test
    void getAverageRatingNotExisting() throws Exception
    {
        String managerId = "myRatings";
        when(this.ratingsManager.getIdentifier()).thenReturn(managerId);
        DocumentReference reference = new DocumentReference("xwiki","XWiki", "Foo");
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(xWikiDocument.clone()).thenReturn(xWikiDocument);
        when(xWikiDocument.getDocumentReference()).thenReturn(reference);
        when(this.documentAccessBridge.getDocumentInstance(new EntityReference(reference))).thenReturn(xWikiDocument);
        when(this.entityReferenceConverter.convert(String.class, reference)).thenReturn("document:xwiki:XWiki.Foo");
        BaseObject averageRating1 = mock(BaseObject.class);
        BaseObject averageRating2 = mock(BaseObject.class);

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(asList(averageRating1, averageRating2));
        // Wrong identifier
        when(averageRating1.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName()))
            .thenReturn("something");
        when(averageRating2.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName()))
            .thenReturn(managerId);
        // Wrong reference
        when(averageRating2.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName()))
            .thenReturn("document:xwiki:XWiki.Bar");

        when(this.stringEntityReferenceSerializer.serialize(reference)).thenReturn("document:xwiki:XWiki.Foo");
        int hashCode = new HashCodeBuilder()
            .append(managerId)
            .append("document:xwiki:XWiki.Foo")
            .append("document")
            .toHashCode();
        String expectedId = String.format("document:xwiki:XWiki.Foo_%s", hashCode);
        int expectedTotalVote = 0;
        float expectedAverageVote = 0;
        int expectedScale = 6;
        when(this.ratingsManager.getScale()).thenReturn(expectedScale);

        DefaultAverageRating expectedAverageRating = new DefaultAverageRating(expectedId)
            .setManagerId(managerId)
            .setReference(reference)
            .setAverageVote(expectedAverageVote)
            .setTotalVote(expectedTotalVote)
            .setScaleUpperBound(expectedScale);
        AverageRating averageRating = this.averageRatingManager.getAverageRating(reference);
        expectedAverageRating.setUpdatedAt(averageRating.getUpdatedAt());

        assertEquals(expectedAverageRating, averageRating);
    }

    @Test
    void saveAverageRatingCreate() throws Exception
    {
        DocumentReference reference = new DocumentReference("xwiki","XWiki", "Foo");
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(xWikiDocument.clone()).thenReturn(xWikiDocument);
        when(xWikiDocument.getDocumentReference()).thenReturn(reference);
        when(this.documentAccessBridge.getDocumentInstance(new EntityReference(reference))).thenReturn(xWikiDocument);
        when(this.entityReferenceConverter.convert(String.class, reference)).thenReturn("document:xwiki:XWiki.Foo");

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(Collections.emptyList());
        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);
        BaseObject xObject = mock(BaseObject.class);
        when(xWikiDocument.newXObject(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE, context))
            .thenReturn(xObject);
        when(xObject.getOwnerDocument()).thenReturn(xWikiDocument);

        String expectedManagerID = "managerIdentifier";
        int expectedScale = 13;
        int expectedTotalVotes = 43;
        float expectedAverageVote = 8.43f;
        Date expectedDate = new Date(143);

        when(this.ratingsManager.getIdentifier()).thenReturn(expectedManagerID);
        when(this.ratingsManager.getScale()).thenReturn(expectedScale);

        AverageRating averageRating = new DefaultAverageRating("")
            .setReference(reference)
            .setTotalVote(expectedTotalVotes)
            .setAverageVote(expectedAverageVote)
            .setUpdatedAt(expectedDate);

        XWiki xWiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xWiki);

        when(this.contextualLocalizationManager.getTranslationPlain("ratings.averagerating.manager.update.comment"))
            .thenReturn("Update average rating");

        this.averageRatingManager.saveAverageRating(averageRating);

        verify(xObject).setStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName(), expectedManagerID);
        verify(xObject).setIntValue(AverageRatingQueryField.SCALE.getFieldName(), expectedScale);
        verify(xObject).setIntValue(AverageRatingQueryField.TOTAL_VOTE.getFieldName(), expectedTotalVotes);
        verify(xObject).setFloatValue(AverageRatingQueryField.AVERAGE_VOTE.getFieldName(), expectedAverageVote);
        verify(xObject).setDateValue(AverageRatingQueryField.UPDATED_AT.getFieldName(), expectedDate);
        verify(xWiki).saveDocument(xWikiDocument, "Update average rating", true, context);
    }

    @Test
    void saveExistingRating() throws Exception
    {
        DocumentReference reference = new DocumentReference("xwiki","XWiki", "Foo");
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(xWikiDocument.clone()).thenReturn(xWikiDocument);
        when(xWikiDocument.getDocumentReference()).thenReturn(reference);
        when(this.documentAccessBridge.getDocumentInstance(new EntityReference(reference))).thenReturn(xWikiDocument);
        when(this.entityReferenceConverter.convert(String.class, reference)).thenReturn("document:xwiki:XWiki.Foo");

        BaseObject matchingAverageRating = mock(BaseObject.class);
        when(matchingAverageRating.getOwnerDocument()).thenReturn(xWikiDocument);
        BaseObject anotherAverageRating = mock(BaseObject.class);

        String expectedManagerID = "managerIdentifier";
        when(this.ratingsManager.getIdentifier()).thenReturn(expectedManagerID);

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(asList(matchingAverageRating, anotherAverageRating));
        // Wrong identifier
        when(anotherAverageRating.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName()))
            .thenReturn("something");
        when(matchingAverageRating.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName()))
            .thenReturn(expectedManagerID);
        // Wrong reference
        when(matchingAverageRating.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName()))
            .thenReturn("document:xwiki:XWiki.Foo");
        XWikiContext context = mock(XWikiContext.class);

        when(this.contextProvider.get()).thenReturn(context);

        int expectedScale = 13;
        int expectedTotalVotes = 43;
        float expectedAverageVote = 8.43f;
        Date expectedDate = new Date(143);
        when(this.ratingsManager.getScale()).thenReturn(expectedScale);

        AverageRating averageRating = new DefaultAverageRating("")
            .setReference(reference)
            .setTotalVote(expectedTotalVotes)
            .setAverageVote(expectedAverageVote)
            .setUpdatedAt(expectedDate);

        XWiki xWiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xWiki);

        when(this.contextualLocalizationManager.getTranslationPlain("ratings.averagerating.manager.update.comment"))
            .thenReturn("Update average rating");

        this.averageRatingManager.saveAverageRating(averageRating);

        verify(matchingAverageRating).setStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName(),
            expectedManagerID);
        verify(matchingAverageRating).setIntValue(AverageRatingQueryField.SCALE.getFieldName(), expectedScale);
        verify(matchingAverageRating).setIntValue(AverageRatingQueryField.TOTAL_VOTE.getFieldName(),
            expectedTotalVotes);
        verify(matchingAverageRating).setFloatValue(AverageRatingQueryField.AVERAGE_VOTE.getFieldName(),
            expectedAverageVote);
        verify(matchingAverageRating).setDateValue(AverageRatingQueryField.UPDATED_AT.getFieldName(), expectedDate);
        verify(xWiki).saveDocument(xWikiDocument, "Update average rating", true, context);
        verify(xWikiDocument, never()).newXObject(any(), any());
    }

    @Test
    void removeAverageRatingsWikiReference() throws Exception
    {
        assertEquals(0, this.averageRatingManager.removeAverageRatings(new WikiReference("foo")));
        verify(this.documentAccessBridge, never()).getDocumentInstance(any());
    }

    @Test
    void removeAverageRatingsNotExisting() throws Exception
    {
        DocumentReference reference = new DocumentReference("xwiki","XWiki", "Foo");
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(xWikiDocument.clone()).thenReturn(xWikiDocument);
        when(xWikiDocument.getDocumentReference()).thenReturn(reference);
        when(this.documentAccessBridge.getDocumentInstance(new EntityReference(reference))).thenReturn(xWikiDocument);

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(Collections.emptyList());
        assertEquals(0, this.averageRatingManager.removeAverageRatings(reference));
        verify(xWikiDocument, never()).removeXObject(any());
    }

    @Test
    void removeAverageRatings() throws Exception
    {
        DocumentReference reference = new DocumentReference("xwiki","XWiki", "Foo");
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(xWikiDocument.clone()).thenReturn(xWikiDocument);
        when(xWikiDocument.getDocumentReference()).thenReturn(reference);
        when(this.documentAccessBridge.getDocumentInstance(new EntityReference(reference))).thenReturn(xWikiDocument);
        when(this.entityReferenceConverter.convert(String.class, reference)).thenReturn("document:xwiki:XWiki.Foo");

        BaseObject matchingAverageRating = mock(BaseObject.class);
        when(matchingAverageRating.getOwnerDocument()).thenReturn(xWikiDocument);
        BaseObject anotherAverageRating = mock(BaseObject.class);

        String expectedManagerID = "managerIdentifier";
        when(this.ratingsManager.getIdentifier()).thenReturn(expectedManagerID);

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(asList(matchingAverageRating, anotherAverageRating));
        // Wrong identifier
        when(anotherAverageRating.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName()))
            .thenReturn("something");
        when(matchingAverageRating.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName()))
            .thenReturn(expectedManagerID);
        // Wrong reference
        when(matchingAverageRating.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName()))
            .thenReturn("document:xwiki:XWiki.Foo");
        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);
        XWiki xWiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xWiki);
        when(this.contextualLocalizationManager.getTranslationPlain("ratings.averagerating.manager.remove.comment"))
            .thenReturn("Remove average rating");

        assertEquals(1, this.averageRatingManager.removeAverageRatings(reference));

        verify(xWikiDocument).removeXObject(matchingAverageRating);
        verify(xWiki).saveDocument(xWikiDocument, "Remove average rating", true, context);
    }

    @Test
    void moveAverageRatingsOldNull()
    {
        RatingsException ratingsException =
            assertThrows(RatingsException.class, () -> this.averageRatingManager.moveAverageRatings(
                new DocumentReference("xwiki", "XWiki", "Old"), null));

        assertEquals("Impossible to move the average ratings from [xwiki:XWiki.Old] to [null].",
            ratingsException.getMessage());
    }

    @Test
    void moveAverageRatingsNewNull()
    {
        RatingsException ratingsException =
            assertThrows(RatingsException.class, () -> this.averageRatingManager.moveAverageRatings(null,
                new DocumentReference("xwiki",
                    "XWiki", "New")));
        assertEquals("Impossible to move the average ratings from [null] to [xwiki:XWiki.New].",
            ratingsException.getMessage());
    }

    @Test
    void moveAverageRatingsOldWrongType()
    {
        RatingsException ratingsException =
            assertThrows(RatingsException.class,
                () -> this.averageRatingManager.moveAverageRatings(new SpaceReference("xwiki", "Space"),
                    new DocumentReference("xwiki",
                        "XWiki", "New")));
        assertEquals("Impossible to move the average ratings from [Space xwiki:Space] to [xwiki:XWiki.New].",
            ratingsException.getMessage());
    }

    @Test
    void moveAverageRatingsNewWrongType()
    {
        RatingsException ratingsException =
            assertThrows(RatingsException.class,
                () -> this.averageRatingManager.moveAverageRatings(new DocumentReference("xwiki",
                        "XWiki", "Old"),
                    new SpaceReference("xwiki", "Space")));
        assertEquals("Impossible to move the average ratings from [xwiki:XWiki.Old] to [Space xwiki:Space].",
            ratingsException.getMessage());
    }

    @Test
    void moveAverageRatings() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("xwiki","XWiki", "Old");
        DocumentReference newReference = new DocumentReference("xwiki","XWiki", "New");
        XWikiContext context = mock(XWikiContext.class);
        XWikiDocument actualDoc = mock(XWikiDocument.class);
        when(actualDoc.getDocumentReference()).thenReturn(newReference);
        // Skipped because wrong manager
        BaseObject ratings1 = mock(BaseObject.class);
        // Skiped because wrong entity
        BaseObject ratings2 = mock(BaseObject.class);
        // Pass because references Old
        BaseObject ratings3 = mock(BaseObject.class);
        // Pass because references a child of Old
        BaseObject ratings4 = mock(BaseObject.class);
        String expectedManager = "expectedManager";
        XWiki xWiki = mock(XWiki.class);
        Date r3UpdateDate = new Date();
        Date r4UpdateDate = new Date();
        EntityReference oldObjectEntityReference = new EntityReference("XWiki.Object", EntityType.OBJECT, oldReference);
        EntityReference newObjectEntityReference = new EntityReference("XWiki.Object", EntityType.OBJECT, newReference);

        when(this.contextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(xWiki);
        when(this.documentAccessBridge.getDocumentInstance(new EntityReference(newReference))).thenReturn(actualDoc);
        when(actualDoc.clone()).thenReturn(actualDoc);
        when(actualDoc
            .getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(asList(ratings1, ratings2, ratings3, ratings4));

        when(ratings1.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName()))
            .thenReturn("ratings1EntityReference");
        when(ratings1.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName())).thenReturn("wrongManager");

        when(ratings2.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName())).thenReturn(expectedManager);
        when(ratings2.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName()))
            .thenReturn("xwiki:XWiki.Unknown");

        when(ratings3.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName())).thenReturn(expectedManager);
        when(ratings3.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName()))
            .thenReturn("xwiki:XWiki.Old");
        when(ratings3.getFloatValue(AverageRatingQueryField.AVERAGE_VOTE.getFieldName())).thenReturn(10f);
        when(ratings3.getDateValue(AverageRatingQueryField.UPDATED_AT.getFieldName())).thenReturn(r3UpdateDate);
        when(ratings3.getIntValue(AverageRatingQueryField.SCALE.getFieldName(), 5)).thenReturn(9);

        when(ratings4.getStringValue(AverageRatingQueryField.MANAGER_ID.getFieldName())).thenReturn(expectedManager);
        when(ratings4.getStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName()))
            .thenReturn("xwiki:XWiki.Old^XWiki.Object");
        when(ratings4.getFloatValue(AverageRatingQueryField.AVERAGE_VOTE.getFieldName())).thenReturn(20f);
        when(ratings4.getDateValue(AverageRatingQueryField.UPDATED_AT.getFieldName())).thenReturn(r4UpdateDate);
        when(ratings4.getIntValue(AverageRatingQueryField.SCALE.getFieldName(), 5)).thenReturn(2);

        when(this.ratingsManager.getIdentifier()).thenReturn(expectedManager);
        when(this.entityReferenceConverter.convert(EntityReference.class, "xwiki:XWiki.Unknown"))
            .thenReturn(new EntityReference("xwiki:XWiki.Unknown", EntityType.DOCUMENT));
        when(this.entityReferenceConverter.convert(EntityReference.class, "xwiki:XWiki.Old"))
            .thenReturn(oldReference);
        when(this.entityReferenceConverter.convert(EntityReference.class, "xwiki:XWiki.Old^XWiki.Object"))
            .thenReturn(oldObjectEntityReference);
        when(this.entityReferenceConverter.convert(String.class, newReference)).thenReturn("xwiki:XWiki.New");
        when(this.entityReferenceConverter.convert(String.class, newObjectEntityReference))
            .thenReturn("xwiki:XWiki.Old^XWiki.Object");

        when(this.stringEntityReferenceSerializer.serialize(newReference.extractReference(DOCUMENT))).thenReturn(
            "xwiki:XWiki.New");
        when(this.contextualLocalizationManager.getTranslationPlain("ratings.averagerating.manager.move.comment"))
            .thenReturn("Move average ratings XObjects");

        long count = this.averageRatingManager.moveAverageRatings(oldReference, newReference);

        assertEquals(2, count);
        verify(ratings3).setStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(),
            "xwiki:XWiki.New");
        verify(ratings4).setStringValue(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(),
            "xwiki:XWiki.Old^XWiki.Object");
        verify(xWiki).saveDocument(actualDoc, "Move average ratings XObjects", true, context);
        List<DefaultAverageRating> ratingsList = asList(
            new DefaultAverageRating("xwiki:XWiki.New_1593976669")
                .setManagerId("expectedManager")
                .setReference(newReference)
                .setAverageVote(10f)
                .setUpdatedAt(r3UpdateDate)
                .setScaleUpperBound(9),
            new DefaultAverageRating("xwiki:XWiki.New_-989039879")
                .setManagerId("expectedManager")
                .setReference(newObjectEntityReference)
                .setAverageVote(20f)
                .setUpdatedAt(r4UpdateDate)
                .setScaleUpperBound(2)
        );
        verify(this.observationManager).notify(new UpdatingAverageRatingEvent(), expectedManager, ratingsList);
        verify(this.observationManager).notify(new UpdatedAverageRatingEvent(), expectedManager, ratingsList);
    }
}
