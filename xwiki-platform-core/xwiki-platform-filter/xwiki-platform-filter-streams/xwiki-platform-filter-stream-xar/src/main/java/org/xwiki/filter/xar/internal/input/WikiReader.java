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
package org.xwiki.filter.xar.internal.input;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.logging.marker.TranslationMarker;
import org.xwiki.xar.XarPackage;
import org.xwiki.xar.internal.model.XarModel;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component(roles = WikiReader.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class WikiReader
{
    private static final TranslationMarker LOG_DOCUMENT_SKIPPED = new TranslationMarker(
        "filter.xar.log.document.skipped", WikiDocumentFilter.LOG_DOCUMENT_SKIPPED);

    private static final TranslationMarker LOG_DOCUMENT_FAILREAD = new TranslationMarker(
        "filter.xar.log.document.failread", WikiDocumentFilter.LOG_DOCUMENT_ERROR);

    private static final TranslationMarker LOG_DESCRIPTOR_FAILREAD = new TranslationMarker(
        "filter.xar.log.descriptor.failread");

    @Inject
    private DocumentLocaleReader documentReader;

    @Inject
    private Logger logger;

    private XARInputProperties properties;

    private XarPackage xarPackage = new XarPackage();

    public void setProperties(XARInputProperties properties)
    {
        this.properties = properties;

        this.documentReader.setProperties(properties);
    }

    public XarPackage getXarPackage()
    {
        return this.xarPackage;
    }

    public void read(Object filter, XARInputFilter proxyFilter) throws IOException,
        FilterException
    {
        InputStream stream;

        InputSource source = this.properties.getSource();
        if (source instanceof InputStreamInputSource) {
            stream = ((InputStreamInputSource) source).getInputStream();
        } else {
            throw new FilterException("Unsupported source type [" + source.getClass() + "]");
        }

        read(stream, filter, proxyFilter);

        // Close last space
        if (this.documentReader.getCurrentSpace() != null) {
            proxyFilter.endWikiSpace(this.documentReader.getCurrentSpace(),
                this.documentReader.getCurrentSpaceParameters());
        }

        // Send extension event
        if (this.xarPackage.getPackageExtensionId() != null) {
            proxyFilter.beginExtension(this.xarPackage.getPackageExtensionId(), this.xarPackage.getPackageVersion(),
                FilterEventParameters.EMPTY);
            proxyFilter.endExtension(this.xarPackage.getPackageExtensionId(), this.xarPackage.getPackageVersion(),
                FilterEventParameters.EMPTY);
        }
    }

    public void read(InputStream stream, Object filter, XARInputFilter proxyFilter) throws IOException
    {
        ZipArchiveInputStream zis = new ZipArchiveInputStream(stream, "UTF-8", false);

        for (ZipArchiveEntry entry = zis.getNextZipEntry(); entry != null; entry = zis.getNextZipEntry()) {
            if (entry.isDirectory() || entry.getName().startsWith("META-INF")) {
                // The entry is either a directory or is something inside of the META-INF dir.
                // (we use that directory to put meta data such as LICENSE/NOTICE files.)
                continue;
            } else if (entry.getName().equals(XarModel.PATH_PACKAGE)) {
                // The entry is the manifest (package.xml). Read this differently.
                try {
                    this.xarPackage.readDescriptor(zis);
                } catch (Exception e) {
                    if (this.properties.isVerbose()) {
                        this.logger.warn(LOG_DESCRIPTOR_FAILREAD, "Failed to read XAR descriptor from entry [{}]: {}",
                            entry.getName(), ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            } else {
                try {
                    this.documentReader.read(zis, filter, proxyFilter);
                } catch (SkipEntityException skip) {
                    if (this.properties.isVerbose()) {
                        this.logger.info(LOG_DOCUMENT_SKIPPED, "Skipped document [{}]", skip.getEntityReference());
                    }
                } catch (Exception e) {
                    if (this.properties.isVerbose()) {
                        this.logger.warn(LOG_DOCUMENT_FAILREAD, "Failed to read XAR XML document from entry [{}]: {}",
                            entry.getName(), ExceptionUtils.getRootCauseMessage(e), e);
                    }
                }
            }
        }
    }
}
