package com.xpn.xwiki.util;

import junit.framework.Assert;

import org.junit.Test;

public class TOCGeneratorTest
{
    @Test
    public void testMakeHeadingID()
    {
        Assert.assertEquals("Hheader", TOCGenerator.makeHeadingID("header", 0, null));
        Assert.assertEquals("Hheaderwithspace", TOCGenerator.makeHeadingID("header with space", 0, null));
        Assert.assertEquals("Hheader-1", TOCGenerator.makeHeadingID("header", 1, null));
    }
}
