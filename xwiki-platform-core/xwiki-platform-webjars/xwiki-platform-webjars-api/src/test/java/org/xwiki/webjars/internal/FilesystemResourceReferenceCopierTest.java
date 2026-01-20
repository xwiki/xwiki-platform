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
package org.xwiki.webjars.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;
import org.xwiki.url.filesystem.FilesystemExportContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FilesystemResourceReferenceCopier}.
 *
 * @version $Id$
 */
@ExtendWith(XWikiTempDirExtension.class)
@SuppressWarnings({ "checkstyle:MultipleStringLiterals" })
class FilesystemResourceReferenceCopierTest
{
    private static final String RESOURCE_PREFIX = "META-INF/resources/webjars/testlib/1.0.0";

    @XWikiTempDir
    private File tmpDir;

    private File exportDir;

    private File jarFile;

    private FilesystemExportContext exportContext;

    private ClassLoader originalClassLoader;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp()
    {
        this.exportDir = new File(this.tmpDir, "export");
        this.exportDir.mkdirs();

        this.exportContext = new FilesystemExportContext();
        this.exportContext.setExportDir(this.exportDir);

        // Store original class loader
        this.originalClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    void tearDown()
    {
        // Restore original class loader
        Thread.currentThread().setContextClassLoader(this.originalClassLoader);
    }

    @Test
    void copyResourceFromJAR() throws Exception
    {
        // Create a JAR with a simple resource
        String resourcePath = "content/file.txt";
        createTestJarWithResources(resourcePath);

        FilesystemResourceReferenceCopier copier = new FilesystemResourceReferenceCopier();
        copier.copyResourceFromJAR(RESOURCE_PREFIX, resourcePath, "webjars/testlib/1.0.0", this.exportContext);

        // Verify the resource was copied
        assertTrue(new File(this.exportDir, "webjars/testlib/1.0.0/content/file.txt").exists());
    }

    @Test
    void copyResourceFromJARDirectory() throws Exception
    {
        // Create a JAR with a file and a compressed file
        createTestJarWithResources("assets/file1.txt", "assets/file1.txt.gz");

        FilesystemResourceReferenceCopier copier = new FilesystemResourceReferenceCopier();
        // When copying a file, actually all files that start with the resource path will be copied.
        copier.copyResourceFromJAR(RESOURCE_PREFIX, "assets/file1.txt", "webjars/testlib/1.0.0", this.exportContext);

        assertTrue(new File(this.exportDir, "webjars/testlib/1.0.0/assets/file1.txt").exists());
        assertTrue(new File(this.exportDir, "webjars/testlib/1.0.0/assets/file1.txt.gz").exists());
    }

    @Test
    void copyResourceFromJARWithPathTraversalAttack() throws Exception
    {
        // Create a JAR with a malicious resource
        String resourcePath = "../../../../file.txt";
        createTestJarWithResources(resourcePath);

        FilesystemResourceReferenceCopier copier = new FilesystemResourceReferenceCopier();
        copier.copyResourceFromJAR(RESOURCE_PREFIX, "../../../../file.txt", "webjars/testlib/1.0.0",
            this.exportContext);

        assertThat(this.logCapture.getMessage(0), containsString("Skipping copying of resource"));
    }

    @ParameterizedTest
    @CsvSource({
        ".icon { background: url(\"../images/icon.png\"); }, css/style.css, images/icon.png",
        ".icon { background: URL('../fonts/font.woff'); }, css/style.css, fonts/font.woff",
        ".icon { background: Url(../assets/bg.svg); }, css/style.css, assets/bg.svg",
        ".icon { background: url(  \"../images/icon.png\"  ); }, css/style.css, images/icon.png",
        ".icon { background: url(\"../fonts/font.woff?v=1.0\"); }, css/style.css, fonts/font.woff",
        ".icon { background: url(\"../fonts/icons.svg#icon-home\"); }, css/style.css, fonts/icons.svg",
        ".icon { background: url('../fonts/icons.svg?v=2.0#glyph'); }, css/style.css, fonts/icons.svg",
        ".icon { background: url(\"icon.png\"); }, css/style.css, css/icon.png",
        ".icon { background: url(\"images/icon.png\"); }, css/style.css, css/images/icon.png",
        ".icon { background: url(\"../../assets/deep/icon.png\"); }, css/sub/style.css, assets/deep/icon.png"
    })
    void processCSSWithSingleUrl(String cssContent, String cssPath, String resourcePath)
        throws Exception
    {
        createTestJarWithCSS(cssContent, cssPath, resourcePath);

        FilesystemResourceReferenceCopier copier = new FilesystemResourceReferenceCopier();
        copier.processCSS(RESOURCE_PREFIX, cssPath, "webjars/testlib/1.0.0", this.exportContext);

        assertTrue(new File(this.exportDir, "webjars/testlib/1.0.0/" + resourcePath).exists());
    }

    @ParameterizedTest
    @ValueSource(strings = { """
            .icon1 { background: url("../images/icon1.png"); }
            .icon2 { background: url('../images/icon2.png'); }
            .icon3 { background: url(../images/icon3.png); }
            """,
        ".icons { background: url(\"../images/icon1.png\"), url('../images/icon2.png'), url(../images/icon3.png); }"
    })
    void processCSSWithMultipleUrls(String cssContent) throws Exception
    {
        String[] paths = {"images/icon1.png", "images/icon2.png", "images/icon3.png"};

        createTestJarWithCSS(cssContent, "css/style.css", paths);

        FilesystemResourceReferenceCopier copier = new FilesystemResourceReferenceCopier();
        copier.processCSS(RESOURCE_PREFIX, "css/style.css", "webjars/testlib/1.0.0", this.exportContext);

        for (String path : paths) {
            assertTrue(new File(this.exportDir, "webjars/testlib/1.0.0/" + path).exists());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ".icon { background: url(\"https://example.com/image.png\"); }",
        ".icon { background: url(\"data:image/png;base64,iVBORw0KGgo=\"); }"
    })
    void processCSSWithIgnoredUrl(String cssContent) throws Exception
    {
        createTestJarWithCSS(cssContent, "css/style.css");

        FilesystemResourceReferenceCopier copier = new FilesystemResourceReferenceCopier();
        copier.processCSS(RESOURCE_PREFIX, "css/style.css", "webjars/testlib/1.0.0", this.exportContext);

        File webjarDir = new File(this.exportDir, "webjars/testlib/1.0.0");
        assertFalse(webjarDir.exists());
    }

    @Test
    void processCSSWithPathTraversalAttack() throws Exception
    {
        // Create a JAR with a malicious CSS file
        String cssContent = ".icon { background: url(\"../../../../file.png\"); }";
        createTestJarWithCSS(cssContent, "css/style.css", "../../../../file.png");

        FilesystemResourceReferenceCopier copier = new FilesystemResourceReferenceCopier();
        copier.processCSS(RESOURCE_PREFIX, "css/style.css", "webjars/testlib/1.0.0", this.exportContext);

        assertThat(this.logCapture.getMessage(0), containsString("Skipping copying of resource"));
    }

    /**
     * Creates a test JAR file with a CSS file and optional resource files, then sets up the class loader to find it.
     */
    private void createTestJarWithCSS(String cssContent, String cssPath, String... resourcePaths) throws Exception
    {
        this.jarFile = new File(this.tmpDir, "test.jar");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(this.jarFile))) {
            // Add the CSS file
            String fullCssPath = RESOURCE_PREFIX + "/" + cssPath;
            jos.putNextEntry(new JarEntry(fullCssPath));
            jos.write(cssContent.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            // Add the resource files referenced by the CSS
            addResources(resourcePaths, jos);
        }

        // Set up a custom class loader that can find resources in our test JAR
        setupTestClassLoader();
    }

    /**
     * Creates a test JAR file with the specified resource files.
     */
    private void createTestJarWithResources(String... resourcePaths) throws Exception
    {
        this.jarFile = new File(this.tmpDir, "test.jar");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(this.jarFile))) {
            addResources(resourcePaths, jos);
        }

        // Set up a custom class loader that can find resources in our test JAR
        setupTestClassLoader();
    }

    private void addResources(String[] resourcePaths, JarOutputStream jos) throws IOException
    {
        for (String resourcePath : resourcePaths) {
            String fullResourcePath = RESOURCE_PREFIX + "/" + resourcePath;
            jos.putNextEntry(new JarEntry(fullResourcePath));
            jos.write(("content of " + resourcePath).getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }

    /**
     * Sets up a custom class loader that includes our test JAR file.
     */
    private void setupTestClassLoader() throws Exception
    {
        URLClassLoader testClassLoader =
            new URLClassLoader(new URL[] {this.jarFile.toURI().toURL()}, this.originalClassLoader);
        Thread.currentThread().setContextClassLoader(testClassLoader);
    }
}
