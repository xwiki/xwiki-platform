package org.xwiki.cache.jcache.internal;

import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.ExpiryPolicy;

public class XWikiConfiguration<T> extends MutableConfiguration<String, T>
{
    private final String name;

    private ExpiryPolicy expiryPolicy;

    public XWikiConfiguration(String name)
    {
        this.name = name;

        setExpiryPolicyFactory(new Factory<ExpiryPolicy>()
        {
            @Override
            public ExpiryPolicy create()
            {
                return XWikiConfiguration.this.expiryPolicy;
            }
        });
        setManagementEnabled(true);
        setStatisticsEnabled(true);
        setStoreByValue(false);
    }

    public String getName()
    {
        return this.name;
    }

    public void setExpiryPolicy(ExpiryPolicy expiryPolicy)
    {
        this.expiryPolicy = expiryPolicy;
    }
}
