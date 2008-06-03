package org.xwiki.velocity;

import org.apache.velocity.VelocityContext;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextInitializerException;

/**
 * Allow registering the Velocity Context in the Execution Context object since it's shared during
 * the whole execution of the current request.
 *
 * @see org.xwiki.context.ExecutionContextInitializer
 * @since 1.5M1
 * @version $Id: $
 */
public class VelocityExecutionContextInitializer implements ExecutionContextInitializer
{
    /**
     * The id under wich the Velocity Context is stored in the Execution Context.
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
     * @see org.xwiki.context.ExecutionContextInitializer#initialize(org.xwiki.context.ExecutionContext)
     */
    public void initialize(ExecutionContext executionContext)
        throws ExecutionContextInitializerException
    {
        try {
            VelocityContext context = this.velocityContextFactory.createContext();
            executionContext.setProperty(
                VelocityExecutionContextInitializer.REQUEST_VELOCITY_CONTEXT, context);
        } catch (XWikiVelocityException e) {
            throw new ExecutionContextInitializerException("Failed to initialize Velocity Context",
                e);
        }
    }
}
