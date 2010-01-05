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
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;

import java.util.Map;

/**
 * Default implementation bridging to the old XWiki Context to get current Model Reference Objects.
 *
 * @version $Id$
 * @since 2.2M1
 */
@Component
public class DefaultModelContext implements ModelContext
{
    /**
     * The Execution Context from which we get the old XWiki Context from which we get the current Model Reference
     * Objects.
     */
    @Requirement
    private Execution execution;

    /**
     * {@inheritDoc}
     * @see org.xwiki.model.ModelContext#getCurrentEntityReference()
     */
    public EntityReference getCurrentEntityReference()
    {
        WikiReference result;
        // TODO: This is bridge to the old XWiki Context since we currently don't store the current entity in the
        // new Execution Context yet. Remove when we do so.
        try {
            Map<String, Object> xcontext =
                (Map<String, Object>) this.execution.getContext().getProperty("xwikicontext");
            result = new WikiReference((String) xcontext.get("wiki")); 
        } catch (Exception e) {
            // Shouldn't happen in normal cases. Could happen if the context doesn't contain the old XWiki Context
            // but that would be a bug in the initialization system somewhere.
            result = null;
        }
        return result;        
    }
}
