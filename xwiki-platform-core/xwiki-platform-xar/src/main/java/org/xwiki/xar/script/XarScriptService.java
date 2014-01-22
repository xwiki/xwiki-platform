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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.xar.internal.XarEntry;
import org.xwiki.xar.internal.XarPackage;

/**
 * Provide APIs to manipulate XAR files.
 * 
 * @version $Id$
 * @since 5.4RC1
 */
@Component
@Named("xar")
@Unstable
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
     * Extract the list of the document in the passed XAR file.
     * 
     * @param file the XAR file
     * @return the documents in the passed XAR file
     */
    public List<LocalDocumentReference> getReferences(File file)
    {
        try {
            return getReferences(new XarPackage(file));
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * Extract the list of the document in the passed XAR file.
     * 
     * @param stream the XAR file
     * @return the documents in the passed XAR file
     */
    public List<LocalDocumentReference> getReferences(InputStream stream)
    {
        try {
            return getReferences(new XarPackage(stream));
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    private List<LocalDocumentReference> getReferences(XarPackage xarPackage)
    {
        Collection<XarEntry> entries = xarPackage.getEntries();
        List<LocalDocumentReference> references = new ArrayList<LocalDocumentReference>(entries.size());
        for (XarEntry entry : entries) {
            references.add(entry.getReference());
        }

        return references;
    }
}
