package org.xwiki.rendering.scaffolding;

import org.xwiki.rendering.internal.MockDocumentAccessBridge;
import org.xwiki.rendering.internal.configuration.MockConfigurationSourceCollection;
import org.xwiki.rendering.internal.util.MockIdGenerator;

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
