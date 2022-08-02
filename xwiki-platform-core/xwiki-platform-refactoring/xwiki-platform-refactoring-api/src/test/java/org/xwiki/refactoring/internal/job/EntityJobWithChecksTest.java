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

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AbstractEntityJobWithChecks}.
 *
 * @version $Id$
 */
@ComponentTest
class EntityJobWithChecksTest
{
    // This class needs to remain public so that the test framework can find it.
    public static class MockAbstractEntityJobWithChecks
        extends AbstractEntityJobWithChecks<EntityRequest, EntityJobStatus<EntityRequest>>
    {
        @Override
        protected void process(EntityReference entityReference)
        {
            // do nothing
        }

        @Override
        public String getType()
        {
            return "test";
        }
    }

    @InjectMockComponents
    private MockAbstractEntityJobWithChecks abstractEntityJobWithChecks;

    @Mock
    private EntityRequest request;

    private DocumentReference documentReference;

    @BeforeEach
    void setup(MockitoComponentManager componentManager) throws ComponentLookupException
    {
        EntityReferenceProvider entityReferenceProvider = componentManager.getInstance(EntityReferenceProvider.class);

        when(entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
        abstractEntityJobWithChecks.initialize(this.request);
        this.documentReference = new DocumentReference("wiki", "space", "doc");
    }

    @Test
    void getDocumentEntities_deepfalse()
    {
        // request deep is false so we don't visit the space
        when(request.isDeep()).thenReturn(false);
        abstractEntityJobWithChecks.getEntities(documentReference);

        assertEquals(new EntitySelection(documentReference),
            abstractEntityJobWithChecks.getConcernedEntitiesEntitySelection(documentReference));
    }


    @Test
    void getDocumentEntities_deepTrueTerminal()
    {
        // request deep is true, but isSpaceHomeReference will return false, so we still don't visit the space
        when(request.isDeep()).thenReturn(true);
        abstractEntityJobWithChecks.getEntities(documentReference);
        assertEquals(new EntitySelection(documentReference),
            abstractEntityJobWithChecks.getConcernedEntitiesEntitySelection(documentReference));
    }

    @Test
    void getDocumentEntities_deepTrueTerminalRootLocale()
    {
        when(request.isDeep()).thenReturn(true);
        // Ensure that the root locale is removed
        DocumentReference documentReferenceWithLocale = new DocumentReference(documentReference, Locale.ROOT);
        abstractEntityJobWithChecks.getEntities(documentReferenceWithLocale);
        assertEquals(new EntitySelection(documentReference),
            abstractEntityJobWithChecks.getConcernedEntitiesEntitySelection(documentReferenceWithLocale));
    }

    @Test
    void getDocumentEntities_deepTrueTerminalRootNotLocale()
    {
        // Ensure that any other locale remains
        DocumentReference documentReferenceWithLocale = new DocumentReference(documentReference, Locale.FRANCE);
        abstractEntityJobWithChecks.getEntities(documentReferenceWithLocale);

        assertEquals(new EntitySelection(documentReferenceWithLocale),
            abstractEntityJobWithChecks.getConcernedEntitiesEntitySelection(documentReferenceWithLocale));
    }
}
