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
package org.xwiki.extension.security;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.script.ExtensionIndexScriptService;
import org.xwiki.extension.script.ExtensionManagerScriptService;
import org.xwiki.extension.security.internal.ExtensionSecuritySolrClient;
import org.xwiki.script.service.ScriptService;

import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Script service for the extension security extension. Gives access to the configuration.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named(ExtensionManagerScriptService.ROLEHINT + '.' + ExtensionIndexScriptService.ID + '.'
    + ExtensionSecurityScriptService.ID)
public class ExtensionSecurityScriptService implements ScriptService
{
    /**
     * The id of the component, also used as the last segment of the component hint.
     */
    public static final String ID = "security";

    @Inject
    private ExtensionSecurityConfiguration extensionSecurityConfiguration;

    @Inject
    private ExtensionSecuritySolrClient solrClient;

    @Inject
    private Logger logger;

    /**
     * @return {@code true} when the security scan is enabled, {@code false} otherwise. When the security scan is
     *     disabled, no security scan is performed and the list of security vulnerabilities is not displayed in the
     *     administration. The default value is {@code true}
     */
    public boolean isSecurityScanEnabled()
    {
        return this.extensionSecurityConfiguration.isSecurityScanEnabled();
    }

    /**
     * @return the count of extensions with at least one known vulnerability, if an extension is available with
     *     different (vulnerable) versions, they are all counted individually
     */
    public long getVulnerableExtensionsCount()
    {
        try {
            return this.solrClient.getVulnerableExtensionsCount();
        } catch (SolrServerException | IOException e) {
            this.logger.warn("Failed to retrieve the count of extensions with known vulnerabilities. Cause: [{}]",
                getRootCauseMessage(e));
            return 0;
        }
    }
}
