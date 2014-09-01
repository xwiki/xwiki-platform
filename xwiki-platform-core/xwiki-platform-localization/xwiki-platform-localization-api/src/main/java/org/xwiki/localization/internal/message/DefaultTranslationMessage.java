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
package org.xwiki.localization.internal.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.localization.message.TranslationMessageElement;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;

/**
 * Default implementation of {@link TranslationMessage}.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public class DefaultTranslationMessage implements TranslationMessage
{
    /**
     * The source of the translation message.
     */
    private String rawSource;

    /**
     * The elements in the message.
     */
    private List<TranslationMessageElement> elements;

    /**
     * @param rawSource the source of the translation message
     * @param elements the elements in the message
     */
    public DefaultTranslationMessage(String rawSource, List<TranslationMessageElement> elements)
    {
        this.rawSource = rawSource;
        this.elements = new ArrayList<TranslationMessageElement>(elements);
    }

    @Override
    public Block render(Locale locale, Collection<TranslationBundle> bundles, Object... parameters)
    {
        Block block = new CompositeBlock();

        for (TranslationMessageElement element : this.elements) {
            block.addChild(element.render(locale, bundles, parameters));
        }

        return null;
    }

    @Override
    public String getRawSource()
    {
        return this.rawSource;
    }
    
    @Override
    public String toString()
    {
        return getRawSource();
    }
}
