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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Default implementation bridging to the old XWiki Context to get current Model Reference Objects.
 * 
 * @version $Id$
 * @since 2.2M1
 */
@Component
@Singleton
public class DefaultModelContext implements ModelContext
{
    /**
     * Key of the XWikiContext located in the {@link ExecutionContext}.
     */
    public static final String XCONTEXT_KEY = "xwikicontext";
    
    /**
     * Key of the wiki name located in the XWikiContext.
     */
    private static final String WIKINAME_KEY = "wiki";
    
    /**
     * The Execution Context from which we get the old XWiki Context from which we get the current Model Reference
     * Objects.
     */
    @Inject
    private Execution execution;

    @Override
    public EntityReference getCurrentEntityReference()
    {
        WikiReference result = null;

        // TODO: This is bridge to the old XWiki Context since we currently don't store the current entity in the
        // new Execution Context yet. Remove when we do so.
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            Map<String, Object> xcontext = (Map<String, Object>) econtext.getProperty(XCONTEXT_KEY);

            if (xcontext != null) {
                String wikiName = (String) xcontext.get(WIKINAME_KEY);

                if (wikiName != null) {
                    result = new WikiReference(wikiName);
                }
            }
        }

        return result;
    }

    @Override
    public void setCurrentEntityReference(EntityReference entityReference)
    {
        // TODO: This is bridge to the old XWiki Context since we currently don't store the current entity in the
        // new Execution Context yet. Remove when we do so.
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            Map<String, Object> xcontext = (Map<String, Object>) econtext.getProperty(XCONTEXT_KEY);

            if (xcontext != null) {
                xcontext.put(WIKINAME_KEY, extractWikiName(entityReference));
            }
        }
    }

    /**
     * Extract wiki name from provided entity reference.
     * 
     * @param entityReference the entity reference
     * @return the wiki name, null if no wiki name can be found.
     */
    private String extractWikiName(EntityReference entityReference)
    {
        String wikiName = null;
        if (entityReference != null) {
            EntityReference wikiReference = entityReference.extractReference(EntityType.WIKI);
            if (wikiReference != null) {
                wikiName = wikiReference.getName();
            }
        }

        return wikiName;
    }
}
