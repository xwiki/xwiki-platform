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
package org.xwiki.context.internal;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.ExecutionContextManager;

@Component
public class DefaultExecutionContextManager implements ExecutionContextManager
{
    @Requirement
    private List<ExecutionContextInitializer> initializers = new ArrayList<ExecutionContextInitializer>();
    
    public ExecutionContext clone(ExecutionContext context) throws ExecutionContextException
    {
        ExecutionContext clonedContext = new ExecutionContext();

        // Ideally we would like to do a deep cloning here. However it's just too hard since we don't control
        // objects put in the Execution Context and they can be of any type, including Maps which are cloneable
        // but only do shallow clones. 
        // Thus instead we recreate the Execution Context from scratch and reinitialize it by calling all the
        // Execution Context Initializers on it.
        initialize(clonedContext);
        
        // Manually add the XWiki Context so that old code continues to work.
        // Note that we need to add it manually here since there's no Context Initializer that adds it.
        clonedContext.setProperty("xwikicontext", context.getProperty("xwikicontext"));

        // Manually clone the Velocity Context too since currently the XWikiVelocityContextInitializer is not yet 
        // implemented.
        // Note that we're using reflection since we don't want to add a dependency on Velocity module since that
        // would cause a cyclic dependency.
        // TODO: Fix this when XWikiVelocityContextInitializer is implemented
        // Note that Velocity doesn't provide a method for cloning a Velocity Context
        // (see https://issues.apache.org/jira/browse/VELOCITY-712). Thus we're not cloning the Velocity Context
        // which can raise problems if the included page modifies the Velocity Context...
        Object velocityContext = context.getProperty("velocityContext");
        if (velocityContext != null) {
            try {
                clonedContext.setProperty("velocityContext", 
                    velocityContext.getClass().getMethod("clone").invoke(velocityContext));
            } catch (Exception e) {
                throw new ExecutionContextException("Failed to clone Velocity Context for the new Execution Context", e);
            }
        }
        
        return clonedContext;
    }

    public void initialize(ExecutionContext context) throws ExecutionContextException
    {
        for (ExecutionContextInitializer initializer: this.initializers) {
            initializer.initialize(context);
        }
    }
    
    public void addExecutionContextInitializer(ExecutionContextInitializer initializer)
    {
        this.initializers.add(initializer);
    }
}
