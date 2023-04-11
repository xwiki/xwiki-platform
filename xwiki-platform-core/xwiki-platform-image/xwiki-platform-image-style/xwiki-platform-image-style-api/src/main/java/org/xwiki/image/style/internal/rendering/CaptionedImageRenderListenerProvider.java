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
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.ListenerProvider;
import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.chaining.ChainingListener;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.LookaheadChainingListener;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.syntax.Syntax;

import static org.xwiki.rendering.syntax.Syntax.HTML_5_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Provides a renderer listener for image with captions, taking into account the image styles.
 *
 * @version $Id$
 * @since 15.3RC1
 * @since 14.10.8
 */
@Component
@Singleton
@Named("captionedImageRender")
public class CaptionedImageRenderListenerProvider implements ListenerProvider
{
    static final String WIDTH_PROPERTY = "width";

    static final String STYLE_PROPERTY = "style";

    static final String STYLE_SEPARATOR = ";";

    static final String DATA_XWIKI_IMAGE_STYLE = "data-xwiki-image-style";

    static final String DATA_XWIKI_IMAGE_STYLE_ALIGNMENT = "data-xwiki-image-style-alignment";

    static final String DATA_XWIKI_IMAGE_STYLE_BORDER = "data-xwiki-image-style-border";

    static final String DATA_XWIKI_IMAGE_STYLE_TEXT_WRAP = "data-xwiki-image-style-text-wrap";

    private static final List<Syntax> ACCEPTED_SYNTAX = List.of(HTML_5_0, XWIKI_2_0, XWIKI_2_1);

    static final Set<String> KNOWN_PARAMETERS = Set.of(
        WIDTH_PROPERTY,
        STYLE_PROPERTY,
        DATA_XWIKI_IMAGE_STYLE,
        DATA_XWIKI_IMAGE_STYLE_ALIGNMENT,
        DATA_XWIKI_IMAGE_STYLE_BORDER,
        DATA_XWIKI_IMAGE_STYLE_TEXT_WRAP
    );

    private static class InternalChainingListener extends LookaheadChainingListener
    {
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
                    Map<String, String> figureParameters = new LinkedHashMap<>(this.figureParametersQueue.pop());
                    cleanupFigureParameters(figureParameters, parameters);
                    this.figureParametersQueue.push(figureParameters);
                    eventParameters[0] = figureParameters;
                }
            }

            super.onImage(reference, freestanding, id, parameters);
        }

        private void cleanupFigureParameters(Map<String, String> figureParameters, Map<String, String> imageParameters)
        {
            KNOWN_PARAMETERS.forEach(key -> {
                if (key.equals(STYLE_PROPERTY) && figureParameters.containsKey(STYLE_PROPERTY)
                    && imageParameters.containsKey(WIDTH_PROPERTY))
                {
                    String figureStyle = figureParameters.get(STYLE_PROPERTY);
                    String cleanupStyle =
                        Arrays.stream(figureStyle.split(STYLE_SEPARATOR))
                            .filter(Predicate.not(value -> Objects.equals(value.trim(),
                                String.format("width: %spx", imageParameters.get(WIDTH_PROPERTY)))))
                            .collect(Collectors.joining(STYLE_SEPARATOR, StringUtils.EMPTY, STYLE_SEPARATOR));

                    if (cleanupStyle.equals(STYLE_SEPARATOR)) {
                        cleanupStyle = "";
                    }
                    if (cleanupStyle.isEmpty()) {
                        figureParameters.remove(key);
                    } else {
                        figureParameters.put(key, cleanupStyle);
                    }
                } else if (!key.equals(STYLE_PROPERTY)) {
                    figureParameters.remove(key);
                }
            });
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
        return Objects.equals(action, RENDER_ACTION) && syntax != null && ACCEPTED_SYNTAX.contains(syntax);
    }
}
