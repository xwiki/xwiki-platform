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
package org.xwiki.ratings.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.ratings.script.RatingsScriptService.EXECUTION_CONTEXT_PREFIX;

/**
 * Tests for {@link RatingsScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
public class RatingsScriptServiceTest
{
    @InjectMockComponents
    private RatingsScriptService scriptService;

    @MockComponent
    private RatingsManagerFactory ratingsManagerFactory;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private RatingsConfiguration ratingsConfiguration;

    private UserReference currentUser;

    private RatingsManager defaultManager;

    @BeforeComponent
    void beforeComponent() throws RatingsException
    {
        this.defaultManager = mock(RatingsManager.class);
        when(this.ratingsManagerFactory.getRatingsManager(RatingsManagerFactory.DEFAULT_APP_HINT))
            .thenReturn(this.defaultManager);
    }

    @BeforeEach
    void setup()
    {
        XWikiContext xWikiContext = mock(XWikiContext.class);
        this.currentUser = mock(UserReference.class);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        DocumentReference userDocReference = mock(DocumentReference.class);
        when(xWikiContext.getUserReference()).thenReturn(userDocReference);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(this.currentUser);
        this.ratingsConfiguration = mock(RatingsConfiguration.class);
        when(this.defaultManager.getRatingConfiguration()).thenReturn(this.ratingsConfiguration);
    }

    @Test
    void get() throws Exception
    {
        String requestedHint = "customHint";
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);

        when(executionContext.hasProperty(EXECUTION_CONTEXT_PREFIX + requestedHint)).thenReturn(false);
        RatingsManager ratingsManager = mock(RatingsManager.class);
        when(this.ratingsManagerFactory.getRatingsManager(requestedHint)).thenReturn(ratingsManager);
        DefaultScriptRatingsManager scriptRatingsManager = mock(DefaultScriptRatingsManager.class);
        when(this.componentManager.getInstance(DefaultScriptRatingsManager.class))
            .thenReturn(scriptRatingsManager);

        assertEquals(scriptRatingsManager, this.scriptService.get(requestedHint));
        verify(scriptRatingsManager).setRatingsManager(ratingsManager);
        verify(executionContext).setProperty(EXECUTION_CONTEXT_PREFIX + requestedHint, scriptRatingsManager);
    }

    @Test
    void setRating() throws Exception
    {
        EntityReference reference = mock(EntityReference.class);
        int vote = 12;
        Rating expectedRating = mock(Rating.class);
        when(this.defaultManager.saveRating(reference, this.currentUser, vote)).thenReturn(expectedRating);
        when(this.ratingsConfiguration.getExcludedReferencesFromRatings()).thenReturn(Collections.emptySet());
        assertEquals(Optional.of(expectedRating), this.scriptService.setRating(reference, vote));
    }

    @Test
    void setRatingWithUserRefernce() throws Exception
    {
        EntityReference reference = mock(EntityReference.class);
        UserReference userReference = mock(UserReference.class);
        int vote = 42;
        Rating expectedRating = mock(Rating.class);
        when(this.defaultManager.saveRating(reference, userReference, vote)).thenReturn(expectedRating);
        when(this.ratingsConfiguration.getExcludedReferencesFromRatings()).thenReturn(Collections.emptySet());
        assertEquals(Optional.of(expectedRating), this.scriptService.setRating(reference, userReference, vote));
    }

    @Test
    void setRatingExcludedReference() throws Exception
    {
        EntityReference reference = mock(EntityReference.class);
        UserReference userReference = mock(UserReference.class);
        int vote = 42;
        when(this.ratingsConfiguration.getExcludedReferencesFromRatings()).thenReturn(new HashSet<>(Arrays.asList(
            mock(EntityReference.class),
            mock(EntityReference.class),
            reference,
            mock(EntityReference.class)
        )));
        assertEquals(Optional.empty(), this.scriptService.setRating(reference, userReference, vote));
        verify(this.defaultManager, never()).saveRating(reference, userReference, vote);
    }

    @Test
    void getRatings() throws Exception
    {
        EntityReference reference = mock(EntityReference.class);
        int offset = 12;
        int limit = 17;
        Map<RatingsManager.RatingQueryField, Object> queryParameters = new HashMap<>();
        queryParameters.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, reference);

        List<Rating> expectedResult = Arrays.asList(mock(Rating.class), mock(Rating.class));
        when(this.defaultManager.getRatings(queryParameters, offset, limit,
            RatingsManager.RatingQueryField.UPDATED_DATE, false)).thenReturn(expectedResult);
        assertEquals(expectedResult, this.scriptService.getRatings(reference, offset, limit));
    }

    @Test
    void getRatingsAscending() throws Exception
    {
        EntityReference reference = mock(EntityReference.class);
        int offset = 23;
        int limit = 3;
        Map<RatingsManager.RatingQueryField, Object> queryParameters = new HashMap<>();
        queryParameters.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, reference);

        List<Rating> expectedResult = Arrays.asList(mock(Rating.class), mock(Rating.class));
        when(this.defaultManager.getRatings(queryParameters, offset, limit,
            RatingsManager.RatingQueryField.UPDATED_DATE, true)).thenReturn(expectedResult);
        assertEquals(expectedResult, this.scriptService.getRatings(reference, offset, limit, true));
    }

    @Test
    void getAverageRating() throws Exception
    {
        EntityReference reference = mock(EntityReference.class);
        AverageRating expectedAverage = mock(AverageRating.class);

        when(this.defaultManager.getAverageRating(reference)).thenReturn(expectedAverage);
        assertEquals(Optional.of(expectedAverage), this.scriptService.getAverageRating(reference));
    }

    @Test
    void getRating() throws Exception
    {
        EntityReference reference = mock(EntityReference.class);
        UserReference userReference = mock(UserReference.class);
        Map<RatingsManager.RatingQueryField, Object> queryParameters = new HashMap<>();
        queryParameters.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, reference);
        queryParameters.put(RatingsManager.RatingQueryField.USER_REFERENCE, userReference);
        Rating expectedRating = mock(Rating.class);

        when(this.defaultManager.getRatings(queryParameters, 0, 1,
            RatingsManager.RatingQueryField.UPDATED_DATE, false)).thenReturn(Collections.singletonList(expectedRating));

        assertEquals(Optional.of(expectedRating), this.scriptService.getRating(reference, userReference));
    }

    @Test
    void getRatingWhenMissing() throws Exception
    {
        EntityReference reference = mock(EntityReference.class);
        UserReference userReference = mock(UserReference.class);
        Map<RatingsManager.RatingQueryField, Object> queryParameters = new HashMap<>();
        queryParameters.put(RatingsManager.RatingQueryField.ENTITY_REFERENCE, reference);
        queryParameters.put(RatingsManager.RatingQueryField.USER_REFERENCE, userReference);

        when(this.defaultManager.getRatings(queryParameters, 0, 1,
            RatingsManager.RatingQueryField.UPDATED_DATE, false)).thenReturn(Collections.emptyList());

        assertEquals(Optional.empty(), this.scriptService.getRating(reference, userReference));
    }

    @Test
    void getCurrentUserRatings() throws Exception
    {
        int offset = 11;
        int limit = 23;
        boolean asc = true;

        List<Rating> expectedResult = Arrays.asList(mock(Rating.class), mock(Rating.class));

        Map<RatingsManager.RatingQueryField, Object> queryParameters = new HashMap<>();
        queryParameters.put(RatingsManager.RatingQueryField.USER_REFERENCE, this.currentUser);
        when(this.defaultManager.getRatings(queryParameters, offset, limit,
            RatingsManager.RatingQueryField.UPDATED_DATE, asc)).thenReturn(expectedResult);
        assertEquals(expectedResult, this.scriptService.getCurrentUserRatings(offset, limit, asc));
    }

    @Test
    void getConfiguration()
    {
        assertSame(this.ratingsConfiguration, this.scriptService.getConfiguration());
    }

    @Test
    void isExcludedFromRatings()
    {
        EntityReference inputRef = mock(EntityReference.class);
        when(this.ratingsConfiguration.getExcludedReferencesFromRatings()).thenReturn(Collections.emptySet());
        assertFalse(this.scriptService.isExcludedFromRatings(inputRef));

        EntityReference lastExcludedRef = mock(EntityReference.class);
        when(this.ratingsConfiguration.getExcludedReferencesFromRatings()).thenReturn(new LinkedHashSet<>(Arrays.asList(
            mock(EntityReference.class),
            inputRef,
            lastExcludedRef
        )));
        assertTrue(this.scriptService.isExcludedFromRatings(inputRef));

        EntityReference excludedRef = mock(EntityReference.class);
        when(inputRef.hasParent(excludedRef)).thenReturn(true);
        when(this.ratingsConfiguration.getExcludedReferencesFromRatings()).thenReturn(new LinkedHashSet<>(Arrays.asList(
            mock(EntityReference.class),
            excludedRef,
            lastExcludedRef
        )));
        assertTrue(this.scriptService.isExcludedFromRatings(inputRef));

        // Ensure that we stop the loop whenever we found a match.
        verify(inputRef, never()).hasParent(lastExcludedRef);
    }

    @Test
    void recomputeAverageRating() throws RatingsException
    {
        DocumentReference docRef = mock(DocumentReference.class);

        when(this.authorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        AverageRating averageRating = mock(AverageRating.class);
        when(this.defaultManager.recomputeAverageRating(docRef)).thenReturn(averageRating);
        assertEquals(Optional.of(averageRating), this.scriptService.recomputeAverageRating(docRef));
    }
}
