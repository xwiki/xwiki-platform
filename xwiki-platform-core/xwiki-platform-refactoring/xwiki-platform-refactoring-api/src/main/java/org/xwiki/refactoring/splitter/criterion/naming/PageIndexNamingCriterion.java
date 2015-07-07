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
package org.xwiki.refactoring.splitter.criterion.naming;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.block.XDOM;

/**
 * A {@link NamingCriterion} based on the name of the main document being split.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class PageIndexNamingCriterion implements NamingCriterion
{
    /**
     * {@link DocumentAccessBridge} used to lookup for existing wiki pages and avoid name clashes.
     */
    private DocumentAccessBridge docBridge;

    /**
     * Base name to be used for generating new document names.
     */
    private String baseDocumentName;

    /**
     * Current value of the post-fix appended to new document names.
     */
    private int index = 0;

    /**
     * Constructs a new {@link PageIndexNamingCriterion}.
     * 
     * @param baseDocumentName base name to be used for generating new document names.
     * @param docBridge {@link DocumentAccessBridge} used to lookup for documents.
     */
    public PageIndexNamingCriterion(String baseDocumentName, DocumentAccessBridge docBridge)
    {
        this.baseDocumentName = baseDocumentName;
        this.docBridge = docBridge;
    }

    @Override
    public String getDocumentName(XDOM newDoc)
    {
        int newIndex = ++index;
        String newDocumentName = baseDocumentName + INDEX_SEPERATOR + newIndex;
        // Resolve any name clashes.
        int localIndex = 0;
        while (docBridge.exists(newDocumentName)) {
            // Append a trailing local index if the page already exists
            newDocumentName =
                baseDocumentName + INDEX_SEPERATOR + newIndex + INDEX_SEPERATOR + (++localIndex);
        }
        return newDocumentName;
    }
}
