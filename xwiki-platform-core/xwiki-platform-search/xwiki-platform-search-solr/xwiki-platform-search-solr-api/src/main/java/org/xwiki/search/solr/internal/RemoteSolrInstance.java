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
package org.xwiki.search.solr.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.search.solr.internal.api.SolrConfiguration;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Remote Solr instance communicating over HTTP.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named(RemoteSolrInstance.TYPE)
@Singleton
public class RemoteSolrInstance extends AbstractSolrInstance
{
    /**
     * Solr instance type for this implementation.
     */
    public static final String TYPE = "remote";

    /**
     * Default URL to use when none is specified.
     */
    public static final String DEFAULT_REMOTE_URL = "http://localhost:8983/solr/";

    /**
     * The name of the attachment containing the zipped configuration files.
     */
    public static final String CONFIGURATION_ZIP_FILE_NAME = "conf.zip";

    /**
     * Execution context needed for XWiki model operation.
     */
    @Inject
    private Execution execution;

    /**
     * Solr's configuration.
     */
    @Inject
    private SolrConfiguration configuration;

    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        String remoteURL = configuration.getInstanceConfiguration(TYPE, "url", DEFAULT_REMOTE_URL);

        try {
            this.generateAndAttachConfigurationZipIfNotExist();
        } catch (Exception e) {
            // This is not a critical issue, since the remote server may already be configured.
            // We still log it as an error though.
            logger.error("Failed to generate the remote server's configuration.", e);
        }

        // Initialize the remote Solr server.
        this.server = new HttpSolrServer(remoteURL);
    }

    /**
     * Generates the configuration files required to properly configure and initialize the remote Solr server.
     * <p/>
     * The files are available as a zip archive ("solr.zip") attached to the main wiki's XWiki.SolrSearchAdmin document
     * and exposed in the user interface of the main wiki.
     * <p/>
     * Note: If the attachment already exists, nothing will be done.
     * 
     * @throws Exception if problems occur.
     */
    public void generateAndAttachConfigurationZipIfNotExist() throws Exception
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Get or Create the attachment and dump the content of the zip into the attachment.
        DocumentReference configDocumentReference =
            new DocumentReference(getXWikiContext().getMainXWiki(), "XWiki", "SolrSearchAdmin");
        XWikiDocument configurationDocument = xwiki.getDocument(configDocumentReference, context);
        XWikiAttachment configurationZipAttachment = configurationDocument.getAttachment(CONFIGURATION_ZIP_FILE_NAME);
        if (configurationZipAttachment == null) {
            // Create the zip file to attach.
            Map<String, URL> homeDirectoryFiles = this.configuration.getHomeDirectoryConfiguration();
            InputStream zipFileInputStream = createZip(homeDirectoryFiles);

            // Attach the file.
            configurationDocument.addAttachment(CONFIGURATION_ZIP_FILE_NAME, zipFileInputStream, context);
        }
    }

    /**
     * @param files a map of (fileName, fileDataURL).
     * @return a zip containing the files specified by the input map.
     * @throws IOException if problems occur.
     */
    private static InputStream createZip(Map<String, URL> files) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipfile = new ZipOutputStream(baos);
        for (Map.Entry<String, URL> file : files.entrySet()) {
            // Create a new zip entry.
            zipfile.putNextEntry(new ZipEntry(file.getKey()));

            // Get the file data as a byte array.
            URL fileURL = file.getValue();
            byte[] fileData = IOUtils.toByteArray(fileURL.openStream());

            // Compress the file data for the current zip entry.
            zipfile.write(fileData);
        }
        zipfile.close();

        // Return the zipped data as an input stream.
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * @return the XWiki context needed for XWiki model operations.
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }
}
