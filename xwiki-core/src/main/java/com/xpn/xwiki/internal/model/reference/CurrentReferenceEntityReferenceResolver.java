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
package com.xpn.xwiki.internal.model.reference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.internal.reference.DefaultReferenceEntityReferenceResolver;
import org.xwiki.model.reference.EntityReference;

/**
 * Resolve an {@link org.xwiki.model.reference.EntityReference} into a valid and absolute reference (with all required
 * parents filled in). The behavior is the same as for
 * {@link com.xpn.xwiki.internal.model.reference.CurrentStringEntityReferenceResolver}.
 *
 * @version $Id$
 * @since 2.2M1
 * @see com.xpn.xwiki.internal.model.reference.CurrentStringEntityReferenceResolver
 */
@Component("current/reference")
public class CurrentReferenceEntityReferenceResolver extends DefaultReferenceEntityReferenceResolver
{
    @Requirement
    private ModelContext modelContext;

    @Requirement
    private Execution execution;

    /**
     * {@inheritDoc}
     * 
     * @see DefaultReferenceEntityReferenceResolver#getDefaultReferenceName(org.xwiki.model.EntityType)
     */
    @Override
    protected String getDefaultReferenceName(EntityType type)
    {
        String result;

        if (type == EntityType.WIKI) {
            EntityReference wikiReference = this.modelContext.getCurrentEntityReference();
            if (wikiReference != null) {
                wikiReference = wikiReference.extractReference(EntityType.WIKI);
            }
            if (wikiReference != null) {
                result = wikiReference.getName();
            } else {
                result = super.getDefaultReferenceName(type);
            }
        } else if (type == EntityType.SPACE || type == EntityType.DOCUMENT) {
            XWikiDocument currentDoc = getContext().getDoc();
            if (currentDoc != null) {
                if (type == EntityType.SPACE) {
                    result = currentDoc.getDocumentReference().getLastSpaceReference().getName();
                } else {
                    result = currentDoc.getDocumentReference().getName();
                }
            } else {
                result = super.getDefaultReferenceName(type);
            }
        } else {
            result = super.getDefaultReferenceName(type);
        }

        return result;
    }

    /**
     * @return the XWiki Context used to bridge with the old API
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
