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
package org.xwiki.refactoring.internal.job;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.job.ReplaceUserRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReplaceUserJob}.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@ComponentTest
public class ReplaceUserJobTest
{
    @InjectMockComponents
    private ReplaceUserJob replaceUserJob;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument document;

    private DocumentReference alice = new DocumentReference("test", "Users", "Alice");

    private DocumentReference bob = new DocumentReference("test", "Users", "Bob");

    private ReplaceUserRequest request = new ReplaceUserRequest();

    @BeforeEach
    public void configure() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        DocumentReference documentReference = new DocumentReference("test", "Some", "Page");
        when(this.document.getDocumentReference()).thenReturn(documentReference);
        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(this.document);

        this.request.setOldUserReference(this.alice);
        this.request.setNewUserReference(this.bob);
        this.replaceUserJob.initialize(this.request);
    }

    @Test
    public void updateCreator() throws Exception
    {
        this.request.setReplaceDocumentCreator(true);
        // Verify this doesn't have any effect if the document author doesn't match the request author.
        this.request.setReplaceDocumentAuthor(true);

        // Make sure the document creator matches the request creator.
        when(this.document.getCreatorReference()).thenReturn(this.alice);

        this.replaceUserJob.update(this.document.getDocumentReference());

        verify(this.document).setCreatorReference(this.bob);
        verify(this.xwiki).saveDocument(this.document,
            "Replaced user [test:Users.Alice] in fields [creator] to user [test:Users.Bob]", this.xcontext);
    }

    @Test
    public void updateAuthor() throws Exception
    {
        this.request.setReplaceDocumentAuthor(true);
        this.request.setReplaceDocumentContentAuthor(true);

        // Make sure the document author matches the request creator.
        when(this.document.getAuthorReference()).thenReturn(alice);
        when(this.document.getContentAuthorReference()).thenReturn(alice);

        this.replaceUserJob.update(this.document.getDocumentReference());

        verify(this.document).setAuthorReference(bob);
        verify(this.document).setContentAuthorReference(bob);
        verify(this.xwiki).saveDocument(this.document,
            "Replaced user [test:Users.Alice] in fields [author, contentAuthor] to user [test:Users.Bob]",
            this.xcontext);
    }
}
