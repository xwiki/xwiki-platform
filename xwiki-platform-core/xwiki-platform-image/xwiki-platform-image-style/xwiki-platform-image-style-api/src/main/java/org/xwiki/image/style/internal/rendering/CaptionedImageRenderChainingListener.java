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
package org.xwiki.image.style.internal.rendering;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.rendering.listener.chaining.ListenerChain;

/**
 * A listener that cleans the image parameters from the figure when rendering to XWiki syntax.
 *
 * @version $Id$
 * @since 14.10.13
 * @since 15.5RC1
 */
class CaptionedImageRenderChainingListener extends AbstractCaptionedImageChainingListener
{
    static final Set<String> KNOWN_PARAMETERS = Set.of(
        WIDTH_PROPERTY,
        STYLE_PROPERTY,
        DATA_XWIKI_IMAGE_STYLE,
        DATA_XWIKI_IMAGE_STYLE_ALIGNMENT,
        DATA_XWIKI_IMAGE_STYLE_BORDER,
        DATA_XWIKI_IMAGE_STYLE_TEXT_WRAP
    );

    /**
     * Default constructor.
     *
     * @param listenerChain the listener chainer to set for this listener
     */
    protected CaptionedImageRenderChainingListener(ListenerChain listenerChain)
    {
        super(listenerChain);
    }

    @Override
    protected Map<String, String> updateFigureParameters(Map<String, String> figureParameters,
        Map<String, String> imageParameters)
    {
        Map<String, String> updatedFigureParameters = new LinkedHashMap<>(figureParameters);
        KNOWN_PARAMETERS.forEach(key -> {
            if (key.equals(STYLE_PROPERTY) && updatedFigureParameters.containsKey(
                STYLE_PROPERTY)
                && imageParameters.containsKey(WIDTH_PROPERTY))
            {
                String figureStyle = updatedFigureParameters.get(STYLE_PROPERTY);
                String cleanupStyle =
                    Arrays.stream(figureStyle.split(STYLE_SEPARATOR))
                        .filter(Predicate.not(value -> Objects.equals(value.trim(),
                            String.format("width: %spx", imageParameters.get(
                                WIDTH_PROPERTY)))))
                        .collect(
                            Collectors.joining(STYLE_SEPARATOR, StringUtils.EMPTY,
                                STYLE_SEPARATOR));

                if (cleanupStyle.equals(STYLE_SEPARATOR)) {
                    cleanupStyle = "";
                }
                if (cleanupStyle.isEmpty()) {
                    updatedFigureParameters.remove(key);
                } else {
                    updatedFigureParameters.put(key, cleanupStyle);
                }
            } else if (!key.equals(STYLE_PROPERTY)) {
                updatedFigureParameters.remove(key);
            }
        });

        return updatedFigureParameters;
    }
}
