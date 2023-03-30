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
 * TODO.
 *
 * @version $Id$
 * @since x.y.z
 */
@Component
@Singleton
@Named("tmprender")
public class CaptionedImageRenderListenerProvider implements ListenerProvider
{
    private static final String WIDTH_PROPERTY = "width";

    private static final String STYLE_PROPERTY = "style";

    private static final List<Syntax> ACCEPTED_SYNTAX = List.of(HTML_5_0, XWIKI_2_0, XWIKI_2_1);

    private static class InternalChainingListener extends AbstractChainingListener
    {
        private static final List<String> KNOWN_PARAMETERS = List.of(
            WIDTH_PROPERTY,
            STYLE_PROPERTY,
            // TODO: reuse constant if it exists
            "data-xwiki-image-style",
            "data-xwiki-image-style-alignment",
            "data-xwiki-image-style-border",
            "data-xwiki-image-style-text-wrap"
        );

        private Map<String, String> cleanedUpParameters;

        /**
         * TODO.
         *
         * @param listenerChain TODO
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
