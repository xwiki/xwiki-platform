package org.xwiki.velocity;

import org.apache.velocity.VelocityContext;
import org.xwiki.container.Request;
import org.xwiki.container.RequestInitializer;

/**
 * Allow registering the Velocity Context in the XWiki Request object since it's shared during the
 * whole request.
 *
 * @See RequestInitializerManager
 * @see RequestInitializer
 */
public class VelocityRequestInitializer implements RequestInitializer
{
    /**
     * The id under wich the Velocity Context is stored in the Request.
     */
    public static final String REQUEST_VELOCITY_CONTEXT = "velocityContext";

    /**
     * The Velocity context factory component used for creating the Velocity Context (injected
     * automatically by the Component subsystem).
     */
    private VelocityContextFactory velocityContextFactory;

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.container.RequestInitializer#initialize(org.xwiki.container.Request)
     */
    public void initialize(Request request)
    {
        VelocityContext context = this.velocityContextFactory.createContext();
        request.setProperty(VelocityRequestInitializer.REQUEST_VELOCITY_CONTEXT, context);
    }
}
