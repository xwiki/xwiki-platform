package com.xpn.xwiki.objects;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Validate {@link BaseObjectReference}.
 * 
 * @version $Id$
 */
public class BaseObjectReferenceTest extends AbstractBridgedComponentTestCase
{
    private DocumentReference document;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.document = new DocumentReference("wiki", "space", "page");
    }

    @Test
    public void testSerialize() throws Exception
    {
        BaseObjectReference reference =
            new BaseObjectReference(new DocumentReference("wiki", "space", "class"), 42, this.document);

        Assert.assertEquals("wiki:space.class[42]", reference.getName());

        reference = new BaseObjectReference(new DocumentReference("wiki", "space", "class"), null, this.document);

        Assert.assertEquals("wiki:space.class", reference.getName());
    }

    @Test
    public void testSerializeEscape() throws Exception
    {
        BaseObjectReference reference =
            new BaseObjectReference(new DocumentReference("wiki", "space", "class[42]"), null, this.document);

        Assert.assertEquals("wiki:space.class\\[42]", reference.getName());

        reference =
            new BaseObjectReference(new DocumentReference("wiki", "space", "class\\\\[42]"), null, this.document);

        Assert.assertEquals("wiki:space.class\\\\\\\\\\[42]", reference.getName());
    }

    @Test
    public void testUnserialize() throws Exception
    {
        BaseObjectReference reference =
            new BaseObjectReference(new EntityReference("wiki:space.class[42]", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class"), reference.getXClassReference());
        Assert.assertEquals((int) 42, (int) reference.getObjectNumber());

        reference = new BaseObjectReference(new EntityReference("wiki:space.class", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class"), reference.getXClassReference());
        Assert.assertNull(reference.getObjectNumber());
    }

    @Test
    public void testUnserializeEscape() throws Exception
    {
        BaseObjectReference reference =
            new BaseObjectReference(new EntityReference("wiki:space.class\\[42]", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class[42]"), reference.getXClassReference());
        Assert.assertNull(reference.getObjectNumber());

        reference =
            new BaseObjectReference(new EntityReference("wiki:space.class\\\\[42]", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class\\"), reference.getXClassReference());
        Assert.assertEquals((int) 42, (int) reference.getObjectNumber());

        reference =
            new BaseObjectReference(new EntityReference("wiki:space.class\\\\\\[42]", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class\\[42]"), reference.getXClassReference());
        Assert.assertNull(reference.getObjectNumber());

        reference =
            new BaseObjectReference(new EntityReference("wiki:space.class[word]", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class[word]"), reference.getXClassReference());
        Assert.assertNull(reference.getObjectNumber());
    }
}
