/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.xwiki.velocity.internal;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.velocity.VelocityContextFactory;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Allow registering the Velocity Context in the Execution Context object since it's shared during the whole execution
 * of the current request.
 * 
 * @see org.xwiki.context.ExecutionContextInitializer
 * @since 1.5M1
 * @version $Id$
 */
@Component("velocity")
public class VelocityExecutionContextInitializer implements ExecutionContextInitializer
{
    /**
     * The id under which the Velocity Context is stored in the Execution Context.
     */
    public static final String VELOCITY_CONTEXT_ID = "velocityContext";

    /**
     * The Velocity context factory component used for creating the Velocity Context (injected automatically by the
     * Component subsystem).
     */
    @Requirement
    private VelocityContextFactory velocityContextFactory;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.context.ExecutionContextInitializer#initialize(org.xwiki.context.ExecutionContext)
     */
    public void initialize(ExecutionContext executionContext) throws ExecutionContextException
    {
        try {
            VelocityContext context = this.velocityContextFactory.createContext();
            executionContext.setProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID, context);
        } catch (XWikiVelocityException e) {
            throw new ExecutionContextException("Failed to initialize Velocity Context", e);
        }
    }
}
