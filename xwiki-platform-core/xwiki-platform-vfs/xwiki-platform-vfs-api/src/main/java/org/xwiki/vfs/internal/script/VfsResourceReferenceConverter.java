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

import java.lang.reflect.Type;
import java.net.URI;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Converts {@link String} into {@link VfsResourceReference} objects.
 * <p>
 * Example:
 * <ul>
 *   <li>Input: {@code attach:Sandbox.WebHome@my.zip/some/path}</li>
 *   <li>Output: uri = {@code attach:Sandbox.WebHome@my.zip}, path = {@code some/path}</li>
 * </ul>
 * Note that if the input contains "/" characters they need to be URL-encoded.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Singleton
public class VfsResourceReferenceConverter extends AbstractConverter<VfsResourceReference>
{
    @Override
    protected VfsResourceReference convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        VfsResourceReference reference;

        try {
            reference = new VfsResourceReference(new URI(value.toString()));
        } catch (Exception e) {
            throw new ConversionException(
                String.format("Failed to convert [%s] to a VFS Resource Reference", value), e);
        }

        return reference;
    }
}
