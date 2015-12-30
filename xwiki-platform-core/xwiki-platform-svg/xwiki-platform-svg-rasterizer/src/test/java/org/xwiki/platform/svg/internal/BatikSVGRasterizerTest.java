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
package org.xwiki.platform.svg.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.xwiki.container.Container;
import org.xwiki.container.Response;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.platform.svg.SVGRasterizer;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PatientDataScriptService} component.
 *
 * @version $Id$
 * @since 1.0M1
 */
public class BatikSVGRasterizerTest
{
    private static final String SVG =
        "<svg xmlns='http://www.w3.org/2000/svg'><ellipse cx='50' cy='100' rx='25' ry='50'/></svg>";

    private static final String RASTER_FILE_NAME = Math.abs(SVG.hashCode()) + ".png";

    private static final File RASTER_FILE =
        new File(System.getProperty("java.io.tmpdir") + "/temp/svg/wiki/Space/Document/" + RASTER_FILE_NAME);

    private static final File TEMPORARY_FILE =
        new File(System.getProperty("java.io.tmpdir") + "/temp/svg/" + RASTER_FILE_NAME);

    private static final String TEMPORARY_FILE_PATH =
        TEMPORARY_FILE.getAbsolutePath();

    @Rule
    public final MockitoComponentMockingRule<SVGRasterizer> mocker =
        new MockitoComponentMockingRule<SVGRasterizer>(BatikSVGRasterizer.class);

    private DocumentReference dref = new DocumentReference("wiki", "Space", "Document");

    private Environment environment;

    private DocumentReferenceResolver<String> resolver;

    private Container container;

    @Mock
    private ServletResponse sresponse;

    @Mock
    private HttpServletResponse hsresponse;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        FileUtils.deleteQuietly(RASTER_FILE);
        FileUtils.deleteQuietly(TEMPORARY_FILE);

        this.environment = this.mocker.getInstance(Environment.class);
        when(this.environment.getTemporaryDirectory()).thenReturn(new File(System.getProperty("java.io.tmpdir")));

        this.resolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(this.resolver.resolve("")).thenReturn(this.dref);

        this.container = this.mocker.getInstance(Container.class);
        when(this.container.getResponse()).thenReturn(this.sresponse);
        when(this.sresponse.getHttpServletResponse()).thenReturn(this.hsresponse);
    }

    @Test
    public void rasterizeToTemporaryFileCreatesTemporaryFile() throws Exception
    {
        File tfile =
            this.mocker.getComponentUnderTest().rasterizeToTemporaryFile(SVG, 100, 200);
        Assert.assertEquals(TEMPORARY_FILE_PATH, tfile.getAbsolutePath());
        Assert.assertTrue(tfile.exists());
        Assert.assertTrue(isPNG(tfile));
    }

    @Test
    public void rasterizeToTemporaryFileReusesFile() throws Exception
    {
        writeTestFile(TEMPORARY_FILE);
        File tfile =
            this.mocker.getComponentUnderTest().rasterizeToTemporaryFile(SVG, 100, 200);
        Assert.assertEquals(TEMPORARY_FILE_PATH, tfile.getAbsolutePath());
        Assert.assertTrue(tfile.exists());
        Assert.assertTrue(isTestFile(tfile));
    }

    @Test
    public void rasterizeToTemporaryFileReturnsNullOnExceptions() throws Exception
    {
        File tfile =
            this.mocker.getComponentUnderTest().rasterizeToTemporaryFile("<bad>svg!", 0, 0);
        Assert.assertNull(tfile);
    }

    @Test
    public void rasterizeToTemporaryResourceUsesContextDocument() throws Exception
    {
        TemporaryResourceReference tref =
            this.mocker.getComponentUnderTest().rasterizeToTemporaryResource(SVG, 100, 200);
        Assert.assertEquals("svg", tref.getModuleId());
        Assert.assertTrue(tref.getParameters().isEmpty());
        Assert.assertEquals(this.dref, tref.getOwningEntityReference());
        Assert.assertEquals(RASTER_FILE_NAME, tref.getResourceName());
    }

    @Test
    public void rasterizeToTemporaryResourceReusesFile() throws Exception
    {
        writeTestFile(RASTER_FILE);
        TemporaryResourceReference tref =
            this.mocker.getComponentUnderTest().rasterizeToTemporaryResource(SVG, 100, 200);
        Assert.assertEquals("svg", tref.getModuleId());
        Assert.assertTrue(tref.getParameters().isEmpty());
        Assert.assertEquals(this.dref, tref.getOwningEntityReference());
        Assert.assertEquals(Math.abs(SVG.hashCode()) + ".png", tref.getResourceName());
        Assert.assertTrue(isTestFile(RASTER_FILE));
    }

    @Test
    public void rasterizeToTemporaryResourceReturnsNullOnExceptions() throws Exception
    {
        TemporaryResourceReference tref =
            this.mocker.getComponentUnderTest().rasterizeToTemporaryResource("<bad>svg!", 0, 0);
        Assert.assertNull(tref);
    }

    @Test
    public void rasterizeToResponseWritesImageToServletOutputStream() throws Exception
    {
        CapturingOutputStream out = new CapturingOutputStream();
        when(this.hsresponse.getOutputStream()).thenReturn(out);
        this.mocker.getComponentUnderTest().rasterizeToResponse(SVG, 100, 200);
        Assert.assertTrue(out.out.size() > 0);
        Assert.assertArrayEquals("PNG".getBytes("UTF-8"), Arrays.copyOfRange(out.out.toByteArray(), 1, 4));
    }

    @Test
    public void rasterizeToResponseDoesNothingIfNotHttpResponse() throws Exception
    {
        Response r = Mockito.mock(Response.class);
        when(this.container.getResponse()).thenReturn(r);
        this.mocker.getComponentUnderTest().rasterizeToResponse(SVG, 100, 200);
        Mockito.verifyZeroInteractions(r);
    }

    @Test
    public void rasterizeToResponseDoesNothingOnExceptions() throws Exception
    {
        CapturingOutputStream out = new CapturingOutputStream();
        when(this.hsresponse.getOutputStream()).thenReturn(out);
        this.mocker.getComponentUnderTest().rasterizeToResponse("<bad>svg!", 0, 0);
        Assert.assertEquals(0, out.out.size());
    }

    @After
    public void cleanup()
    {
        FileUtils.deleteQuietly(RASTER_FILE);
        FileUtils.deleteQuietly(TEMPORARY_FILE);
    }

    @AfterClass
    public static void cleanupTempDir() throws IOException
    {
        FileUtils.forceDelete(TEMPORARY_FILE.getParentFile().getParentFile());
    }

    private boolean isPNG(File file)
    {
        try (InputStream in = new FileInputStream(file)) {
            byte[] expected = "PNG".getBytes("UTF-8");
            byte[] actual = new byte[3];
            in.read();
            Assert.assertEquals(3, in.read(actual, 0, 3));
            Assert.assertArrayEquals(expected, actual);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isTestFile(File file)
    {
        try (InputStream in = new FileInputStream(file)) {
            byte[] expected = "test".getBytes("UTF-8");
            byte[] actual = new byte[4];
            Assert.assertEquals(4, in.read(actual, 0, 4));
            Assert.assertArrayEquals(expected, actual);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean writeTestFile(File file) throws IOException
    {
        FileUtils.forceMkdir(file.getParentFile());
        try (OutputStream out = new FileOutputStream(file)) {
            IOUtils.write("test", out);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private class CapturingOutputStream extends ServletOutputStream
    {
        private ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException
        {
            this.out.write(b);
        }
    }
}
