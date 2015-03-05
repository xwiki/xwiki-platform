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
package com.xpn.xwiki.doc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Unit tests for {@link XWikiDocument}.
 * 
 * @version $Id$
 */
public class XWikiDocumentMockitoTest
{
    /**
     * Matches {@link Document}s that wrap the given {@link XWikiDocument} instance.
     */
    private static class Wraps extends ArgumentMatcher<Document>
    {
        /**
         * The document to match.
         */
        private XWikiDocument document;

        /**
         * Creates a new matcher that matches {@link Document}s that wrap the given {@link XWikiDocument} instance.
         * 
         * @param document the document to match
         */
        public Wraps(XWikiDocument document)
        {
            this.document = document;
        }

        @Override
        public boolean matches(Object document)
        {
            return this.document == ((Document) document).getDocument();
        }
    }

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    /**
     * The object being tested.
     */
    private XWikiDocument document;

    @Before
    public void setUp() throws Exception
    {
        this.oldcore.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING);
        this.oldcore.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");

        // Activate programming rights in order to be able to call com.xpn.xwiki.api.Document#getDocument().
        when(this.oldcore.getMockRightService().hasProgrammingRights(this.oldcore.getXWikiContext())).thenReturn(true);

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.oldcore.getMockXWiki().saveDocument(this.document, "", true, this.oldcore.getXWikiContext());
    }

    @Test
    public void getChildrenReferences() throws Exception
    {
        QueryManager queryManager = mock(QueryManager.class);
        when(this.oldcore.getMockStore().getQueryManager()).thenReturn(queryManager);

        Query query = mock(Query.class);
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(query);

        QueryFilter hiddenFilter = this.oldcore.getMocker().registerMockComponent(QueryFilter.class, "hidden");

        when(query.setLimit(7)).thenReturn(query);

        List<Object[]> result = Arrays.asList(new Object[]{ "X", "y" }, new Object[]{ "A", "b" });
        when(query.<Object[]> execute()).thenReturn(result);

        List<DocumentReference> childrenReferences = document.getChildrenReferences(7, 3,
            this.oldcore.getXWikiContext());

        verify(query).addFilter(hiddenFilter);
        verify(query).setLimit(7);
        verify(query).setOffset(3);

        Assert.assertEquals(2, childrenReferences.size());
        Assert.assertEquals(new DocumentReference("wiki", "X", "y"), childrenReferences.get(0));
        Assert.assertEquals(new DocumentReference("wiki", "A", "b"), childrenReferences.get(1));
    }

    /**
     * @see "XWIKI-8024: XWikiDocument#setAsContextDoc doesn't set the 'cdoc' in the Velocity context"
     */
    @Test
    public void setAsContextDoc() throws Exception
    {
        VelocityManager velocityManager = this.oldcore.getMocker().registerMockComponent(VelocityManager.class);
        VelocityContext velocityContext = mock(VelocityContext.class);
        when(velocityManager.getVelocityContext()).thenReturn(velocityContext);

        this.document.setAsContextDoc(this.oldcore.getXWikiContext());

        assertEquals(this.document, this.oldcore.getXWikiContext().getDoc());
        Wraps wrapsThisDocument = new Wraps(this.document);
        verify(velocityContext).put(eq("doc"), argThat(wrapsThisDocument));
        verify(velocityContext).put(eq("tdoc"), argThat(wrapsThisDocument));
        verify(velocityContext).put(eq("cdoc"), argThat(wrapsThisDocument));
    }

    @Test
    public void setTranslationAsContextDoc() throws Exception
    {
        VelocityManager velocityManager = this.oldcore.getMocker().registerMockComponent(VelocityManager.class);
        VelocityContext velocityContext = mock(VelocityContext.class);
        when(velocityManager.getVelocityContext()).thenReturn(velocityContext);

        this.document.setLocale(Locale.US);
        XWikiDocument defaultTranslation = new XWikiDocument(this.document.getDocumentReference());
        when(this.oldcore.getMockXWiki().getDocument(this.document.getDocumentReference(),
            this.oldcore.getXWikiContext())).thenReturn(defaultTranslation);

        this.document.setAsContextDoc(this.oldcore.getXWikiContext());

        assertEquals(this.document, this.oldcore.getXWikiContext().getDoc());
        verify(velocityContext).put(eq("doc"), argThat(new Wraps(defaultTranslation)));
        verify(velocityContext).put(eq("tdoc"), argThat(new Wraps(this.document)));
        verify(velocityContext).put(eq("cdoc"), argThat(new Wraps(this.document)));
    }
}
