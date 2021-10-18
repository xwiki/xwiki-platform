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
package org.xwiki.annotation.internal.content;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.annotation.content.TextExtractor;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Component responsible for finding the specific implementation depending on the given syntax and extracting the plain
 * text, if possible.
 * 
 * @version $Id$
 * @since 13.9
 */
@Component
@Singleton
public class DefaultTextExtractor implements TextExtractor
{
    @Inject
    private ComponentManager componentManager;

    @Override
    public String extractText(String content, Syntax syntax)
    {
        try {
            HTMLTextExtractor syntaxSpecificTextExtractor =
                componentManager.getInstance(TextExtractor.class, syntax.getType().getId());
            return syntaxSpecificTextExtractor.extractText(content, syntax);
        } catch (ComponentLookupException e) {
            // In case this happens, the content will not be altered.
        }
        return content;
    }

}
