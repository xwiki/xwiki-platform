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
package org.xwiki.wiki.user.internal.membermigration;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.user.internal.WikiCandidateMemberClassInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultMemberCandidaciesMigrator}.
 *
 * @version $Id$
 * @since 5.4RC1
 */
public class DefaultMemberCandidaciesMigratorTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultMemberCandidaciesMigrator> mocker =
            new MockitoComponentMockingRule(DefaultMemberCandidaciesMigrator.class);

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));

        xcontext = mock(XWikiContext.class);
        xwiki = mock(XWiki.class);

        when(xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);
    }

    @Test
    public void migrateCandidacies() throws Exception
    {
        XWikiDocument allGroupDoc = mock(XWikiDocument.class);
        XWikiDocument memberGroupDoc = mock(XWikiDocument.class);

        when(xwiki.getDocument(eq(new DocumentReference("subwiki", "XWiki", "XWikiAllGroup")),
                any(XWikiContext.class))).thenReturn(allGroupDoc);

        when(xwiki.getDocument(eq(new DocumentReference("subwiki", "XWiki", "XWikiMemberGroup")),
                any(XWikiContext.class))).thenReturn(memberGroupDoc);

        DocumentReference candidateClassReference = new DocumentReference("subwiki",
                WikiCandidateMemberClassInitializer.DOCUMENT_SPACE,
                WikiCandidateMemberClassInitializer.DOCUMENT_NAME);

        List<BaseObject> oldCandidacies = new ArrayList<BaseObject>();
        oldCandidacies.add(null);
        when(allGroupDoc.getXObjects(eq(candidateClassReference))).thenReturn(oldCandidacies);

        BaseObject oldCandidacy = mock(BaseObject.class);
        when(oldCandidacy.getStringValue("type")).thenReturn("test Type");
        oldCandidacies.add(oldCandidacy);

        BaseObject newCandidacy = mock(BaseObject.class);
        when(memberGroupDoc.newXObject(eq(candidateClassReference), any(XWikiContext.class))).thenReturn(newCandidacy);

        // Test
        mocker.getComponentUnderTest().migrateCandidacies("subwiki");

        // Verify
        verify(newCandidacy).setStringValue("type", "test Type");
        verify(allGroupDoc).removeXObject(oldCandidacy);
        verify(xwiki).saveDocument(allGroupDoc, "[UPGRADE] Move candidacies from XWikiAllGroup to XWikiMemberGroup.", xcontext);
        verify(xwiki).saveDocument(memberGroupDoc, "[UPGRADE] Move candidacies from XWikiAllGroup to XWikiMemberGroup.", xcontext);

    }

    @Test
    public void migrateCandidaciesWithException() throws Exception
    {
        XWikiException exception = new XWikiException();
        when(xwiki.getDocument(eq(new DocumentReference("subwiki", "XWiki", "XWikiAllGroup")),
                any(XWikiContext.class))).thenThrow(exception);

        // Test
        boolean exceptionCaught = false;
        try {
            mocker.getComponentUnderTest().migrateCandidacies("subwiki");
        } catch (DataMigrationException e) {
            exceptionCaught = true;
            assertEquals("Failed to move candidacies from XWikiAllGroup to XWikiMemberGroup.", e.getMessage());
        }

        assertTrue(exceptionCaught);
    }

}

