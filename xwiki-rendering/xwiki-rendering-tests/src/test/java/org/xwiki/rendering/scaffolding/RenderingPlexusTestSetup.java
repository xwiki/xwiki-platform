package org.xwiki.rendering.scaffolding;


import junit.framework.TestSuite;

import com.xpn.xwiki.test.PlexusTestSetup;

public class RenderingPlexusTestSetup extends PlexusTestSetup
{
    public RenderingPlexusTestSetup(TestSuite suite)
    {
        super(suite);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        getComponentManager().registerComponentDescriptor(MockDocumentAccessBridge.getComponentDescriptor());
        getComponentManager().registerComponentDescriptor(MockIdGenerator.getComponentDescriptor());
        getComponentManager().registerComponentDescriptor(MockConfigurationSourceCollection.getComponentDescriptor());
    }
}
