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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.xwiki.rendering.listener.chaining.ListenerChain;

import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * A listener that puts the image parameters in the figure parameters when the image is wrapped in a figure.
 *
 * @version $Id$
 * @since 14.10.13
 * @since 15.5RC1
 */
class CaptionedImageParseChainingListener extends AbstractCaptionedImageChainingListener
{
    private static final Set<String> KNOWN_PARAMETERS = Set.of(
        WIDTH_PROPERTY,
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
    protected CaptionedImageParseChainingListener(ListenerChain listenerChain)
    {
        super(listenerChain);
    }

    protected Map<String, String> updateFigureParameters(Map<String, String> figureParameters,
        Map<String, String> imageParameters)
    {
        Map<String, String> mergedMap = new LinkedHashMap<>(figureParameters);
        KNOWN_PARAMETERS.stream()
            .filter(imageParameters::containsKey)
            .forEach(key -> mergeParameter(key, mergedMap, imageParameters, figureParameters));
        return mergedMap;
    }

    private void mergeParameter(String key, Map<String, String> mergedMap,
        Map<String, String> imageParameters, Map<String, String> figureParameters)
    {
        String imageParameter = imageParameters.get(key);
        if (Objects.equals(key, WIDTH_PROPERTY)) {
            mergeWidthParameter(mergedMap, figureParameters, imageParameters.get(WIDTH_PROPERTY));
        } else {
            mergedMap.put(key, imageParameter);
        }
    }

    private void mergeWidthParameter(Map<String, String> mergedMap, Map<String, String> figureParameters,
        String imageWidthParameter)
    {
        String styleValue = String.format("width: %spx;", imageWidthParameter);
        if (figureParameters.containsKey(STYLE_PROPERTY)) {
            if (figureParameters.get(STYLE_PROPERTY).contains("width:")) {
                styleValue = figureParameters.get(STYLE_PROPERTY);
            } else {
                if (!figureParameters.get(STYLE_PROPERTY).endsWith(STYLE_SEPARATOR)) {
                    styleValue = STYLE_SEPARATOR + SPACE + styleValue;
                } else {
                    styleValue = SPACE + styleValue;
                }
                styleValue = figureParameters.get(STYLE_PROPERTY) + styleValue;
            }
        }
        mergedMap.put(STYLE_PROPERTY, styleValue);
    }
}
