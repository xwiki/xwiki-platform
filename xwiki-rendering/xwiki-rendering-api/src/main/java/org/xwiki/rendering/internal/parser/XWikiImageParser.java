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
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.listener.Attachment;
import org.xwiki.rendering.parser.AttachmentParser;

/**
 * Parses XWiki image definitions, using either a URL (pointing to an image or the following format:
 * <code>wiki:Space.Page@attachmentName</code> where <code>imageName</code> is the name of the image attachment (for
 * example "my.png").
 * 
 * @version $Id$
 * @since 1.7M3
 */
@Component("xwiki/2.0")
public class XWikiImageParser extends AbstractImageParser
{
    /**
     * Used to parse the attachment syntax to extract document name and attachment name.
     */
    @Requirement
    private AttachmentParser attachmentParser;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractImageParser#parseAttachment(String)
     */
    @Override
    protected Attachment parseAttachment(String attachment)
    {
        return this.attachmentParser.parse(attachment);
    }
}
