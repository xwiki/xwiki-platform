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
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * TODO.
 *
 * @version $Id$
 * @since x.y.z
 */
@Component
@Singleton
@Named("tmpparse")
public class CaptionedImageParseListenerProvider implements ListenerProvider
{
    private static final String WIDTH_PROPERTY = "width";

    private static final String STYLE_PROPERTY = "style";

    private static final List<Syntax> ACCEPTED_SYNTAX = List.of(XWIKI_2_0, XWIKI_2_1);

    private static final String STYLE_SEPARATOR = ";";

    private static class InternalChainingListener extends LookaheadChainingListener
    {
        private static final List<String> KNOWN_PARAMETERS = List.of(
            WIDTH_PROPERTY,
            // TODO: reuse constant if it exists
            "data-xwiki-image-style",
            "data-xwiki-image-style-alignment",
            "data-xwiki-image-style-border",
            "data-xwiki-image-style-text-wrap"
        );

        private final Deque<Map<String, String>> figureParametersQueue = new ArrayDeque<>();

        /**
         * TODO.
         *
         * @param listenerChain TODO
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
                // modify figure parameters
                Object[] eventParameters = nextEvent.eventParameters;
                // Note: sanity check to make sure that we are handling the expected case.
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
