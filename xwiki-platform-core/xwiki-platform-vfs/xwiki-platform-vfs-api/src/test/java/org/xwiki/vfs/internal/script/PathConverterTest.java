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
package org.xwiki.vfs.internal.script;

import java.net.URI;
import java.nio.file.Path;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsPermissionChecker;
import org.xwiki.vfs.VfsResourceReference;

import net.java.truevfs.access.TPath;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PathConverter}.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class PathConverterTest
{
    @Rule
    public MockitoComponentMockingRule<PathConverter> mocker =
        new MockitoComponentMockingRule<>(PathConverter.class);

    @Test
    public void convertWhenOk() throws Exception
    {
        ResourceReferenceSerializer<VfsResourceReference, URI> serializer = this.mocker.getInstance(
            new DefaultParameterizedType(null, ResourceReferenceSerializer.class, VfsResourceReference.class,
                URI.class), "truevfs");
        VfsResourceReference reference = new VfsResourceReference(URI.create("attach:Sandbox.WebHome@my.zip"), "a/b/c");
        when(serializer.serialize(reference)).thenReturn(URI.create("attach://xwiki:Sandbox.WebHome/my.zip/a/b/c"));

        Path path = this.mocker.getComponentUnderTest().convert(new DefaultParameterizedType(null, Path.class),
            "attach:Sandbox.WebHome@my.zip/a/b/c");
        assertEquals("attach://xwiki:Sandbox.WebHome/my.zip/a/b/c", path.toString());
        assertEquals(TPath.class.getName(), path.getClass().getName());
    }

    @Test
    public void convertWhenError() throws Exception
    {
        ResourceReferenceSerializer<VfsResourceReference, URI> serializer = this.mocker.getInstance(
            new DefaultParameterizedType(null, ResourceReferenceSerializer.class, VfsResourceReference.class,
                URI.class), "truevfs");
        VfsResourceReference reference = new VfsResourceReference(URI.create("attach:Sandbox.WebHome@my.zip"), "a/b/c");
        when(serializer.serialize(reference)).thenThrow(new SerializeResourceReferenceException("error"));

        try {
            this.mocker.getComponentUnderTest().convert(new DefaultParameterizedType(null, Path.class),
                "attach:Sandbox.WebHome@my.zip/a/b/c");
            fail("Should have thrown an exception here");
        } catch (ConversionException expected) {
            assertEquals("Failed to convert [attach:Sandbox.WebHome@my.zip/a/b/c] to a Path object",
                expected.getMessage());
        }
    }

    @Test
    public void convertWhenNoPermission() throws Exception
    {
        ResourceReferenceSerializer<VfsResourceReference, URI> serializer = this.mocker.getInstance(
            new DefaultParameterizedType(null, ResourceReferenceSerializer.class, VfsResourceReference.class,
                URI.class), "truevfs");
        VfsResourceReference reference = new VfsResourceReference(URI.create("attach:Sandbox.WebHome@my.zip"), "a/b/c");
        when(serializer.serialize(reference)).thenReturn(URI.create("attach://xwiki:Sandbox.WebHome/my.zip/a/b/c"));

        VfsPermissionChecker permissionChecker = this.mocker.getInstance(VfsPermissionChecker.class, "cascading");
        doThrow(new VfsException("unauthorized")).when(permissionChecker).checkPermission(reference);

        try {
            this.mocker.getComponentUnderTest().convert(new DefaultParameterizedType(null, Path.class),
                "attach:Sandbox.WebHome@my.zip/a/b/c");
            fail("Should have thrown an exception here");
        } catch (ConversionException expected) {
            assertEquals("Failed to convert [attach:Sandbox.WebHome@my.zip/a/b/c] to a Path object",
                expected.getMessage());
            assertEquals("VfsException: unauthorized", ExceptionUtils.getRootCauseMessage(expected));
        }
    }
}
