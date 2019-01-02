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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.job.JobContext;
import org.xwiki.job.JobGroupPath;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.job.AbstractEntityJob.Visitor;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractEntityJob}.
 * 
 * @version $Id$
 */
public class EntityJobTest
{
    private static class NoopEntityJob extends AbstractEntityJob<EntityRequest, EntityJobStatus<EntityRequest>>
    {
        @Override
        public String getType()
        {
            return null;
        }

        @Override
        public void run()
        {
            try {
                super.runInternal();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void process(EntityReference entityReference)
        {
            // No operation.
        }

        @Override
        public void visitDocuments(SpaceReference spaceReference, Visitor<DocumentReference> visitor)
        {
            super.visitDocuments(spaceReference, visitor);
        }

        @Override
        public boolean hasAccess(Right right, EntityReference reference)
        {
            return super.hasAccess(right, reference);
        }
    }

    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    private AuthorizationManager authorization = mock(AuthorizationManager.class);

    private ModelBridge modelBridge = mock(ModelBridge.class);

    private void initialize(NoopEntityJob job, EntityRequest request)
    {
        ReflectionUtils.setFieldValue(job, "jobContext", mock(JobContext.class));
        ReflectionUtils.setFieldValue(job, "progressManager", mock(JobProgressManager.class));
        ReflectionUtils.setFieldValue(job, "authorization", this.authorization);
        ReflectionUtils.setFieldValue(job, "modelBridge", this.modelBridge);

        EntityReferenceProvider defaultEntityReferenceProvider = mock(EntityReferenceProvider.class);
        ReflectionUtils.setFieldValue(job, "defaultEntityReferenceProvider", defaultEntityReferenceProvider);
        when(defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));

        job.initialize(request);
    }

    @Test
    public void getGroupPath()
    {
        NoopEntityJob job = new NoopEntityJob();
        EntityRequest request = new EntityRequest();

        DocumentReference aliceReference = new DocumentReference("chess", Arrays.asList("Path", "To"), "Alice");
        request.setEntityReferences(aliceReference.getReversedReferenceChain());
        initialize(job, request);
        assertEquals(new JobGroupPath(Arrays.asList("refactoring", "chess")), job.getGroupPath());

        DocumentReference bobReference = new DocumentReference("dev", Arrays.asList("Path", "To"), "Bob");
        request.setEntityReferences(Arrays.<EntityReference>asList(aliceReference, bobReference));
        initialize(job, request);
        assertEquals(new JobGroupPath(Arrays.asList("refactoring")), job.getGroupPath());

        DocumentReference carolReference = new DocumentReference("chess", Arrays.asList("Path", "To"), "Carol");
        request.setEntityReferences(Arrays.<EntityReference>asList(aliceReference, carolReference));
        initialize(job, request);
        assertEquals(new JobGroupPath(Arrays.asList("refactoring", "chess", "Path", "To")), job.getGroupPath());

        DocumentReference daveReference = new DocumentReference("chess", Arrays.asList("Path", "To2"), "Dave");
        request.setEntityReferences(Arrays.<EntityReference>asList(aliceReference, carolReference, daveReference));
        initialize(job, request);
        assertEquals(new JobGroupPath(Arrays.asList("refactoring", "chess", "Path")), job.getGroupPath());
    }

    @Test
    public void processEachEntity()
    {
        final List<EntityReference> references = new ArrayList<>();
        NoopEntityJob job = new NoopEntityJob()
        {
            @Override
            protected void process(EntityReference entityReference)
            {
                references.add(entityReference);
            }
        };

        DocumentReference documentReference = new DocumentReference("chess", Arrays.asList("Path", "To"), "Success");
        EntityRequest request = new EntityRequest();
        request.setEntityReferences(documentReference.getReversedReferenceChain());
        initialize(job, request);
        job.run();
        assertEquals(request.getEntityReferences(), references);
    }

    @Test
    public void visitDocuments()
    {
        DocumentReference alice = new DocumentReference("foo", "Alice", "WebHome");
        DocumentReference alicePrefs = new DocumentReference("WebPreferences", alice.getLastSpaceReference());
        DocumentReference aliceBio = new DocumentReference("Bio", alice.getLastSpaceReference());

        DocumentReference bob = new DocumentReference("foo", Arrays.asList("Alice", "Bob"), "WebHome");
        DocumentReference bobPrefs = new DocumentReference("WebPreferences", bob.getLastSpaceReference());
        DocumentReference bobBio = new DocumentReference("Bio", bob.getLastSpaceReference());

        DocumentReference carolBio = new DocumentReference("bar", Arrays.asList("Users", "Carol"), "Bio");

        SpaceReference spaceReference = mock(SpaceReference.class);
        when(this.modelBridge.getDocumentReferences(spaceReference))
            .thenReturn(Arrays.asList(alice, alicePrefs, aliceBio, bob, bobPrefs, bobBio, carolBio));

        NoopEntityJob job = new NoopEntityJob();
        initialize(job, new EntityRequest());

        final List<DocumentReference> documentReferences = new ArrayList<>();
        job.visitDocuments(spaceReference, new Visitor<DocumentReference>()
        {
            @Override
            public void visit(DocumentReference documentReference)
            {
                documentReferences.add(documentReference);
            }
        });
        // Space preferences documents are handled after their siblings.
        assertEquals(Arrays.asList(carolBio, aliceBio, bobBio, bob, bobPrefs, alice, alicePrefs), documentReferences);
    }

    @Test
    public void hasAccess()
    {
        EntityRequest request = mock(EntityRequest.class);
        when(request.isCheckRights()).thenReturn(true, true, false);

        DocumentReference userReference = new DocumentReference("foo", "Users", "Alice");
        when(request.getUserReference()).thenReturn(userReference);
        when(request.getAuthorReference()).thenReturn(userReference);

        DocumentReference documentReference = new DocumentReference("math", "Space", "Page");
        when(this.authorization.hasAccess(Right.EDIT, userReference, documentReference.getLastSpaceReference()))
            .thenReturn(true);
        when(this.authorization.hasAccess(Right.DELETE, userReference, documentReference)).thenReturn(false);

        NoopEntityJob job = new NoopEntityJob();
        initialize(job, request);

        // checkRights = true
        assertTrue(job.hasAccess(Right.EDIT, documentReference.getLastSpaceReference()));
        assertFalse(job.hasAccess(Right.DELETE, documentReference));

        // checkRights = false
        assertTrue(job.hasAccess(Right.DELETE, documentReference));
    }
}
