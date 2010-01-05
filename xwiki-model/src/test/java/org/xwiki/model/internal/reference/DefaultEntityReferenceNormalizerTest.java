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
package org.xwiki.model.internal.reference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceNormalizer;
import org.xwiki.model.reference.InvalidEntityReferenceException;

/**
 * Unit tests for {@link DefaultEntityReferenceNormalizer}.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class DefaultEntityReferenceNormalizerTest
{
    private EntityReferenceNormalizer normalizer;

    private ModelConfiguration mockModelConfiguration;

    private Mockery mockery = new Mockery();

    @Before
    public void setUp()
    {
        this.normalizer = new DefaultEntityReferenceNormalizer();
        this.mockModelConfiguration = this.mockery.mock(ModelConfiguration.class);
        ReflectionUtils.setFieldValue(this.normalizer, "configuration", this.mockModelConfiguration);

        this.mockery.checking(new Expectations() {{
            allowing(mockModelConfiguration).getDefaultReferenceName(EntityType.SPACE); will(returnValue("defspace"));
            allowing(mockModelConfiguration).getDefaultReferenceName(EntityType.WIKI); will(returnValue("defwiki"));
            allowing(mockModelConfiguration).getDefaultReferenceName(EntityType.DOCUMENT); will(returnValue("defpage"));
        }});
    }

    @Test
    public void testNormalizeDocumentReferenceWhenMissingParents()
    {
        EntityReference reference = new EntityReference("page", EntityType.DOCUMENT);
        normalizer.normalize(reference);
        Assert.assertEquals("defspace", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("defwiki", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType()); 
    }

    @Test
    public void testNormalizeAttachmentReferenceWhenMissingParents()
    {
        EntityReference reference = new EntityReference("filename", EntityType.ATTACHMENT);
        normalizer.normalize(reference);
        Assert.assertEquals("defpage", reference.getParent().getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        Assert.assertEquals("defspace", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        Assert.assertEquals("defwiki", reference.getParent().getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @Test
    public void testNormalizeDocumentReferenceWhenThereAreNullNames()
    {
        EntityReference reference = new EntityReference(null, EntityType.DOCUMENT);
        normalizer.normalize(reference);
        Assert.assertEquals("defpage", reference.getName());
        Assert.assertEquals("defspace", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("defwiki", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    }

    @Test
    public void testNormalizeDocumentReferenceWhenMissingParentBetweenReferences()
    {
        EntityReference reference = new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("wiki", EntityType.WIKI));
        normalizer.normalize(reference);
        Assert.assertEquals("defspace", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("wiki", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    }

    @Test
    public void testNormalizeAttachmentReferenceWhenMissingParentBetweenReferences()
    {
        EntityReference reference = new EntityReference("filename", EntityType.ATTACHMENT,
            new EntityReference("wiki", EntityType.WIKI));
        normalizer.normalize(reference);
        Assert.assertEquals("defpage", reference.getParent().getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        Assert.assertEquals("defspace", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        Assert.assertEquals("wiki", reference.getParent().getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @Test
    public void testNormalizeWhenInvalidReference()
    {
        EntityReference reference = new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("filename", EntityType.ATTACHMENT));
        try {
            normalizer.normalize(reference);
            Assert.fail("Should have thrown an exception here");
        } catch (InvalidEntityReferenceException expected) {
            Assert.assertEquals("Invalid reference [name = [page], type = [DOCUMENT], parent = [name = [defspace], "
                + "type = [SPACE], parent = [name = [defwiki], type = [WIKI], parent = [name = [filename], "
                + "type = [ATTACHMENT], parent = [null]]]]]", expected.getMessage());            
        }
        
    }
}