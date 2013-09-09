package org.xwiki.wikistream.type;

import org.junit.Assert;
import org.junit.Test;

/**
 * Validate {@link WikiStreamType}.
 * 
 * @version $Id$
 */
public class WikiStreamTypeTest
{
    // Tests

    @Test
    public void testSerializeWithDataAndVersion()
    {
        WikiStreamType type = new WikiStreamType(new WikiType("type"), "data", "version");

        Assert.assertEquals("type+data/version", type.serialize());
    }

    @Test
    public void testUnserializeWithDataAndVersion()
    {
        WikiStreamType type = WikiStreamType.unserialize("type+data/version");

        Assert.assertEquals("type", type.getType().getId());
        Assert.assertEquals("data", type.getDataFormat());
        Assert.assertEquals("version", type.getVersion());
    }

    @Test
    public void testUnserializeWithData()
    {
        WikiStreamType type = WikiStreamType.unserialize("type+data");

        Assert.assertEquals("type", type.getType().getId());
        Assert.assertEquals("data", type.getDataFormat());
        Assert.assertNull(type.getVersion());
    }

    @Test
    public void testUnserializeWithEmptyData()
    {
        WikiStreamType type = WikiStreamType.unserialize("type+");

        Assert.assertEquals("type", type.getType().getId());
        Assert.assertEquals("", type.getDataFormat());
        Assert.assertNull(type.getVersion());
    }

    @Test
    public void testUnserializeWithVersion()
    {
        WikiStreamType type = WikiStreamType.unserialize("type/version");

        Assert.assertEquals("type", type.getType().getId());
        Assert.assertNull(type.getDataFormat());
        Assert.assertEquals("version", type.getVersion());
    }

    @Test
    public void testUnserializeWithEmptyVersion()
    {
        WikiStreamType type = WikiStreamType.unserialize("type/");

        Assert.assertEquals("type", type.getType().getId());
        Assert.assertNull(type.getDataFormat());
        Assert.assertEquals("", type.getVersion());
    }
}
