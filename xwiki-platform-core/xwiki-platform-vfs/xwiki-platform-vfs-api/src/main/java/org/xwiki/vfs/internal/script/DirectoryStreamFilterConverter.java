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
import java.nio.file.DirectoryStream;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;

/**
 * Converts from {@link String} to {@link java.nio.file.DirectoryStream.Filter} by looking at Component implementations
 * of the {@link java.nio.file.DirectoryStream.Filter} role with the passed string as the component hint. This makes it
 * easy to call the VFS module scripting API from Velocity for example.
 * <p>
 * For example the following would list all entries in the referenced zip which are of type File:
 * <code><pre>
 * #set ($ds = $services.vfs.getPaths("attach:Sandbox.WebHome@my.zip", "/", "File"))
 * ...
 * $ds.close()
 * </pre></code>
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Singleton
public class DirectoryStreamFilterConverter extends AbstractConverter<DirectoryStream.Filter>
{
    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    protected DirectoryStream.Filter convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        DirectoryStream.Filter filter;

        try {
            filter = this.componentManagerProvider.get().getInstance(DirectoryStream.Filter.class, value.toString());
        } catch (Exception e) {
            throw new ConversionException(
                String.format("Failed to convert [%s] to [%s]", value, DirectoryStream.Filter.class.getName()), e);
        }

        return filter;
    }
}
