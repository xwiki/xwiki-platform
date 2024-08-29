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
package com.xpn.xwiki.internal.pdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fop.apps.EnvironmentProfile;
import org.apache.fop.apps.EnvironmentalProfileFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.apps.PageSequenceResults;
import org.apache.fop.fonts.FontManager;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.pdf.impl.PDFResourceResolver;
import com.xpn.xwiki.pdf.impl.XWikiFOPEventListener;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Renders XSL-FO using Apache FOP.
 * 
 * @version $Id$
 * @since 9.11
 */
@Component
@Singleton
@Named("fop")
public class FOPXSLFORenderer implements XSLFORenderer, Initializable
{
    /**
     * The location where the renderer looks for fonts.
     */
    private static final String FONTS_PATH = "/WEB-INF/fonts/";

    private static final String RENDERERS = "renderers";

    private static final String MIME_TYPE_PDF = "application/pdf";

    private static final String MIME = "mime";

    private static final String FONTS = "fonts";

    @Inject
    private Logger logger;

    @Inject
    private PDFResourceResolver resourceResolver;

    @Inject
    private Environment environment;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * XSLT transformer factory.
     */
    private TransformerFactory transformerFactory = TransformerFactory.newInstance();

    /**
     * The Apache FOP instance used for XSL-FO processing.
     */
    private FopFactory fopFactory;

    @Override
    public void initialize() throws InitializationException
    {
        EnvironmentProfile environmentProfile =
            EnvironmentalProfileFactory.createDefault(getBaseURI(), this.resourceResolver);
        FopFactoryBuilder builder = new FopFactoryBuilder(environmentProfile);

        // Change the location of the FOP font cache file so that it doesn't use the current user's home directory
        // since the user used to start XWiki (i.e. the user with whom the servlet container is started) doesn't
        // always have a home directory on servers, nor have a ".fop" directory in the current directory. Thus to be
        // safe, we make it point to the XWiki permanent directory.
        setCacheFile(environmentProfile.getFontManager());

        Configuration configuration = loadConfiguration();
        if (configuration != null) {
            builder.setConfiguration(configuration);
        }

        this.fopFactory = builder.build();
    }

    @Override
    public void render(InputStream input, OutputStream output, String outputFormat) throws Exception
    {
        FOUserAgent foUserAgent = this.fopFactory.newFOUserAgent();

        // Transform FOP fatal errors into warnings so that the PDF export isn't stopped.
        foUserAgent.getEventBroadcaster().addEventListener(new XWikiFOPEventListener());

        // Construct FOP with desired output format.
        Fop fop = this.fopFactory.newFop(outputFormat, foUserAgent, output);

        // Identity transformer
        Transformer transformer = this.transformerFactory.newTransformer();

        // Setup input stream.
        Source source = new StreamSource(input);

        // Resulting SAX events (the generated FO) must be piped through to FOP.
        Result result = new SAXResult(fop.getDefaultHandler());

        // Start XSLT transformation and FOP processing.
        transformer.transform(source, result);

        // Result processing
        FormattingResults foResults = fop.getResults();
        if (foResults != null && this.logger.isDebugEnabled()) {
            @SuppressWarnings("unchecked")
            List<PageSequenceResults> pageSequences = foResults.getPageSequences();
            for (PageSequenceResults pageSequenceResults : pageSequences) {
                this.logger.debug("PageSequence " + StringUtils.defaultIfEmpty(pageSequenceResults.getID(), "<no id>")
                    + " generated " + pageSequenceResults.getPageCount() + " pages.");
            }
            this.logger.debug("Generated " + foResults.getPageCount() + " pages in total.");
        }
    }

    private Configuration loadConfiguration()
    {
        Configuration configuration = null;
        try (InputStream fopConfigurationFile = FOPXSLFORenderer.class.getResourceAsStream("/fop-config.xml")) {
            if (fopConfigurationFile != null) {
                configuration = new DefaultConfigurationBuilder().build(fopConfigurationFile);
            }
        } catch (Exception e) {
            this.logger.warn("Wrong FOP configuration: " + ExceptionUtils.getRootCauseMessage(e));
        }

        configuration = maybeExtendConfiguration(configuration);

        return configuration;
    }

