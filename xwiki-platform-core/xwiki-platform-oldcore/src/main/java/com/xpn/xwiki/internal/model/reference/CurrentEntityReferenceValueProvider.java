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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The behavior is the following:
 * <ul>
 * <li>The wiki value used is the current wiki if no wiki was specified in the passed reference (or if it was empty).
 * Note that this is different from using the current document's wiki value.</li>
 * <li>The space value used is the space from the current document reference if no space was specified in the passed
 * reference (or if it was empty). If the current document reference is not defined then the default space value is used
 * instead.</li>
 * <li>The page value used is the page from the current document reference if no page was specified in the passed
 * reference (or if it was empty). If the current document reference is not defined then the default page value is used
 * instead.</li>
 * </ul>
 *
 * @version $Id$
 * @since 2.3M1
 * @deprecated since 7.2M1, use {@link CurrentEntityReferenceProvider} instead
 */
@Component
@Named("current")
@Singleton
@Deprecated
public class CurrentEntityReferenceValueProvider extends DefaultEntityReferenceValueProvider
{
    @Inject
    private ModelContext modelContext;

    @Inject
    private Execution execution;

    @Override
    public String getDefaultValue(EntityType type)
    {
        String result = null;

        if (type == EntityType.WIKI) {
            EntityReference wikiReference = this.modelContext.getCurrentEntityReference();
            if (wikiReference != null) {
                wikiReference = wikiReference.extractReference(EntityType.WIKI);
            }
            if (wikiReference != null) {
                result = wikiReference.getName();
            }
        } else if (type == EntityType.SPACE || type == EntityType.DOCUMENT) {
            XWikiContext xcontext = getContext();
            if (xcontext != null) {
                XWikiDocument currentDoc = xcontext.getDoc();
                if (currentDoc != null) {
                    if (type == EntityType.SPACE) {
                        result = currentDoc.getDocumentReference().getLastSpaceReference().getName();
                    } else {
                        result = currentDoc.getDocumentReference().getName();
                    }
                }
            }
        }

        if (result == null) {
            result = super.getDefaultValue(type);
        }

        return result;
    }

    /**
     * @return the XWiki Context used to bridge with the old API
     */
    private XWikiContext getContext()
    {
        XWikiContext xcontext = null;

        if (this.execution.getContext() != null) {
            xcontext = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        }

        return xcontext;
    }
}
