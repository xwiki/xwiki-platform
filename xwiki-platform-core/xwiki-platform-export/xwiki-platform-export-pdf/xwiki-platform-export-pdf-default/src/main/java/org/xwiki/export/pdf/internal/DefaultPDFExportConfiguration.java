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
package org.xwiki.export.pdf.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.export.pdf.PDFExportConfiguration;

/**
 * Default implementation of {@link PDFExportConfiguration}.
 * 
 * @version $Id$
 * @since 14.4.1
 * @since 14.5RC1
 */
@Component
@Singleton
public class DefaultPDFExportConfiguration implements PDFExportConfiguration
{
    private static final String PREFIX = "export.pdf.";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Override
    public String getChromeDockerImage()
    {
        return this.configurationSource.getProperty(PREFIX + "chromeDockerImage", "zenika/alpine-chrome:latest");
    }

    @Override
    public String getChromeDockerContainerName()
    {
        return this.configurationSource.getProperty(PREFIX + "chromeDockerContainerName",
            "headless-chrome-pdf-printer");
    }

    @Override
    public String getChromeDockerHostName()
    {
        return this.configurationSource.getProperty(PREFIX + "chromeDockerHostName", "host.docker.internal");
    }

    @Override
    public int getChromeRemoteDebuggingPort()
    {
        return this.configurationSource.getProperty(PREFIX + "chromeRemoteDebuggingPort", 9222);
    }
}
