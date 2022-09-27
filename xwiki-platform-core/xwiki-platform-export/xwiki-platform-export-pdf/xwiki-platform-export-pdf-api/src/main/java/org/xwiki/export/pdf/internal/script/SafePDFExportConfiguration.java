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
package org.xwiki.export.pdf.internal.script;

import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.script.internal.safe.AbstractSafeObject;
import org.xwiki.script.internal.safe.ScriptSafeProvider;

/**
 * Script-safe version of {@link PDFExportConfiguration}.
 * 
 * @version $Id$
 * @since 14.8
 */
public class SafePDFExportConfiguration extends AbstractSafeObject<PDFExportConfiguration>
    implements PDFExportConfiguration
{
    /**
     * Creates a new safe instance that wraps the given unsafe instance.
     * 
     * @param wrapped the unsafe, wrapped object
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafePDFExportConfiguration(PDFExportConfiguration wrapped, ScriptSafeProvider<?> safeProvider)
    {
        super(wrapped, safeProvider);
    }

    @Override
    public String getChromeDockerImage()
    {
        return null;
    }

    @Override
    public String getChromeDockerContainerName()
    {
        return null;
    }

    @Override
    public String getDockerNetwork()
    {
        return null;
    }

    @Override
    public String getChromeHost()
    {
        return null;
    }

    @Override
    public int getChromeRemoteDebuggingPort()
    {
        return 0;
    }

    @Override
    public String getXWikiHost()
    {
        return null;
    }

    @Override
    public boolean isServerSide()
    {
        return getWrapped().isServerSide();
    }
}
