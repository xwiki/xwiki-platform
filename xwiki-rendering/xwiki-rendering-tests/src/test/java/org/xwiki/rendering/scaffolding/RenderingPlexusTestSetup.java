package org.xwiki.rendering.scaffolding;

import org.xwiki.test.PlexusTestSetup;

import junit.framework.TestSuite;

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

        getComponentManager().registerComponent(MockDocumentAccessBridge.getComponentDescriptor());
        getComponentManager().registerComponent(MockConfigurationSourceCollection.getComponentDescriptor());
        getComponentManager().registerComponent(MockDocumentNameSerializer.getComponentDescriptor());
    }
}
