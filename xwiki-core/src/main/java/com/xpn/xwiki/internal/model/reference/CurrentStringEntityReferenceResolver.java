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
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.reference.EntityReference;

/**
 * Generic implementation that resolve {@link org.xwiki.model.reference.EntityReference} objects from their string
 * representation. This implementation uses values from the current document reference in the context when parts of the
 * Reference are missing in the string representation.  
 *
 * @version $Id$
 * @since 2.2M1
 */
@Component("current")
public class CurrentStringEntityReferenceResolver extends DefaultStringEntityReferenceResolver
{
    @Requirement
    private ModelContext modelContext;

    @Requirement
    private Execution execution;

    /**
     * {@inheritDoc}
     * @see DefaultStringEntityReferenceResolver#getDefaultValuesForType(org.xwiki.model.EntityType)
     */
    protected String getDefaultValuesForType(EntityType type)
    {
        String result;

        XWikiDocument currentDoc = getContext().getDoc();
            switch (type) {
                case WIKI:
                    EntityReference wikiReference = this.modelContext.getCurrentEntityReference();
                    if (wikiReference != null) {
                        wikiReference = wikiReference.extractReference(EntityType.WIKI);
                    }
                    if (wikiReference != null) {
                        result = wikiReference.getName();
                    } else {
                        result = super.getDefaultValuesForType(type);
                    }
                    break;
                case SPACE:
                    if (currentDoc != null) {
                        result = currentDoc.getSpaceName();
                    } else {
                        result = super.getDefaultValuesForType(type);
                    }
                    break;
                case DOCUMENT:
                    if (currentDoc != null) {
                        result = currentDoc.getPageName();
                    } else {
                        result = super.getDefaultValuesForType(type);
                    }
                    break;
                default:
                    result = super.getDefaultValuesForType(type);
                    break;
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
