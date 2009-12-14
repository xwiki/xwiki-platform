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
package org.xwiki.model.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.DocumentName;
import org.xwiki.model.Model;

import java.util.Map;

/**
 * Default implementation bridging to the old XWiki Context to get current Model Objects.
 *
 * @version $Id$
 * @since 2.2M1
 */
@Component
public class DefaultModel implements Model
{
    /**
     * The Execution Context from which we get the old XWiki Context from which we get the current Model Objects.
     */
    @Requirement
    private Execution execution;

    /**
     * {@inheritDoc}
     * @see org.xwiki.model.Model#getCurrentDocumentName()
     */
    public DocumentName getCurrentDocumentName()
    {
        DocumentName result;
        // TODO: This is bridge to the old XWiki Context since we currently don't store the current document in the
        // new Execution Context yet. Remove when we do so.
        try {
            Map<Object, Object> xcontext =
                (Map<Object, Object>) this.execution.getContext().getProperty("xwikicontext");
            Object currentDocument = xcontext.get("doc");
            result = (DocumentName) currentDocument.getClass().getMethod("getDocumentName").invoke(currentDocument);
        } catch (Exception e) {
            // Shouldn't happen in normal cases. Could happen if the context doesn't contain the old XWiki Context
            // but that would be a bug in the initialization system somewhere.
            result = null;
        }
        return result;        
    }
}
