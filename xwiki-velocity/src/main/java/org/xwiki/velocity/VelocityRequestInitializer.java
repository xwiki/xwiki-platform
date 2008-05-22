package org.xwiki.velocity;

import org.apache.velocity.VelocityContext;
import org.xwiki.container.Request;
import org.xwiki.container.RequestInitializer;

public class VelocityRequestInitializer implements RequestInitializer
{
    public static final String REQUEST_VELOCITY_CONTEXT = "velocityContext";

    private VelocityContextFactory velocityContextFactory;
    
    public void initialize(Request request)
    {
        VelocityContext context = this.velocityContextFactory.createContext();
        request.setProperty(VelocityRequestInitializer.REQUEST_VELOCITY_CONTEXT, context);
    }
}
