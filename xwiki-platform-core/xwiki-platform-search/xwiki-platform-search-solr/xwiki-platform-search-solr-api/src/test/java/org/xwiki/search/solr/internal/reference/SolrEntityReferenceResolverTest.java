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

import java.util.List;
import java.util.Locale;

import javax.inject.Named;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Unit tests for {@link SolrEntityReferenceResolver}.
 *
 * @version $Id$
 */
@ComponentTest
class SolrEntityReferenceResolverTest
{
    @InjectMockComponents
    private SolrEntityReferenceResolver resolver;

    @MockComponent
    @Named("explicit")
    private EntityReferenceResolver<EntityReference> explicitReferenceEntityReferenceResolver;

    private SolrDocument solrDocument;

    @BeforeEach
    void configure()
    {
        this.solrDocument = new SolrDocument();
        this.solrDocument.setField(FieldUtils.WIKI, "chess");
        this.solrDocument.setField(FieldUtils.SPACES, List.of("Path", "To", "Success"));
        this.solrDocument.setField(FieldUtils.NAME, "WebHome");
        this.solrDocument.setField(FieldUtils.DOCUMENT_LOCALE, "fr");
        // The file name field can have multiple values.
        this.solrDocument.addField(FieldUtils.FILENAME, "image.png");
        this.solrDocument.addField(FieldUtils.FILENAME, "presentation.odp");
        // The class name field can have multiple values too.
        this.solrDocument.addField(FieldUtils.CLASS, "App.Code.PlayerClass");
        this.solrDocument.addField(FieldUtils.CLASS, "App.Code.TrainerClass");
        this.solrDocument.setField(FieldUtils.NUMBER, 13);
        this.solrDocument.setField(FieldUtils.PROPERTY_NAME, "age");

        doAnswer(invocation -> {
            EntityReference reference = invocation.getArgument(0);
            EntityType type = invocation.getArgument(1);
            return reference.extractReference(type);
        }).when(this.explicitReferenceEntityReferenceResolver).resolve(any(EntityReference.class),
            any(EntityType.class));
    }

    @Test
    void resolve()
    {
        WikiReference wikiReference = new WikiReference("chess");
        assertReference(wikiReference);

        assertReference(new SpaceReference("Success", new SpaceReference("To",
            new SpaceReference("Path", wikiReference))));

        DocumentReference documentReference =
            new DocumentReference("chess", List.of("Path", "To", "Success"), "WebHome", Locale.FRENCH);
        assertReference(documentReference);

        assertReference(new AttachmentReference("image.png", documentReference));

        ObjectReference objectReference = new ObjectReference("App.Code.PlayerClass[13]", documentReference);
        assertReference(objectReference);

        assertReference(new ObjectPropertyReference("age", objectReference));
    }

    private void assertReference(EntityReference reference)
    {
        this.solrDocument.setField(FieldUtils.TYPE, reference.getType().name());
        assertEquals(reference, this.resolver.resolve(this.solrDocument, reference.getType()));
    }
}
