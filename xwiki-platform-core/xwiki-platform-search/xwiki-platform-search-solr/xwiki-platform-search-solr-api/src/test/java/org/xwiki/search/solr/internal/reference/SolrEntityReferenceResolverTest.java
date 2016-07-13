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
package org.xwiki.search.solr.internal.reference;

import java.util.Arrays;
import java.util.Locale;

import org.apache.solr.common.SolrDocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SolrEntityReferenceResolver}.
 * 
 * @version $Id$
 */
public class SolrEntityReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<EntityReferenceResolver<SolrDocument>> mocker =
        new MockitoComponentMockingRule<EntityReferenceResolver<SolrDocument>>(SolrEntityReferenceResolver.class);

    private SolrDocument solrDocument;

    @Before
    public void configure() throws Exception
    {
        solrDocument = new SolrDocument();
        solrDocument.setField(FieldUtils.WIKI, "chess");
        solrDocument.setField(FieldUtils.SPACES, Arrays.asList("Path", "To", "Success"));
        solrDocument.setField(FieldUtils.NAME, "WebHome");
        solrDocument.setField(FieldUtils.DOCUMENT_LOCALE, "fr");
        // The file name field can have multiple values.
        solrDocument.addField(FieldUtils.FILENAME, "image.png");
        solrDocument.addField(FieldUtils.FILENAME, "presentation.odp");
        // The class name field can have multiple values too.
        solrDocument.addField(FieldUtils.CLASS, "App.Code.PlayerClass");
        solrDocument.addField(FieldUtils.CLASS, "App.Code.TrainerClass");
        solrDocument.setField(FieldUtils.NUMBER, 13);
        solrDocument.setField(FieldUtils.PROPERTY_NAME, "age");

        EntityReferenceResolver<EntityReference> explicitReferenceEntityReferenceResolver =
            this.mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "explicit");
        doAnswer(new Answer<EntityReference>()
        {
            @Override
            public EntityReference answer(InvocationOnMock invocation) throws Throwable
            {
                EntityReference reference = invocation.getArgumentAt(0, EntityReference.class);
                EntityType type = invocation.getArgumentAt(1, EntityType.class);
                return reference.extractReference(type);
            }
        }).when(explicitReferenceEntityReferenceResolver).resolve(any(EntityReference.class), any(EntityType.class));
    }

    @Test
    public void resolve() throws Exception
    {
        WikiReference wikiReference = new WikiReference("chess");
        assertReference(wikiReference);

        assertReference(new SpaceReference("Success", new SpaceReference("To",
            new SpaceReference("Path", wikiReference))));

        DocumentReference documentReference =
            new DocumentReference("chess", Arrays.asList("Path", "To", "Success"), "WebHome", Locale.FRENCH);
        assertReference(documentReference);

        assertReference(new AttachmentReference("image.png", documentReference));

        ObjectReference objectReference = new ObjectReference("App.Code.PlayerClass[13]", documentReference);
        assertReference(objectReference);

        assertReference(new ObjectPropertyReference("age", objectReference));
    }

    private void assertReference(EntityReference reference) throws Exception
    {
        solrDocument.setField(FieldUtils.TYPE, reference.getType().name());
        assertEquals(reference, this.mocker.getComponentUnderTest().resolve(solrDocument, reference.getType()));
    }
}
