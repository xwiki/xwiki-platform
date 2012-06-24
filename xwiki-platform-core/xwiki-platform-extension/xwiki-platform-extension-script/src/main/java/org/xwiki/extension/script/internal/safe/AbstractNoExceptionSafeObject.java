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
package org.xwiki.extension.script.internal.safe;

import javax.inject.Inject;

import org.xwiki.context.Execution;
import org.xwiki.extension.internal.safe.AbstractSafeObject;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.extension.script.internal.ExtensionManagerScriptService;

/**
 * Encapsulate {@link AbstractSafeObject} with tools to make easier to use the wrapped API with script language not
 * supporting exceptions.
 * 
 * @param <T> the type of the wrapped object
 * @version $Id$
 * @since 4.0M2
 */
public abstract class AbstractNoExceptionSafeObject<T> extends AbstractSafeObject<T>
{
    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    /**
     * @param wrapped the wrapped object
     * @param safeProvider the provider of instances safe for public scripts
     * @param execution provide access to the current context
     */
    public AbstractNoExceptionSafeObject(T wrapped, ScriptSafeProvider< ? > safeProvider, Execution execution)
    {
        super(wrapped, safeProvider);

        this.execution = execution;
    }

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ExtensionManagerScriptService.EXTENSIONERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     * 
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    protected void setError(Exception e)
    {
        this.execution.getContext().setProperty(ExtensionManagerScriptService.EXTENSIONERROR_KEY, e);
    }
}
