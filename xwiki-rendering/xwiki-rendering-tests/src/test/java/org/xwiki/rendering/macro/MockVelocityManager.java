package org.xwiki.rendering.macro;

import org.apache.velocity.VelocityContext;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentLookupException;

import java.util.Properties;

/**
 * Mock VelocityManager implementation used for testing, since we don't want to pull any dependency
 * on the Model/Skin/etc for the Rendering module's unit tests.
 */
public class MockVelocityManager implements VelocityManager, Composable
{
    private ComponentManager componentManager;

    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    public VelocityContext getVelocityContext()
    {
        return new VelocityContext();
    }

    public VelocityEngine getVelocityEngine() throws XWikiVelocityException
    {
        VelocityEngine engine;
        try {
            engine = (VelocityEngine) this.componentManager.lookup(VelocityEngine.ROLE);
        } catch (ComponentLookupException e) {
            throw new XWikiVelocityException("Failed to look up Velocity Engine", e);
        }

        // Configure the Velocity Engine not to use the Resource Webadd Loader since we don't
        // need it and we would need to setup the Container component's ApplicationContext
        // otherwise.
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "file");
        engine.initialize(properties);

        return engine;
    }
}
