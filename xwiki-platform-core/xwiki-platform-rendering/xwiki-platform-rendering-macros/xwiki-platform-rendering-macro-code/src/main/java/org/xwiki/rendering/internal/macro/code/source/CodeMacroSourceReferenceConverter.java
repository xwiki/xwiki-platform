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
package org.xwiki.rendering.internal.macro.code.source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.configuration2.ex.ConversionException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.rendering.macro.code.source.CodeMacroSourceReference;

/**
 * Convert various types (especially String) to {@link CodeMacroSourceReference}.
 * 
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.2
 */
@Component
@Singleton
public class CodeMacroSourceReferenceConverter extends AbstractConverter<CodeMacroSourceReference>
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Override
    protected CodeMacroSourceReference convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        try {
            return convertToType(value);
        } catch (IOException e) {
            throw new ConversionException("Failed to convert to CodeMacroSourceReference", e);
        }
    }

    private CodeMacroSourceReference convertToType(Object value) throws IOException
    {
        CodeMacroSourceReference reference;

        if (value instanceof String) {
            reference = fromString((String) value);
        } else if (value instanceof InputStream) {
            reference = new CodeMacroSourceReference(CodeMacroSourceReference.TYPE_STRING,
                IOUtils.toString((InputStream) value, StandardCharsets.UTF_8));
        } else if (value instanceof byte[]) {
            reference = new CodeMacroSourceReference(CodeMacroSourceReference.TYPE_STRING,
                new String((byte[]) value, StandardCharsets.UTF_8));
        } else if (value instanceof Reader) {
            reference =
                new CodeMacroSourceReference(CodeMacroSourceReference.TYPE_STRING, IOUtils.toString((Reader) value));
        } else if (value instanceof URL) {
            reference = new CodeMacroSourceReference(CodeMacroSourceReference.TYPE_URL, ((URL) value).toExternalForm());
        } else if (value instanceof File) {
            reference =
                new CodeMacroSourceReference(CodeMacroSourceReference.TYPE_FILE, ((File) value).getAbsolutePath());
        } else {
            reference = fromUnknownType(value);
        }

        return reference;
    }

    private CodeMacroSourceReference fromUnknownType(Object value)
    {
        ParameterizedType componentRole = TypeUtils.parameterize(
            org.xwiki.rendering.macro.code.source.CodeMacroSourceReferenceConverter.class, value.getClass());

        ComponentManager componentManager = this.contextComponentManagerProvider.get();

        if (componentManager.hasComponent(componentRole)) {
            try {
                org.xwiki.rendering.macro.code.source.CodeMacroSourceReferenceConverter converter =
                    componentManager.getInstance(componentRole);

                return converter.convert(value);
            } catch (ComponentLookupException e) {
                throw new ConversionException(
                    "Failed to get the code macro source reference converter component for type [" + value.getClass()
                        + "]",
                    e);
            }
        }

        // Fallback on the String logic
        return fromString(value.toString());
    }

    private CodeMacroSourceReference fromString(String source)
    {
        int index = source.indexOf(':');

        return new CodeMacroSourceReference(
            index <= 0 ? CodeMacroSourceReference.TYPE_STRING : source.substring(0, index),
            index < source.length() ? source.substring(index + 1) : "");
    }
}
