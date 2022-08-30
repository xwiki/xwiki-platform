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
package org.xwiki.search.solr.internal;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Validate {@link DefaultIndexingUserConfig}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultIndexingUserConfigTest
{
    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @InjectMockComponents
    private DefaultIndexingUserConfig indexUserConfig;

    private DocumentReference wikiConfigRef = new DocumentReference("xwiki", "XWiki", "SolrSearchAdminIndexingUser");
    private DocumentReference wikiConfigClassRef = new DocumentReference("xwiki", "XWiki", "SolrSearchAdminIndexingUserClass");

    private XWikiDocument testMainwikiConfigDocument;

    @BeforeEach
    public void setUp() throws XWikiException
    {
        testMainwikiConfigDocument = mock(XWikiDocument.class);

        XWikiContext testContext = mock(XWikiContext.class);
        XWiki testMainwiki = mock(XWiki.class);
        when(testMainwiki.getDocument(wikiConfigRef, testContext)).thenReturn(testMainwikiConfigDocument);

        when(testContext.getWiki()).thenReturn(testMainwiki);
        when(testContext.getWikiReference()).thenReturn(new WikiReference("xwiki"));
        when(contextProvider.get()).thenReturn(testContext);
    }

    @Test
    public void mainWikiWithoutSettings() throws XWikiException
    {
        assertEquals(null, indexUserConfig.getIndexingUserReference());
    }

    @Test
    public void mainWikiWithSettings() throws XWikiException
    {
        String mainwikiIndexingUserString = "expected by mock resolver";
        DocumentReference mainwikiIndexingUser = new DocumentReference("xwiki", "XWiki", "SomeIndexingUser");
        when(documentReferenceResolver.resolve(mainwikiIndexingUserString)).thenReturn(mainwikiIndexingUser);

        BaseObject testMainwikiConfigObject = mock(BaseObject.class);
        when(testMainwikiConfigObject.getLargeStringValue("indexer")).thenReturn(mainwikiIndexingUserString);
        when(testMainwikiConfigDocument.getXObject(wikiConfigClassRef)).thenReturn(testMainwikiConfigObject);

        assertEquals(mainwikiIndexingUser, indexUserConfig.getIndexingUserReference());
    }
}
