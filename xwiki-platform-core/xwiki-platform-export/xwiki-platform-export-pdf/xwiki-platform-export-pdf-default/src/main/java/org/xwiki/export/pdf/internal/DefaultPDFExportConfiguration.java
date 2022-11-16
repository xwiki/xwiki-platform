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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Default implementation of {@link PDFExportConfiguration}.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component
@Singleton
public class DefaultPDFExportConfiguration implements PDFExportConfiguration
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @Inject
    @Named("export/pdf")
    private ConfigurationSource configDocument;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public String getChromeDockerImage()
    {
        return getProperty("chromeDockerImage", "zenika/alpine-chrome:latest");
    }

    @Override
    public String getChromeDockerContainerName()
    {
        return getProperty("chromeDockerContainerName", "headless-chrome-pdf-printer");
    }

    @Override
    public String getDockerNetwork()
    {
        return getProperty("dockerNetwork", "bridge");
    }

    @Override
    public String getChromeHost()
    {
        return getProperty("chromeHost", "");
    }

    @Override
    public int getChromeRemoteDebuggingPort()
    {
        return getProperty("chromeRemoteDebuggingPort", 9222);
    }

    @Override
    public String getXWikiHost()
    {
        return getProperty("xwikiHost", "host.xwiki.internal");
    }

    @Override
    public boolean isServerSide()
    {
        return getProperty("serverSide", PDFExportConfiguration.super.isServerSide());
    }

    @Override
    public List<DocumentReference> getTemplates()
    {
        @SuppressWarnings("unchecked")
        List<String> templates = this.configDocument.getProperty("templates", List.class,
            Collections.singletonList("XWiki.PDFExport.Template"));
        return templates.stream().filter(StringUtils::isNotBlank)
            .map(template -> this.documentReferenceResolver.resolve(template)).collect(Collectors.toList());
    }

    @Override
    public int getPageReadyTimeout()
    {
        return getProperty("pageReadyTimeout", PDFExportConfiguration.super.getPageReadyTimeout());
    }

    @Override
    public int getMaxContentSize()
    {
        return getProperty("maxContentSize", PDFExportConfiguration.super.getMaxContentSize());
    }

    private <T> T getProperty(String key, T defaultValue)
    {
        if (this.configDocument.containsKey(key)) {
            return this.configDocument.getProperty(key, defaultValue);
        } else {
            return this.xwikiProperties.getProperty("export.pdf." + key, defaultValue);
        }
    }
}
