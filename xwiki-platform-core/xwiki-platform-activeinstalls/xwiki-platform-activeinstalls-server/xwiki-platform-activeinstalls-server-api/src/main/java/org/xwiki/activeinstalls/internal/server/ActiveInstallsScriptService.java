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
package org.xwiki.activeinstalls.internal.server;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.activeinstalls.server.DataManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

/**
 * Provides Scripting APIs for the Active Installs module.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Named("activeinstalls")
@Singleton
public class ActiveInstallsScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    public static final String ACTIVEINSTALLS_ERROR_KEY = "scriptservice.activeinstalls.error";

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    @Inject
    private DataManager dataManager;

    public long getTotalInstalls()
    {
        setError(null);

        long result = -1;

        try {
            result = this.dataManager.getTotalInstalls();
        } catch (Exception e) {
            setError(e);
        }

        return result;
    }

    public long getActiveInstalls()
    {
        setError(null);

        long result = -1;

        try {
            result = this.dataManager.getActiveInstalls();
        } catch (Exception e) {
            setError(e);
        }

        return result;
    }

    // Error management

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return the exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ACTIVEINSTALLS_ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(ACTIVEINSTALLS_ERROR_KEY, e);
    }
}
