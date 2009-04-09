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
package org.xwiki.rendering.internal.parser;

import java.util.Arrays;
import java.util.List;

import org.xwiki.component.annotation.Component;

/**
 * Each syntax should have its own link parser. However while we wait for syntax specific parser to be implemented
 * this generic parser should provide a good approximation.
 * 
 * @version $Id$
 * @since 1.6RC1
 * @see AbstractLinkParser
 */
@Component
public class GenericLinkParser extends AbstractLinkParser
{
    /**
     * The list of recognized URL prefixes.
     */
    private static final List<String> URI_PREFIXES = Arrays.asList("mailto");

    /**
     * {@inheritDoc}
     * @see AbstractLinkParser#getAllowedURIPrefixes()
     */
    @Override
    protected List<String> getAllowedURIPrefixes()
    {
        return URI_PREFIXES;
    }
}
