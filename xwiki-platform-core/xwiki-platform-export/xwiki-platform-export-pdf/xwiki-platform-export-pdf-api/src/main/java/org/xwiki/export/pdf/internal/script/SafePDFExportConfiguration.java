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

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.safe.AbstractSafeObject;
import org.xwiki.script.safe.ScriptSafeProvider;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Script-safe version of {@link PDFExportConfiguration}.
 * 
 * @version $Id$
 * @since 14.8
 */
public class SafePDFExportConfiguration extends AbstractSafeObject<PDFExportConfiguration>
    implements PDFExportConfiguration
{
    private ContextualAuthorizationManager authorization;

    /**
     * Creates a new safe instance that wraps the given unsafe instance.
     * 
     * @param wrapped the unsafe, wrapped object
     * @param safeProvider the provider of instances safe for public scripts
     * @param authorization the component used to check access rights
     */
    public SafePDFExportConfiguration(PDFExportConfiguration wrapped, ScriptSafeProvider<?> safeProvider,
        ContextualAuthorizationManager authorization)
    {
        super(wrapped, safeProvider);

        this.authorization = authorization;
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
    public URI getXWikiURI()
    {
        return null;
    }

    @Override
    public boolean isServerSide()
    {
        return getWrapped().isServerSide();
    }

    @Override
    public List<DocumentReference> getTemplates()
    {
        return getWrapped().getTemplates().stream()
            .filter(templateReference -> this.authorization.hasAccess(Right.VIEW, templateReference))
            .collect(Collectors.toList());
    }

    @Override
    public int getPageReadyTimeout()
    {
        return getWrapped().getPageReadyTimeout();
    }

    @Override
    public int getMaxContentSize()
    {
        return getWrapped().getMaxContentSize();
    }

    @Override
    public int getThreadPoolSize()
    {
        return getWrapped().getThreadPoolSize();
    }

    @Override
    public boolean isReplacingFOP()
    {
        return getWrapped().isReplacingFOP();
    }
}
