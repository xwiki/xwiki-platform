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
 */
package org.xwiki.mail.internal.thread.context;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.CloneFailedException;
import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Additionally to {@link ExecutionContextManager#clone(ExecutionContext)}, this method also clones the
 * {@link XWikiContext} by using {@link XWikiContextCloner} and thus the resulting {@link ExecutionContext} should be
 * usable in a new thread.
 *
 * @version $Id$
 * @since 7.1M2
 */
@Component
@Singleton
public class ExecutionContexCloner implements Cloner<ExecutionContext>
{
    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private VelocityManager velociyManager;

    @Inject
    private Execution execution;

    @Inject
    private Cloner<XWikiContext> xwikiContextCloner;

    @Override
    public ExecutionContext clone(ExecutionContext originalExecutionContext) throws CloneFailedException
    {
        try {
            ExecutionContext clonedExecutionContext = this.executionContextManager.clone(originalExecutionContext);

            // XWikiContext
            // The above clone just creates and initializes an empty XWiki Context, so it needs special handling.
            XWikiContext xwikiContext =
                (XWikiContext) originalExecutionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
            XWikiContext clonedXWikiContext = xwikiContextCloner.clone(xwikiContext);
            clonedExecutionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, clonedXWikiContext);

            // We have just set the clonedXWikiContext. We can now use our clonedExecutionContext as base for the
            // initialization of the ScriptContext and the VelocityContext by pushing it in the Execution.
            try {
                this.execution.pushContext(clonedExecutionContext);

                // The managers will also run the initializers on the returned contexts, thus using the XWikiContext
                // from the execution.
                ScriptContext reInitializedScriptContext = scriptContextManager.getScriptContext();
                // Note: scriptContextManager.getScriptContext() is already called by
                // velociyManager.getVelocityContext() internally, so we could avoid running it twice just for the sake
                // of code readability.
                VelocityContext reInitializedVelocityContext = velociyManager.getVelocityContext();
                // Note2: we could even argue that velociyManager.getVelocityContext() will eventually be called by a
                // client willing to use it, since it should generally not be used directly from the ExecutionContext,
                // but it should be always used through the VelocityManager. Could this entire inner try-catch block be
                // removed and deemed not needed? Would it be safe enough?
            } finally {
                // Remember to pop it, since we don`t want it to replace our current ExecutionContext.
                this.execution.popContext();
            }

            return clonedExecutionContext;
        } catch (Exception e) {
            throw new CloneFailedException(String.format("Failed to clone [%s]", originalExecutionContext), e);
        }
    }

}
