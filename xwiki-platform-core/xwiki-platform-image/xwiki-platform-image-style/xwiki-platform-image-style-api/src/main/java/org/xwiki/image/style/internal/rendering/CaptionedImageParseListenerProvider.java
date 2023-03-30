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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.ListenerProvider;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.chaining.ChainingListener;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.LookaheadChainingListener;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.syntax.Syntax;

import static org.xwiki.image.style.internal.rendering.CaptionedImageRenderListenerProvider.DATA_XWIKI_IMAGE_STYLE;
import static org.xwiki.image.style.internal.rendering.CaptionedImageRenderListenerProvider.DATA_XWIKI_IMAGE_STYLE_ALIGNMENT;
import static org.xwiki.image.style.internal.rendering.CaptionedImageRenderListenerProvider.DATA_XWIKI_IMAGE_STYLE_BORDER;
import static org.xwiki.image.style.internal.rendering.CaptionedImageRenderListenerProvider.DATA_XWIKI_IMAGE_STYLE_TEXT_WRAP;
import static org.xwiki.image.style.internal.rendering.CaptionedImageRenderListenerProvider.STYLE_PROPERTY;
import static org.xwiki.image.style.internal.rendering.CaptionedImageRenderListenerProvider.WIDTH_PROPERTY;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Provides a parser listener for image with captions, taking into account the image styles.
 *
 * @version $Id$
 * @since 15.3RC1
 * @since 14.10.8
 */
@Component
@Singleton
@Named("captionedImageParse")
public class CaptionedImageParseListenerProvider implements ListenerProvider
{
    private static final Set<Syntax> ACCEPTED_SYNTAX = Set.of(XWIKI_2_0, XWIKI_2_1);

    private static final String STYLE_SEPARATOR = ";";

    private static class InternalChainingListener extends LookaheadChainingListener
    {
        private static final Set<String> KNOWN_PARAMETERS = Set.of(
            WIDTH_PROPERTY,
            DATA_XWIKI_IMAGE_STYLE,
            DATA_XWIKI_IMAGE_STYLE_ALIGNMENT,
            DATA_XWIKI_IMAGE_STYLE_BORDER,
            DATA_XWIKI_IMAGE_STYLE_TEXT_WRAP
        );

        private final Deque<Map<String, String>> figureParametersQueue = new ArrayDeque<>();

        /**
         * Default constructor.
         *
         * @param listenerChain the listener chainer to set for this listener
         */
        protected InternalChainingListener(ListenerChain listenerChain)
        {
            super(listenerChain, 1);
        }

        @Override
        public void beginFigure(Map<String, String> parameters)
        {
            this.figureParametersQueue.push(parameters);
            super.beginFigure(parameters);
        }

        @Override
        public void onImage(ResourceReference reference, boolean freestanding, String id,
            Map<String, String> parameters)
        {
            QueueListener.Event nextEvent = getPreviousEvents().peekLast();
            if (nextEvent != null && nextEvent.eventType == EventType.BEGIN_FIGURE) {
                // Merge the image parameters to the figure parameters when the image is wrapped in a figure.
                Object[] eventParameters = nextEvent.eventParameters;
                // Sanity check to make sure that we are handling the expected case.
                if (eventParameters.length == 1 && eventParameters[0] instanceof Map<?, ?>) {
                    Map<String, String> figureParameters = this.figureParametersQueue.pop();
                    Map<String, String> mergedMap = new HashMap<>(figureParameters);
                    KNOWN_PARAMETERS.stream()
                        .filter(parameters::containsKey)
                        .forEach(key -> mergeParameter(key, mergedMap, parameters, figureParameters));
                    this.figureParametersQueue.push(mergedMap);
                    eventParameters[0] = mergedMap;
                }
            }

            super.onImage(reference, freestanding, id, parameters);
        }

        private void mergeParameter(String key, Map<String, String> mergedMap,
            Map<String, String> imageParameters, Map<String, String> figureParameters)
        {
            String value = imageParameters.get(key);
            if (Objects.equals(key, WIDTH_PROPERTY)) {
                String styleValue = String.format("width: %spx;", imageParameters.get(key));
                if (figureParameters.containsKey(STYLE_PROPERTY)) {
                    if (!figureParameters.get(STYLE_PROPERTY).endsWith(STYLE_SEPARATOR)) {
                        styleValue = STYLE_SEPARATOR + styleValue;
                    }
                    styleValue = figureParameters.get(STYLE_PROPERTY) + styleValue;
                }
                mergedMap.put(STYLE_PROPERTY, styleValue);
            } else {
                mergedMap.put(key, value);
            }
        }

        @Override
        public void endFigure(Map<String, String> parameters)
        {
            super.endFigure(this.figureParametersQueue.pop());
        }
    }

    @Override
    public ChainingListener getListener(ListenerChain listenerChain)
    {
        return new InternalChainingListener(listenerChain);
    }

    @Override
    public boolean accept(String action, Syntax syntax)
    {
        return Objects.equals(action, PARSE_ACTION) && syntax != null && ACCEPTED_SYNTAX.contains(syntax);
    }
}
