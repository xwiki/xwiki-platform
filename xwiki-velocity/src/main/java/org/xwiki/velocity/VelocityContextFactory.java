package org.xwiki.velocity;

import org.apache.velocity.VelocityContext;

public interface VelocityContextFactory
{
    /**
     * This component's role, used when code needs to look it up.
     */
    public final static String ROLE = VelocityContextFactory.class.getName();

    VelocityContext createContext();
}
