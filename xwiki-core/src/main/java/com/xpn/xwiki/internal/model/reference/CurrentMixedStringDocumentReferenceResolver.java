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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

/**
 * This is an implementation for backward-compatibility. It's behavior is essentially similar to the
 * {@link com.xpn.xwiki.internal.model.reference.CurrentStringDocumentReferenceResolver} implementation except for
 * two details:
 * <ul>
 * <li>if the document reference doesn't have a name specified it defaults to the default name and not
 * the current document's page name.</li>
 * <li>if the wiki reference isn't specified then the current wiki reference is used (as opposed to the current
 * document reference's wiki reference for
 * {@link com.xpn.xwiki.internal.model.reference.CurrentStringDocumentReferenceResolver}</li>
 * </ul>
 * This is to behave similarly to the old code. However new code should use the
 * {@link com.xpn.xwiki.internal.model.reference.CurrentStringDocumentReferenceResolver} instead as much as possible
 * since this version will eventually be removed.
 *
 * @version $Id$
 * @since 2.2M1
 */
@Component("currentmixed")
public class CurrentMixedStringDocumentReferenceResolver extends DefaultStringEntityReferenceResolver
    implements DocumentReferenceResolver<String>
{
    @Requirement
    private ModelContext modelContext;

    @Requirement
    private Execution execution;

    /**
     * {@inheritDoc}
     * @see org.xwiki.model.reference.DocumentReferenceResolver#resolve(Object)
     */
    public DocumentReference resolve(String documentReferenceRepresentation)
    {
        return new DocumentReference(resolve(documentReferenceRepresentation, EntityType.DOCUMENT));
    }

    /**
     * {@inheritDoc}
     * @see DefaultStringEntityReferenceResolver#getDefaultValuesForType(org.xwiki.model.EntityType)
     */
    protected String getDefaultValuesForType(EntityType type)
    {
        String result;

        XWikiDocument currentDoc = getContext().getDoc();
        if (currentDoc == null) {
            result = super.getDefaultValuesForType(type);
        } else {
            switch (type) {
                case WIKI:
                    EntityReference wikiReference =
                        this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI);
                    result = wikiReference.getName();
                    break;
                case SPACE:
                    result = currentDoc.getSpaceName();
                    break;
                default:
                    result = super.getDefaultValuesForType(type);
                    break;
            }
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
