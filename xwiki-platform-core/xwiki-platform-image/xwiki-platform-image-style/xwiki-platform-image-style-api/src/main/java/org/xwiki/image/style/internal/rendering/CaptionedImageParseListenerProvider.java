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

import java.util.Objects;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.ListenerProvider;
import org.xwiki.rendering.listener.chaining.ChainingListener;
import org.xwiki.rendering.listener.chaining.ListenerChain;
import org.xwiki.rendering.syntax.Syntax;

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

    @Override
    public ChainingListener getListener(ListenerChain listenerChain)
    {
        return new CaptionedImageParseChainingListener(listenerChain);
    }

    @Override
    public boolean accept(String action, Syntax syntax)
    {
        return Objects.equals(action, PARSE_ACTION) && syntax != null && ACCEPTED_SYNTAX.contains(syntax);
    }
}
