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
package com.xpn.xwiki.internal.template;

import java.util.Stack;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorExecutor;

/**
 * Allow executing some code with the right of a provided user.
 *
 * @version $Id$
 * @since 6.3M2
 * @deprecated since 8.3RC1, use {@link AuthorExecutor} instead
 */
@Component(roles = SUExecutor.class)
@Singleton
@Deprecated
public class SUExecutor
{
    /**
     * Contain the informations to restore.
     *
     * @version $Id$
     */
    public static final class SUExecutorContext
    {
        private AutoCloseable context;

        private SUExecutorContext(AutoCloseable context)
        {
            this.context = context;
        }
    }

    @Inject
    private AuthorExecutor executor;

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    /**
     * Execute the passed {@link Callable} with the rights of the passed user.
     *
     * @param callable the the task to execute
     * @param author the user to check rights on
     * @return computed result
     * @throws Exception if unable to compute a result
     * @param <V> the result type of method {@code call}
     */
    public <V> V call(Callable<V> callable, DocumentReference author) throws Exception
    {
        return this.executor.call(callable, author);
    }

    /**
     * Setup the context so that following code is executed with provided user rights.
     * <p>
     * The returned {@link SUExecutorContext} should be given to {@link #after(SUExecutorContext)} to restore the
     * context to its previous state.
     *
     * @param user the user to check rights on
     * @return the context to restore
     * @see #after(SUExecutorContext)
     */
    public SUExecutorContext before(DocumentReference user)
    {
        return new SUExecutorContext(this.executor.before(user));
    }

    /**
     * Restore the context to it's previous state as defined by the provided {@link SUExecutorContext}.
     *
     * @param suContext the context to restore
     * @see #before(DocumentReference)
     */
    public void after(SUExecutorContext suContext)
    {
        this.executor.after(suContext.context);
    }

    /**
     * Setup the context so that following code is executed with provided user rights.
     * <p>
     * {@link #pop()} should be called to restore the context to its previous state.
     *
     * @param user the user to check rights on
     * @see #pop()
     */
    public void push(DocumentReference user)
    {
        // Modify the context for that following code is executed with the right of wiki macro author
        SUExecutorContext sucontext = before(user);

        // Put it in an hidden context property to restore it later
        ExecutionContext econtext = this.execution.getContext();
        // Use a stack in case a wiki macro calls another wiki macro
        Stack<SUExecutorContext> backup = (Stack<SUExecutorContext>) econtext.getProperty(SUExecutor.class.getName());
        if (backup == null) {
            backup = new Stack<SUExecutorContext>();
            econtext.setProperty(SUExecutor.class.getName(), backup);
        }
        backup.push(sucontext);
    }

    /**
     * Restore the context to it's previous state as defined by the provided {@link SUExecutorContext}.
     */
    public void pop()
    {
        // Get the su context to restore
        ExecutionContext econtext = this.execution.getContext();
        // Use a stack in case a wiki macro calls another wiki macro
        Stack<SUExecutorContext> backup = (Stack<SUExecutorContext>) econtext.getProperty(SUExecutor.class.getName());
        if (backup != null && !backup.isEmpty()) {
            // Restore the context execution rights
            after(backup.pop());
        } else {
            this.logger.error("Can't find any backed up execution right information in the execution context");
        }
    }
}
