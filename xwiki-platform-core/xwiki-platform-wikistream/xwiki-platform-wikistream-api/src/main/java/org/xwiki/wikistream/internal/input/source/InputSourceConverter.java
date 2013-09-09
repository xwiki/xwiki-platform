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
package org.xwiki.wikistream.internal.input.source;

import java.lang.reflect.Type;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.wikistream.input.source.InputSource;

@Component
@Singleton
public class InputSourceConverter extends AbstractConverter<InputSource>
{
    @Inject
    private ResourceReferenceParser resourceReferenceParser;

    @Override
    protected <G extends InputSource> G convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        if (value instanceof InputSource) {
            return (G) value;
        }

        String source = value.toString();

        ResourceReference resourceReference = this.resourceReferenceParser.parse(source);

        InputSource inputSource;

        // TODO: use some InputSourceParser instead to make it extensible
        if (resourceReference.getType().equals(ResourceType.URL)) {
            try {
                inputSource = new DefaultURLInputSource(new URL(source));
            } catch (Exception e) {
                throw new ConversionException("Failed to create input source for URL [" + source + "]", e);
            }
        } else {
            inputSource = new StringInputSource(source);
        }

        return (G) inputSource;
    }
}
