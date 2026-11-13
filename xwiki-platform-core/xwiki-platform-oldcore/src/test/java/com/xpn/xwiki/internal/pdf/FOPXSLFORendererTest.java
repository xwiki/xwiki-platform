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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.apache.fop.configuration.Configuration;
import org.apache.fop.configuration.ConfigurationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.environment.Environment;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.pdf.impl.PDFResourceResolver;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FOPXSLFORenderer}.
 *
 * @version $Id$
 */
@ComponentTest
class FOPXSLFORendererTest
{
    private static final String PDF = "application/pdf";

    @XWikiTempDir
    private File tmpDir;

    @InjectMockComponents
    private FOPXSLFORenderer renderer;

    @MockComponent
    private PDFResourceResolver resourceResolver;

    @MockComponent
    private Environment environment;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private File fontsDirectory;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        XWikiContext xcontext = mock();
        XWikiURLFactory urlFactory = mock();
        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getURLFactory()).thenReturn(urlFactory);
        when(urlFactory.getServerURL(xcontext)).thenReturn(new URL("http://localhost:8080/"));

        when(this.environment.getPermanentDirectory()).thenReturn(this.tmpDir);
        this.fontsDirectory = new File(this.tmpDir, "fonts");
        when(this.environment.getResource("/WEB-INF/fonts/")).thenReturn(this.fontsDirectory.toURI().toURL());
    }

    private Configuration loadConfiguration(String configuration) throws Exception
    {
        return this.renderer
            .loadConfiguration(new ByteArrayInputStream(configuration.getBytes(StandardCharsets.UTF_8)));
    }

    private List<String> getFontDirectories(Configuration renderer)
    {
        return Arrays.stream(renderer.getChild("fonts").getChildren("directory"))
            .map(directory -> directory.getValue(null)).toList();
    }

    private String getFontsPath()
    {
        return this.fontsDirectory.toURI().getPath();
    }

    @Test
    void loadConfigurationWithoutRenderers() throws Exception
    {
        Configuration configuration = loadConfiguration("<fop version=\"1.0\"><strict-validation>false"
            + "</strict-validation></fop>");

        assertEquals("false", configuration.getChild("strict-validation").getValue());
        Configuration[] renderers = configuration.getChild("renderers").getChildren("renderer");
        assertEquals(1, renderers.length);
        assertEquals(PDF, renderers[0].getAttribute("mime"));
        assertEquals(List.of(getFontsPath()), getFontDirectories(renderers[0]));
    }

    @Test
    void loadConfigurationWithExistingPDFFonts() throws Exception
    {
        Configuration configuration = loadConfiguration("<fop version=\"1.0\"><renderers>"
            + "<renderer mime=\"image/png\"><fonts><directory>/png</directory></fonts></renderer>"
            + "<renderer mime=\"application/pdf\"><version>1.7</version><fonts><auto-detect/>"
            + "<directory recursive=\"true\">/custom</directory></fonts></renderer>"
            + "</renderers></fop>");

        Configuration[] renderers = configuration.getChild("renderers").getChildren("renderer");
        assertEquals(2, renderers.length);
        assertEquals(List.of("/png"), getFontDirectories(renderers[0]));
        assertEquals(PDF, renderers[1].getAttribute("mime"));
        assertEquals("1.7", renderers[1].getChild("version").getValue());
        Configuration fonts = renderers[1].getChild("fonts");
        assertEquals(1, fonts.getChildren("auto-detect").length);
        assertEquals("true", fonts.getChildren("directory")[0].getAttribute("recursive"));
        assertEquals(List.of("/custom", getFontsPath()), getFontDirectories(renderers[1]));
    }

    @Test
    void loadConfigurationWithoutPDFRenderer() throws Exception
    {
        Configuration configuration = loadConfiguration("<fop version=\"1.0\"><renderers>"
            + "<renderer mime=\"image/png\"/></renderers></fop>");

        Configuration[] renderers = configuration.getChild("renderers").getChildren("renderer");
        assertEquals(2, renderers.length);
        assertEquals("image/png", renderers[0].getAttribute("mime"));
        assertEquals(0, renderers[0].getChild("fonts").getChildren("directory").length);
        assertEquals(PDF, renderers[1].getAttribute("mime"));
        assertEquals(List.of(getFontsPath()), getFontDirectories(renderers[1]));
    }

    @Test
    void loadConfigurationWithoutFontsFolder() throws Exception
    {
        when(this.environment.getResource("/WEB-INF/fonts/")).thenReturn(null);

        Configuration configuration = loadConfiguration("<fop version=\"1.0\"/>");

        assertEquals(0, configuration.getChild("renderers").getChildren("renderer").length);
        assertThat(this.logCapture.getMessage(0), startsWith("Starting with 1.5, XWiki uses the WEB-INF/fonts/"
            + " directory as the font directory, and it should contain the FreeFont"
            + " (http://savannah.gnu.org/projects/freefont/) fonts. FOP cannot access this directory. If this is an"
            + " upgrade from a previous version, make sure you also copy the WEB-INF/fonts directory from the new"
            + " distribution package. Root cause is [NullPointerException: "));
    }

    @Test
    void render() throws Exception
    {
        String fo = "<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\"><fo:layout-master-set>"
            + "<fo:simple-page-master master-name=\"page\"><fo:region-body/></fo:simple-page-master>"
            + "</fo:layout-master-set><fo:page-sequence master-reference=\"page\">"
            + "<fo:flow flow-name=\"xsl-region-body\"><fo:block>Hello FOP</fo:block></fo:flow></fo:page-sequence>"
            + "</fo:root>";
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        this.renderer.render(new ByteArrayInputStream(fo.getBytes(StandardCharsets.UTF_8)), output, PDF);

        try (PDDocument document = Loader.loadPDF(output.toByteArray())) {
            assertEquals(1, document.getNumberOfPages());
            PDResources resources = document.getPage(0).getResources();
            List<String> fonts = new ArrayList<>();
            for (COSName fontName : resources.getFontNames()) {
                fonts.add(resources.getFont(fontName).getName());
            }
            // The text is written with FOP's default font.
            assertEquals(List.of("Helvetica"), fonts);
        }
        // The FOP font cache is stored in the permanent directory.
        assertTrue(new File(this.tmpDir, "cache/fop").isDirectory());
    }

    @Test
    void loadConfigurationWithExternalDTD() throws Exception
    {
        // The external DTD is not loaded, which doesn't prevent loading the configuration.
        Configuration configuration = loadConfiguration(
            "<!DOCTYPE fop SYSTEM \"" + this.tmpDir.toURI() + "missing.dtd\"><fop version=\"1.0\"/>");

        assertEquals("1.0", configuration.getAttribute("version"));
    }

    @Test
    void loadConfigurationWithExternalEntity() throws Exception
    {
        File secret = new File(this.tmpDir, "secret.txt");
        Files.writeString(secret.toPath(), "secret");

        Configuration configuration =
            loadConfiguration("<!DOCTYPE fop [<!ENTITY secret SYSTEM \"" + secret.toURI() + "\">]>"
                + "<fop version=\"1.0\"><base>before&secret;after</base></fop>");

        // The external entity is not resolved.
        assertEquals("beforeafter", configuration.getChild("base").getValue());
    }

    @Test
    void loadInvalidConfiguration()
    {
        ConfigurationException exception =
            assertThrows(ConfigurationException.class, () -> loadConfiguration("<fop>"));

        assertEquals("Failed to parse the FOP configuration", exception.getMessage());
    }
}