    private Configuration maybeExtendConfiguration(Configuration configuration)
    {
        Configuration writableConfiguration = configuration;
        if (writableConfiguration != null) {
            // Get a writable configuration instance.
            if (!(writableConfiguration instanceof DefaultConfiguration)) {
                try {
                    writableConfiguration = new DefaultConfiguration(configuration, true);
                } catch (ConfigurationException e) {
                    // Should never happen.
                    this.logger.error("Failed to copy configuration.", e);
                }
            }

            if (writableConfiguration instanceof DefaultConfiguration) {
                extendConfiguration((DefaultConfiguration) writableConfiguration);
            }
        }

        return writableConfiguration;
    }

    private void extendConfiguration(DefaultConfiguration writableConfiguration)
    {
        // Add XWiki fonts folder to the configuration.
        try {
            String fontsPath = this.environment.getResource(FONTS_PATH).getPath();
            XWikiContext xcontext = this.xcontextProvider.get();
            if (xcontext != null) {
                XWikiRequest request = xcontext.getRequest();
                if (request != null && request.getSession() != null) {
                    fontsPath = request.getSession().getServletContext().getRealPath(FONTS_PATH);
                }
            }

            // <renderers>
            DefaultConfiguration renderersConfiguration =
                (DefaultConfiguration) writableConfiguration.getChild(RENDERERS, false);
            if (renderersConfiguration == null) {
                renderersConfiguration = new DefaultConfiguration(RENDERERS);
                writableConfiguration.addChild(renderersConfiguration);
            }

            // Ensure we have support for PDF rendering.
            // <renderer mime="application/pdf">
            DefaultConfiguration pdfRenderer = null;
            for (Configuration renderer : renderersConfiguration.getChildren()) {
                if (MIME_TYPE_PDF.equals(renderer.getAttribute(MIME))) {
                    pdfRenderer = (DefaultConfiguration) renderer;
                }
            }
            if (pdfRenderer == null) {
                pdfRenderer = new DefaultConfiguration("renderer");
                pdfRenderer.setAttribute(MIME, MIME_TYPE_PDF);
                renderersConfiguration.addChild(pdfRenderer);
            }

            // <fonts>
            DefaultConfiguration fontsConfiguration = (DefaultConfiguration) pdfRenderer.getChild(FONTS, false);
            if (fontsConfiguration == null) {
                fontsConfiguration = new DefaultConfiguration(FONTS);
                pdfRenderer.addChild(fontsConfiguration);
            }

            // <directory>fontdirectory</directory>
            DefaultConfiguration directoryConfiguration = new DefaultConfiguration("directory");
            directoryConfiguration.setValue(fontsPath);
            fontsConfiguration.addChild(directoryConfiguration);
        } catch (Exception e) {
            this.logger.warn("Starting with 1.5, XWiki uses the WEB-INF/fonts/ directory as the font directory, "
                + "and it should contain the FreeFont (http://savannah.gnu.org/projects/freefont/) fonts. "
                + "FOP cannot access this directory. If this is an upgrade from a previous version, "
                + "make sure you also copy the WEB-INF/fonts directory from the new distribution package.");
        }
    }

    private void setCacheFile(FontManager fontManager) throws InitializationException
    {
        File cacheDirectory =  new File(this.environment.getPermanentDirectory(), "cache");
        File fopDirectory = new File(cacheDirectory, "fop");
        File cacheFile = new File(fopDirectory, "fop-fonts.cache");
        try {
            FileUtils.forceMkdir(fopDirectory);
            fontManager.setCacheFile(cacheFile.toURI());
        } catch (IOException e) {
            throw new InitializationException(String.format("Failed to create FOP cache directory [%s]", fopDirectory),
                e);
        }
    }

    /**
     * Use the instance base URL as the FOP base URI so that any relative URLs returned by XWiki when rendering a
     * page in view mode (for browser consumption - it's a best practice to return relative URLs and let browsers
     * convert them to full URLs) are properly converted by FOP to full URIs before
     * {@link org.apache.xmlgraphics.io.ResourceResolver#getResource(URI)} is called. Note that we currently use
     * {@link com.xpn.xwiki.web.ExternalServletURLFactory} in {@link com.xpn.xwiki.web.ExportAction} and thus all
     * URLs arriving in FOP should already be absolute.
     */
    private URI getBaseURI() throws InitializationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            return xcontext.getURLFactory().getServerURL(xcontext).toURI();
        } catch (Exception e) {
            throw new InitializationException("Failed to get the base URI for exporting to PDF", e);
        }
    }
}
