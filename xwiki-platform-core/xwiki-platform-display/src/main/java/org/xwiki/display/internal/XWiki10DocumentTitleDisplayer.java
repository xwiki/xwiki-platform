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
package org.xwiki.display.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.XDOM;

/**
 * Overwrites {@link DocumentTitleDisplayer} for XWiki 1.0 syntax.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("title/xwiki/1.0")
@Singleton
public class XWiki10DocumentTitleDisplayer extends DocumentTitleDisplayer
{
    /**
     * Regular expression for finding the first level 1 or 2 heading in the document content, to be used as the document
     * title.
     */
    private static final Pattern HEADING_PATTERN_10 = Pattern.compile("^\\s*+1(?:\\.1)?\\s++(.++)$", Pattern.MULTILINE);

    @Override
    protected XDOM extractTitleFromContent(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        String title = "";
        Matcher matcher = HEADING_PATTERN_10.matcher(document.getContent());
        if (matcher.find()) {
            title = matcher.group(1).trim();
        }

        if (!title.isEmpty()) {
            return parseTitle(evaluateTitle(title, document.getDocumentReference(), parameters));
        }

        return null;
    }
}
