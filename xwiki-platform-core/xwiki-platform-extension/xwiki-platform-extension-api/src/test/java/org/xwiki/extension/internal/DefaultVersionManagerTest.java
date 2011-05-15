package org.xwiki.extension.internal;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class DefaultVersionManagerTest
{
    private VersionManager versionManager;
    
    @Before
    public void setUp() throws Exception
    {
        this.versionManager = new DefaultVersionManager();
    }
    
    @Test
    public void testWithIntegers()
    {
        Assert.assertEquals(1, versionManager.compareVersions("1.1", "1.0"));
        Assert.assertEquals(8, versionManager.compareVersions("1.10", "1.2"));
    }
    
    @Test
    public void testWithStrings()
    {
        Assert.assertEquals(8, versionManager.compareVersions("1.10-sometext", "1.2"));
        Assert.assertEquals(1, versionManager.compareVersions("1.1-sometext", "1.1"));
        Assert.assertEquals(67, versionManager.compareVersions("1.sometext", "1.0"));
    }
}
