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

import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsPermissionChecker;
import org.xwiki.vfs.VfsResourceReference;

import net.java.truevfs.access.TPath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PathConverter}.
 *
 * @version $Id$
 * @since 7.4M2
 */
@ComponentTest
class PathConverterTest
{
    @InjectMockComponents
    private PathConverter converter;

    @MockComponent
    @Named("truevfs")
    private ResourceReferenceSerializer<VfsResourceReference, URI> serializer;

    @MockComponent
    @Named("cascading")
    private VfsPermissionChecker permissionChecker;

    @Test
    void convertWhenOk() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("attach:Sandbox.WebHome@my.zip"), "a/b/c");
        when(this.serializer.serialize(reference)).thenReturn(URI.create("attach://xwiki:Sandbox.WebHome/my.zip/a/b/c"));

        Path path = this.converter.convert(new DefaultParameterizedType(null, Path.class),
            "attach:Sandbox.WebHome@my.zip/a/b/c");
        assertEquals("attach://xwiki:Sandbox.WebHome/my.zip/a/b/c", path.toString());
        assertEquals(TPath.class.getName(), path.getClass().getName());
    }

    @Test
    void convertWhenError() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("attach:Sandbox.WebHome@my.zip"), "a/b/c");
        when(this.serializer.serialize(reference)).thenThrow(new SerializeResourceReferenceException("error"));

        ConversionException exception = assertThrows(ConversionException.class,
            () -> this.converter.convert(new DefaultParameterizedType(null, Path.class),
                "attach:Sandbox.WebHome@my.zip/a/b/c"));
        assertEquals("Failed to convert [attach:Sandbox.WebHome@my.zip/a/b/c] to a Path object",
            exception.getMessage());
    }

    @Test
    void convertWhenNoPermission() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(URI.create("attach:Sandbox.WebHome@my.zip"), "a/b/c");
        when(this.serializer.serialize(reference)).thenReturn(URI.create("attach://xwiki:Sandbox.WebHome/my.zip/a/b/c"));

        doThrow(new VfsException("unauthorized")).when(this.permissionChecker).checkPermission(reference);

        ConversionException exception = assertThrows(ConversionException.class,
            () -> this.converter.convert(new DefaultParameterizedType(null, Path.class),
                "attach:Sandbox.WebHome@my.zip/a/b/c"));
        assertEquals("Failed to convert [attach:Sandbox.WebHome@my.zip/a/b/c] to a Path object",
            exception.getMessage());
        assertEquals("VfsException: unauthorized", ExceptionUtils.getRootCauseMessage(exception));
    }
}
