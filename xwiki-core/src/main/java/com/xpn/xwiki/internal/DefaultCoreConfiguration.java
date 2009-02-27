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
package com.xpn.xwiki.internal;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.configuration.ConfigurationSourceCollection;

import com.xpn.xwiki.CoreConfiguration;

/**
 * Configuration for the Core module.
 * 
 * @version $Id: $
 * @since 1.8RC2
 */
public class DefaultCoreConfiguration implements Initializable, CoreConfiguration
{
    /**
     * @see CoreConfiguration#getDefaultDocumentSyntax()
     */
    private String defaultDocumentSyntax = "xwiki/1.0";

    /**
     * Injected by the Component Manager.
     */
    private ConfigurationManager configurationManager;

    /**
     * Injected by the Component Manager.
     */
    private ConfigurationSourceCollection sourceCollection;

    /**
     * {@inheritDoc}
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.configurationManager.initializeConfiguration(this, this.sourceCollection.getConfigurationSources(),
            "core");
    }

    /**
     * @see CoreConfiguration#getDefaultDocumentSyntax()
     */
    public String getDefaultDocumentSyntax()
    {
        return this.defaultDocumentSyntax;
    }

    public void setDefaultDocumentSyntax(String defaultDocumentSyntax)
    {
        this.defaultDocumentSyntax = defaultDocumentSyntax;
    }
}
