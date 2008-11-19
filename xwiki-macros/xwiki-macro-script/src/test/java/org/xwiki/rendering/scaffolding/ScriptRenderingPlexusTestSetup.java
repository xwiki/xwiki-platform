package org.xwiki.rendering.scaffolding;

import junit.framework.TestSuite;

import org.xwiki.rendering.internal.macro.MockScriptContextManager;

public class ScriptRenderingPlexusTestSetup extends RenderingPlexusTestSetup
{
    public ScriptRenderingPlexusTestSetup(TestSuite suite)
    {
        super(suite);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.scaffolding.RenderingPlexusTestSetup#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        getComponentManager().registerComponentDescriptor(MockScriptContextManager.getComponentDescriptor());
    }
}
