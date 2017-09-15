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
package org.xwiki.query.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AttachmentQueryFilter}.
 * 
 * @version $Id$
 * @since 9.7RC1
 */
public class AttachmentQueryFilterTest
{
    @Rule
    public MockitoComponentMockingRule<QueryFilter> mocker =
        new MockitoComponentMockingRule<QueryFilter>(AttachmentQueryFilter.class);

    @Test
    public void filterStatementWithoutFromAndWhere() throws Exception
    {
        String result = this.mocker.getComponentUnderTest()
            .filterStatement("select doc.fullName from XWikiDocument doc order by attachment.date", Query.HQL);
        assertEquals("select doc.fullName, attachment.filename " + "from XWikiDocument doc, XWikiAttachment attachment "
            + "where doc.id = attachment.docId " + "order by attachment.date", result);
    }

    @Test
    public void filterStatementWithWhere() throws Exception
    {
        String result = this.mocker.getComponentUnderTest().filterStatement(
            "select doc.fullName from XWikiDocument doc where attachment.mimeType like 'image/%'", Query.HQL);
        assertEquals("select doc.fullName, attachment.filename " + "from XWikiDocument doc, XWikiAttachment attachment "
            + "where doc.id = attachment.docId and (attachment.mimeType like 'image/%')", result);
    }

    @Test
    public void filterStatementWithFromAndWhere() throws Exception
    {
        String result = this.mocker.getComponentUnderTest()
            .filterStatement("select doc.fullName from XWikiDocument doc, BaseObject as obj "
                + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers'", Query.HQL);
        assertEquals("select doc.fullName, attachment.filename "
            + "from XWikiDocument doc, XWikiAttachment attachment, BaseObject as obj "
            + "where doc.id = attachment.docId and "
            + "(doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers')", result);
    }

    @Test
    public void filterStatementNonHQL() throws Exception
    {
        String statement = "select doc.fullName from XWikiDocument doc ...";
        assertSame(statement, this.mocker.getComponentUnderTest().filterStatement(statement, Query.XWQL));
    }

    @Test
    public void filterStatementThatDoesNotMatch() throws Exception
    {
        String statement = "one two three";
        assertSame(statement, this.mocker.getComponentUnderTest().filterStatement(statement, Query.HQL));

        statement = "select space.reference from XWikiSpace space ...";
        assertSame(statement, this.mocker.getComponentUnderTest().filterStatement(statement, Query.HQL));
    }

    @Test
    public void filterResults() throws Exception
    {
        List<Object[]> results = new ArrayList<>();
        results.add(new Object[] {"A.B", "image.png"});

        DocumentReferenceResolver<String> currentDocumentReferenceResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        DocumentReference documentReference = new DocumentReference("wiki", "A", "B");
        when(currentDocumentReferenceResolver.resolve("A.B")).thenReturn(documentReference);

        List<AttachmentReference> attachmentReferences = this.mocker.getComponentUnderTest().filterResults(results);
        AttachmentReference expectedAttachmentReference = new AttachmentReference("image.png", documentReference);
        assertEquals(Collections.singletonList(expectedAttachmentReference), attachmentReferences);
    }

    @Test
    public void filterResultsWithOneColumn() throws Exception
    {
        List<Object> results = Arrays.asList(13, 27);
        assertSame(results, this.mocker.getComponentUnderTest().filterResults(results));
    }
}
