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
import java.util.Map;

import org.xwiki.rendering.listener.QueueListener;
import org.xwiki.rendering.listener.chaining.EventType;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.listener.chaining.LookaheadChainingListener;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Base class for chaining listeners that handle captioned images.
 *
 * @version $Id$
 * @since 14.10.13
 * @since 15.5RC1
 */
abstract class AbstractCaptionedImageChainingListener extends LookaheadChainingListener
{
    static final String DATA_XWIKI_IMAGE_STYLE_TEXT_WRAP = "data-xwiki-image-style-text-wrap";

    static final String WIDTH_PROPERTY = "width";

    static final String STYLE_PROPERTY = "style";

    static final String STYLE_SEPARATOR = ";";

    static final String DATA_XWIKI_IMAGE_STYLE = "data-xwiki-image-style";

    static final String DATA_XWIKI_IMAGE_STYLE_ALIGNMENT = "data-xwiki-image-style-alignment";

    static final String DATA_XWIKI_IMAGE_STYLE_BORDER = "data-xwiki-image-style-border";

    protected final Deque<Map<String, String>> figureParametersQueue = new ArrayDeque<>();

    protected AbstractCaptionedImageChainingListener(ListenerChain listenerChain)
    {
        super(listenerChain, 2);
    }

    @Override
    public void beginFigure(Map<String, String> parameters)
    {
        this.figureParametersQueue.push(parameters);
        super.beginFigure(parameters);
    }

    @Override
    public void onImage(ResourceReference reference, boolean freestanding, String id, Map<String, String> parameters)
    {
        QueueListener.Event figureEvent = getPreviousFigureEvent();
        if (figureEvent != null) {
            // Merge the image parameters to the figure parameters when the image is wrapped in a figure.
            Object[] eventParameters = figureEvent.eventParameters;
            // Sanity check to make sure that we are handling the expected case.
            if (eventParameters.length == 1 && eventParameters[0] instanceof Map<?, ?>) {
                Map<String, String> figureParameters = this.figureParametersQueue.pop();
                Map<String, String> updatedFigureParameters = updateFigureParameters(figureParameters, parameters);
                this.figureParametersQueue.push(updatedFigureParameters);
                eventParameters[0] = updatedFigureParameters;
            }
        }

        super.onImage(reference, freestanding, id, parameters);
    }

    @Override
    public void endFigure(Map<String, String> parameters)
    {
        super.endFigure(this.figureParametersQueue.pop());
    }

    protected abstract Map<String, String> updateFigureParameters(Map<String, String> figureParameters,
        Map<String, String> imageParameters);

    private QueueListener.Event getPreviousFigureEvent()
    {
        QueueListener.Event result = null;

        QueueListener.Event candidate = getPreviousEvents().peekLast();
        if (candidate != null && candidate.eventType == EventType.BEGIN_LINK) {
            result = candidate;
            candidate = getPreviousEvents().get(getPreviousEvents().size() - 2);
        }
        if (candidate != null && candidate.eventType == EventType.BEGIN_FIGURE) {
            result = candidate;
        }

        return result;
    }
}
