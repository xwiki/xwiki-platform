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
package org.xwiki.xar.script;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;
import org.xwiki.xar.XarPackage;

/**
 * Provide APIs to manipulate XAR files.
 * 
 * @version $Id$
 * @since 5.4RC1
 */
@Component
@Named("xar")
@Singleton
public class XarScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    public static final String ERROR_KEY = "scriptservice.xar.error";

    /**
     * Provides access to the current context.
     */
    @Inject
    protected Execution execution;

    // Error management

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     * 
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    protected void setError(Exception e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }

    // ScriptService

    /**
     * Generate a {@link XarPackage} from the passed XAR file.
     * 
     * @param file the XAR file
     * @return the package
     */
    public XarPackage getXarPackage(File file)
    {
        try {
            return new XarPackage(file);
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * Generate a {@link XarPackage} from the passed XAR file.
     * 
     * @param stream the XAR file
     * @param close indicate if the passed stream should be closed at the end
     * @return the package
     */
    public XarPackage getXarPackage(InputStream stream, boolean close)
    {
        try {
            return new XarPackage(stream);
        } catch (Exception e) {
            setError(e);
        } finally {
            if (close) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // TODO: log something ?
                }
            }
        }

        return null;
    }
}
