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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import javax.inject.Provider;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.properties.converter.Converter;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.internal.averagerating.AverageRatingManager.AverageRatingQueryField;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link XObjectAverageRatingManager}.
 *
 * @version $Id$
 */
@ComponentTest
public class XObjectAverageRatingManagerTest
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
        EntityReference reference = new EntityReference("xwiki:XWiki.Foo", EntityType.DOCUMENT);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getDocumentInstance(reference)).thenReturn(xWikiDocument);
        when(this.entityReferenceConverter.convert(String.class, reference)).thenReturn("document:xwiki:XWiki.Foo");
        BaseObject averageRating1 = mock(BaseObject.class);
        BaseObject averageRating2 = mock(BaseObject.class);

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(Arrays.asList(averageRating1, averageRating2));
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
        EntityReference reference = new EntityReference("xwiki:XWiki.Foo", EntityType.DOCUMENT);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getDocumentInstance(reference)).thenReturn(xWikiDocument);
        when(this.entityReferenceConverter.convert(String.class, reference)).thenReturn("document:xwiki:XWiki.Foo");
        BaseObject averageRating1 = mock(BaseObject.class);
        BaseObject averageRating2 = mock(BaseObject.class);

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(Arrays.asList(averageRating1, averageRating2));
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
        EntityReference reference = new EntityReference("xwiki:XWiki.Foo", EntityType.DOCUMENT);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getDocumentInstance(reference)).thenReturn(xWikiDocument);
        when(this.entityReferenceConverter.convert(String.class, reference)).thenReturn("document:xwiki:XWiki.Foo");

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(Collections.emptyList());
        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);
        when(xWikiDocument.createXObject(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE, context))
            .thenReturn(42);
        BaseObject xObject = mock(BaseObject.class);
        when(xWikiDocument.getXObject(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE, 42))
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
        EntityReference reference = new EntityReference("xwiki:XWiki.Foo", EntityType.DOCUMENT);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getDocumentInstance(reference)).thenReturn(xWikiDocument);
        when(this.entityReferenceConverter.convert(String.class, reference)).thenReturn("document:xwiki:XWiki.Foo");

        BaseObject matchingAverageRating = mock(BaseObject.class);
        when(matchingAverageRating.getOwnerDocument()).thenReturn(xWikiDocument);
        BaseObject anotherAverageRating = mock(BaseObject.class);

        String expectedManagerID = "managerIdentifier";
        when(this.ratingsManager.getIdentifier()).thenReturn(expectedManagerID);

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(Arrays.asList(matchingAverageRating, anotherAverageRating));
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
        verify(xWikiDocument, never()).createXObject(any(), any());
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
        EntityReference reference = new EntityReference("xwiki:Foo.Bar", EntityType.DOCUMENT);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getDocumentInstance(reference)).thenReturn(xWikiDocument);

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(Collections.emptyList());
        assertEquals(0, this.averageRatingManager.removeAverageRatings(reference));
        verify(xWikiDocument, never()).removeXObject(any());
    }

    @Test
    void removeAverageRatings() throws Exception
    {
        EntityReference reference = new EntityReference("xwiki:XWiki.Foo", EntityType.DOCUMENT);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getDocumentInstance(reference)).thenReturn(xWikiDocument);
        when(this.entityReferenceConverter.convert(String.class, reference)).thenReturn("document:xwiki:XWiki.Foo");

        BaseObject matchingAverageRating = mock(BaseObject.class);
        when(matchingAverageRating.getOwnerDocument()).thenReturn(xWikiDocument);
        BaseObject anotherAverageRating = mock(BaseObject.class);

        String expectedManagerID = "managerIdentifier";
        when(this.ratingsManager.getIdentifier()).thenReturn(expectedManagerID);

        when(xWikiDocument.getXObjects(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSREFERENCE))
            .thenReturn(Arrays.asList(matchingAverageRating, anotherAverageRating));
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
        assertEquals(1, this.averageRatingManager.removeAverageRatings(reference));
        verify(xWikiDocument).removeXObject(matchingAverageRating);
        verify(xWiki).saveDocument(xWikiDocument, "Remove average rating", true, context);
    }
}
