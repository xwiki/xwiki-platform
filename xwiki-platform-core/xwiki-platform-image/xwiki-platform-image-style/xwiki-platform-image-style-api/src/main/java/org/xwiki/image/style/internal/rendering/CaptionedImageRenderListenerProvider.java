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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.ListenerProvider;
import org.xwiki.rendering.listener.chaining.AbstractChainingListener;
import org.xwiki.rendering.listener.chaining.ChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
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

    private static class InternalChainingListener extends AbstractChainingListener
    {
        private Map<String, String> cleanedUpParameters;

        /**
         * Default constructor.
         *
         * @param listenerChain the listener chainer to set for this listener
         */
        protected InternalChainingListener(ListenerChain listenerChain)
        {
            setListenerChain(listenerChain);
        }

        @Override
        public void beginFigure(Map<String, String> parameters)
        {
            this.cleanedUpParameters = new HashMap<>(parameters);
            KNOWN_PARAMETERS.forEach(this.cleanedUpParameters::remove);
            super.beginFigure(this.cleanedUpParameters);
        }

        @Override
        public void endFigure(Map<String, String> parameters)
        {
            super.endFigure(this.cleanedUpParameters);
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
