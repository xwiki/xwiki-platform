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
package org.xwiki.gwt.wysiwyg.client.plugin.font;

import org.xwiki.gwt.dom.client.Element;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;

/**
 * Abstract font {@link Matcher}.
 * 
 * @version $Id$
 */
public abstract class AbstractFontMatcher extends DefaultStringMatcher
{
    /**
     * The container element where the matching takes place.
     */
    protected final Element container;

    /**
     * The element with the left value.
     */
    protected final Element left;

    /**
     * The element with the right value.
     */
    protected final Element right;

    /**
     * Creates a new abstract font matcher that uses the given text to match font properties.
     * 
     * @param text the text used to match font properties
     */
    public AbstractFontMatcher(String text)
    {
        left = Document.get().createSpanElement().cast();
        left.appendChild(Document.get().createTextNode(text));

        right = (Element) left.cloneNode(true);

        container = Document.get().createDivElement().cast();
        container.getStyle().setPosition(Position.ABSOLUTE);
        container.getStyle().setLeft(-9999, Unit.PX);
        container.getStyle().setTop(0, Unit.PX);
        container.getStyle().setVisibility(Visibility.HIDDEN);
        container.appendChild(left);
        container.appendChild(right);
        Document.get().getBody().appendChild(container);
    }
}
