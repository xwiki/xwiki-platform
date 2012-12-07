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
package org.xwiki.component.wiki.internal.bridge;

import java.io.StringReader;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;

/**
 * A bridge between Wiki Components and rendering.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class DefaultContentParser implements ContentParser
{
    /**
     * Used to retrieve parsers dynamically depending on documents syntax.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public XDOM parse(String content, Syntax syntax) throws WikiComponentException
    {
        XDOM xdom;

        try {
            Parser parser = componentManager.getInstance(Parser.class, syntax.toIdString());
            xdom  = parser.parse(new StringReader(content));
        } catch (Exception e) {
            throw new WikiComponentException(String.format("Failed to parse content [%s]", content), e);
        }

        return xdom;
    }
}
