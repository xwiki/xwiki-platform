package org.xwiki.cache.jbosscache;

import org.xwiki.cache.tests.AbstractGenericTestCache;

public class JBossCacheLocalCacheTest extends AbstractGenericTestCache
{
    public JBossCacheLocalCacheTest()
    {
        this("jbosscache/local");
    }

    protected JBossCacheLocalCacheTest(String roleHint)
    {
        super(roleHint);
    }
}
