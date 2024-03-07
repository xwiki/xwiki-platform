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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.script.safe.ScriptSafeProvider;
import org.xwiki.security.authorization.ContextualAuthorizationManager;

/**
 * Provide a safe {@link PDFExportConfiguration}.
 * 
 * @version $Id$
 * @since 14.8
 */
@Component
@Singleton
public class SafePDFExportConfigurationProvider implements ScriptSafeProvider<PDFExportConfiguration>
{
    /**
     * The provider of instances safe for public scripts.
     */
    @Inject
    @SuppressWarnings("rawtypes")
    private ScriptSafeProvider defaultSafeProvider;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    @SuppressWarnings("unchecked")
    public <S> S get(PDFExportConfiguration unsafe)
    {
        return (S) new SafePDFExportConfiguration(unsafe, this.defaultSafeProvider, this.authorization);
    }
}
