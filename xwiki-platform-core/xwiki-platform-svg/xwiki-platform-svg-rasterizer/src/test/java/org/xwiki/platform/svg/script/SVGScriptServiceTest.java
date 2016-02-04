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
package org.xwiki.platform.svg.script;

import java.io.IOException;
import java.lang.reflect.Type;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.svg.SVGRasterizer;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link SVGScriptService} component.
 *
 * @version $Id$
 * @since 8.0M1
 */
public class SVGScriptServiceTest
{
    private static final String SVG = "<svg>";

    @Rule
    public final MockitoComponentMockingRule<SVGScriptService> mocker =
        new MockitoComponentMockingRule<>(SVGScriptService.class);

    @Mock
    private TemporaryResourceReference tref;

    @Mock
    private DocumentReference dref;

    @Mock
    private ExtendedURL eurl;

    private SVGRasterizer internal;

    private ResourceReferenceSerializer<TemporaryResourceReference, ExtendedURL> serializer;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.internal = this.mocker.getInstance(SVGRasterizer.class);
        when(this.internal.rasterizeToTemporaryResource(SVG, 0, 0)).thenReturn(this.tref);
        when(this.internal.rasterizeToTemporaryResource(SVG, 100, 200)).thenReturn(this.tref);
        when(this.internal.rasterizeToTemporaryResource(SVG, 0, 0, this.dref)).thenReturn(this.tref);
        when(this.internal.rasterizeToTemporaryResource(SVG, 100, 200, this.dref)).thenReturn(this.tref);

        Type stype = new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
            TemporaryResourceReference.class, ExtendedURL.class);
        this.serializer = this.mocker.getInstance(stype, "standard/tmp");
        when(this.serializer.serialize(this.tref)).thenReturn(this.eurl);
    }

    @Test
    public void rasterizeToTemporaryResource1PForwardsCallsAndNormalizesResult() throws Exception
    {
        Assert.assertSame(this.eurl, this.mocker.getComponentUnderTest().rasterizeToTemporaryResource(SVG));
    }

    @Test
    public void rasterizeToTemporaryResource1PCatchesExceptionsAndReturnsNull() throws Exception
    {
        when(this.internal.rasterizeToTemporaryResource(SVG, 0, 0)).thenThrow(new NullPointerException());
        Assert.assertNull(this.mocker.getComponentUnderTest().rasterizeToTemporaryResource(SVG));
    }

    @Test
    public void rasterizeToTemporaryResource2PForwardsCallsAndNormalizesResult() throws Exception
    {
        Assert.assertSame(this.eurl, this.mocker.getComponentUnderTest().rasterizeToTemporaryResource(SVG, this.dref));
    }

    @Test
    public void rasterizeToTemporaryResource2PCatchesExceptionsAndReturnsNull() throws Exception
    {
        when(this.internal.rasterizeToTemporaryResource(SVG, 0, 0, this.dref)).thenThrow(new NullPointerException());
        Assert.assertNull(this.mocker.getComponentUnderTest().rasterizeToTemporaryResource(SVG, this.dref));
    }

    @Test
    public void rasterizeToTemporaryResource3PForwardsCallsAndNormalizesResult() throws Exception
    {
        Assert.assertSame(this.eurl, this.mocker.getComponentUnderTest().rasterizeToTemporaryResource(SVG, 100, 200));
    }

    @Test
    public void rasterizeToTemporaryResource3PCatchesExceptionsAndReturnsNull() throws Exception
    {
        when(this.internal.rasterizeToTemporaryResource(SVG, 100, 200)).thenThrow(new NullPointerException());
        Assert.assertNull(this.mocker.getComponentUnderTest().rasterizeToTemporaryResource(SVG, 100, 200));
    }

    @Test
    public void rasterizeToTemporaryResource4PForwardsCallsAndNormalizesResult() throws Exception
    {
        Assert.assertSame(this.eurl,
            this.mocker.getComponentUnderTest().rasterizeToTemporaryResource(SVG, 100, 200, this.dref));
    }

    @Test
    public void rasterizeToTemporaryResource4PCatchesExceptionsAndReturnsNull() throws Exception
    {
        when(this.internal.rasterizeToTemporaryResource(SVG, 100, 200, this.dref))
            .thenThrow(new NullPointerException());
        Assert.assertNull(this.mocker.getComponentUnderTest().rasterizeToTemporaryResource(SVG, 100, 200, this.dref));
    }

    @Test
    public void rasterizeToResponseForwardsCalls() throws Exception
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().rasterizeToResponse(SVG));
    }

    @Test
    public void rasterizeToResponseCatchesExceptions() throws Exception
    {
        Mockito.doThrow(new IOException()).when(this.internal).rasterizeToResponse(SVG, 0, 0);
        Assert.assertFalse(this.mocker.getComponentUnderTest().rasterizeToResponse(SVG));
    }

    @Test
    public void rasterizeToResponseWithSizeForwardsCalls() throws Exception
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().rasterizeToResponse(SVG, 100, 200));
    }

    @Test
    public void rasterizeToResponseWithSizeCatchesExceptions() throws Exception
    {
        Mockito.doThrow(new IOException()).when(this.internal).rasterizeToResponse(SVG, 100, 200);
        Assert.assertFalse(this.mocker.getComponentUnderTest().rasterizeToResponse(SVG, 100, 200));
    }
}
