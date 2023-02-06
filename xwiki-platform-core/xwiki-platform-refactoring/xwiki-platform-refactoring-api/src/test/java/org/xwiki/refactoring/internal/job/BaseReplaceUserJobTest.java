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

import java.util.Arrays;
import java.util.Locale;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.job.ReplaceUserRequest;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractReplaceUserJob}.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@ComponentList(value = BaseReplaceUserJob.class)
@ComponentTest
public class BaseReplaceUserJobTest
{
    @InjectMockComponents
    private BaseReplaceUserJob replaceUserJob;

    @MockComponent
    private AuthorizationManager authorization;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private DocumentReference alice = new DocumentReference("test", "Users", "Alice");

    private DocumentReference bob = new DocumentReference("test", "Users", "Bob");

    private DocumentReference carol = new DocumentReference("test", "Users", "Carol");

    private ReplaceUserRequest request = new ReplaceUserRequest();

    @BeforeEach
    public void configure()
    {
        this.request.setUserReference(this.carol);
        this.request.setAuthorReference(this.carol);
        this.request.setOldUserReference(this.alice);
        this.request.setNewUserReference(this.bob);
        this.replaceUserJob.initialize(this.request);
    }

    @Test
    public void emptyRequest()
    {
        this.replaceUserJob.run();

        assertTrue(this.replaceUserJob.updatedDocuments.isEmpty());
        assertEquals("Starting job of type [refactoring/replaceUser]", this.logCapture.getMessage(0));
        assertEquals("Finished job of type [refactoring/replaceUser]", logCapture.getMessage(1));
    }

    @Test
    public void replaceUser() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("test", "Space", "WebHome");
        this.request.setEntityReferences(
            Arrays.asList(documentReference.getWikiReference(), documentReference, documentReference.getParent()));
        this.request.setReplaceDocumentAuthor(true);
        this.request.setReplaceDocumentContentAuthor(true);
        this.request.setReplaceDocumentCreator(true);

        when(this.authorization.hasAccess(Right.ADMIN, this.carol, documentReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.ADMIN, this.carol, documentReference.getParent())).thenReturn(true);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery("select doc.fullName, doc.language from XWikiDocument as doc "
            + "where (doc.author = :oldUser OR doc.contentAuthor = :oldUser OR doc.creator = :oldUser)"
            + " AND doc.space = :space", Query.HQL)).thenReturn(query);

        when(this.compactWikiEntityReferenceSerializer.serialize(this.alice, documentReference.getParent()))
            .thenReturn("Users.Alice");
        when(this.compactWikiEntityReferenceSerializer.serialize(documentReference.getParent(),
            documentReference.getParent())).thenReturn("Space");

        DocumentReference updatedDocRef = new DocumentReference("test", "Some", "Page");
        when(query.<Object[]>execute()).thenReturn(Arrays.<Object[]>asList(new Object[] {"Some.Page", "fr"}));
        when(this.documentReferenceResolver.resolve("Some.Page", documentReference.getParent()))
            .thenReturn(updatedDocRef);

        this.replaceUserJob.run();

        verify(query).setWiki("test");
        verify(query).bindValue("oldUser", "Users.Alice");
        verify(query).bindValue("space", "Space");

        assertEquals("Starting job of type [refactoring/replaceUser]", this.logCapture.getMessage(0));
        assertEquals("You need administration right on [Wiki test] in order to be able to replace the user.",
            this.logCapture.getMessage(1));
        assertEquals("Skipping unsupported entity [test:Space.WebHome].", this.logCapture.getMessage(2));
        assertEquals("Updating documents from [Space test:Space].", this.logCapture.getMessage(3));
        assertEquals("Updating document [test:Some.Page(fr)].", this.logCapture.getMessage(4));
        assertEquals("Finished job of type [refactoring/replaceUser]", logCapture.getMessage(5));

        assertEquals(Arrays.asList(new DocumentReference(updatedDocRef, Locale.FRENCH)),
            this.replaceUserJob.updatedDocuments);
    }
}
