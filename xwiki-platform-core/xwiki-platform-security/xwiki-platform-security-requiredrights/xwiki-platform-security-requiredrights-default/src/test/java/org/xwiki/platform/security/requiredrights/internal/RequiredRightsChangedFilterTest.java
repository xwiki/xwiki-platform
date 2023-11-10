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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.model.EntityType.DOCUMENT;
import static org.xwiki.model.EntityType.WIKI;
import static org.xwiki.security.authorization.Right.SCRIPT;

/**
 * Test of {@link RequiredRightsChangedFilter}.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@ComponentTest
class RequiredRightsChangedFilterTest
{
    private static final DocumentReference CURRENT_USER_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "CurrentUser");

    private static final DocumentReference CONTENT_AUTHOR_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "ContentAuthor");

    private static final DocumentReference EFFECTIVE_METADATA_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "EffectiveMetadataAuthor");

    @InjectMockComponents
    private RequiredRightsChangedFilter filter;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @Mock
    private XWikiContext context;

    @Mock
    private DocumentAuthors documentAuthors;

    @Mock
    private UserReference contentAuthorUserReference;

    @Mock
    private UserReference effectiveMetadataAuthorUserReference;

    @BeforeEach
    void setUp()
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getUserReference()).thenReturn(CURRENT_USER_REFERENCE);
        when(this.documentAuthors.getContentAuthor()).thenReturn(this.contentAuthorUserReference);
        when(this.documentAuthors.getEffectiveMetadataAuthor()).thenReturn(this.effectiveMetadataAuthorUserReference);
        when(this.userReferenceSerializer.serialize(this.contentAuthorUserReference))
            .thenReturn(CONTENT_AUTHOR_REFERENCE);
        when(this.userReferenceSerializer.serialize(this.effectiveMetadataAuthorUserReference))
            .thenReturn(EFFECTIVE_METADATA_REFERENCE);
    }

    @Test
    void filterNoResults()
    {
        assertEquals(new RequiredRightsChangedResult(), this.filter.filter(this.documentAuthors, List.of()));
    }

    @Test
    void filterNullResults()
    {
        assertEquals(new RequiredRightsChangedResult(), this.filter.filter(this.documentAuthors, null));
    }

    @Test
    void filterTooMuchRights()
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");
        ObjectReference objectReference = new ObjectReference("XWiki.XObj", documentReference);
        WikiReference wikiReference = documentReference.getWikiReference();
        when(this.authorizationManager.hasAccess(SCRIPT, CURRENT_USER_REFERENCE, wikiReference)).thenReturn(true);
        when(this.authorizationManager.hasAccess(SCRIPT, EFFECTIVE_METADATA_REFERENCE, wikiReference)).thenReturn(
            false);

        List<RequiredRight> requiredRights = List.of(new RequiredRight(SCRIPT, WIKI, false));
        RequiredRightAnalysisResult requiredRightAnalysisResult =
            new RequiredRightAnalysisResult(objectReference, () -> null, () -> null, requiredRights);
        List<RequiredRightAnalysisResult> resultList = List.of(requiredRightAnalysisResult);
        RequiredRightsChangedResult expected = new RequiredRightsChangedResult();
        expected.add(requiredRightAnalysisResult, SCRIPT, true, false);
        assertEquals(expected, this.filter.filter(this.documentAuthors, resultList));
    }

    @Test
    void filterTooFewRights()
    {
        DocumentReference entityReference = new DocumentReference("xwiki", "Space", "Page");
        when(this.authorizationManager.hasAccess(SCRIPT, CURRENT_USER_REFERENCE, entityReference)).thenReturn(false);
        when(this.authorizationManager.hasAccess(SCRIPT, CONTENT_AUTHOR_REFERENCE, entityReference)).thenReturn(true);

        List<RequiredRight> requiredRights = List.of(new RequiredRight(SCRIPT, DOCUMENT, false));
        RequiredRightAnalysisResult requiredRightAnalysisResult =
            new RequiredRightAnalysisResult(entityReference, () -> null, () -> null, requiredRights);
        List<RequiredRightAnalysisResult> resultList = List.of(requiredRightAnalysisResult);
        RequiredRightsChangedResult expected = new RequiredRightsChangedResult();
        expected.add(requiredRightAnalysisResult, SCRIPT, false, false);
        assertEquals(expected, this.filter.filter(this.documentAuthors, resultList));
    }

    @ParameterizedTest
    @CsvSource({
        "true,true",
        "false,false"
    })
    void filterSameRights(boolean currentUserHasAccess, boolean contentAuthorHasAccess)
    {
        DocumentReference entityReference = new DocumentReference("xwiki", "Space", "Page");
        when(this.authorizationManager.hasAccess(SCRIPT, CURRENT_USER_REFERENCE, entityReference))
            .thenReturn(currentUserHasAccess);
        when(this.authorizationManager.hasAccess(SCRIPT, CONTENT_AUTHOR_REFERENCE, entityReference))
            .thenReturn(contentAuthorHasAccess);

        List<RequiredRight> requiredRights = List.of(new RequiredRight(SCRIPT, DOCUMENT, false));
        List<RequiredRightAnalysisResult> resultList =
            List.of(new RequiredRightAnalysisResult(entityReference, () -> null, () -> null, requiredRights));
        assertEquals(new RequiredRightsChangedResult(), this.filter.filter(this.documentAuthors, resultList));
    }

    @Test
    void filterCurrentAuthorIsDocumentAuthor()
    {
        when(this.context.getUserReference()).thenReturn(CONTENT_AUTHOR_REFERENCE);
        DocumentReference entityReference = new DocumentReference("xwiki", "Space", "Page");

        List<RequiredRight> requiredRights = List.of(new RequiredRight(SCRIPT, DOCUMENT, false));
        List<RequiredRightAnalysisResult> resultList =
            List.of(new RequiredRightAnalysisResult(entityReference, () -> null, () -> null, requiredRights));
        assertEquals(new RequiredRightsChangedResult(), this.filter.filter(this.documentAuthors, resultList));
        // No need to check the rights are the author is the current user
        verifyNoInteractions(this.authorizationManager);
    }
}
