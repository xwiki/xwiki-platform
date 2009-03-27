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

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.DefaultAttachement;
import org.xwiki.rendering.listener.DocumentImage;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.parser.ImageParser;

/**
 * Since we need to have wiki syntax-specific image parsers, this generic parser allows at least to the reference
 * displayed when using syntaxes other than XWiki (which has its specific image parser, see {@link XWikiImageParser}),
 * while waiting for specialized image parsers to be written.
 * 
 * @version $Id$
 * @since 1.7M3
 */
@Component
public class GenericImageParser implements ImageParser
{
    /**
     * {@inheritDoc}
     * 
     * @see ImageParser#parse(String)
     */
    public Image parse(String imageLocation)
    {
        Image result;
        if (imageLocation.startsWith("http://")) {
            result = new URLImage(imageLocation);
        } else {
            result = new DocumentImage(new DefaultAttachement(null, imageLocation));
        }

        return result;
    }
}
