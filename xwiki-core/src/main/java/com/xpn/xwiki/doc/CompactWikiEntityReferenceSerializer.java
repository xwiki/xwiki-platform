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
package com.xpn.xwiki.doc;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.internal.reference.DefaultEntityReferenceSerializer;
import org.xwiki.model.reference.EntityReference;

/**
 * Generate an entity reference string that doesn't contain the wiki reference part if the passed reference matches
 * the current wiki.
 *
 * @version $Id$
 * @since 2.2M1
 */
@Component("compactwiki")
public class CompactWikiEntityReferenceSerializer extends DefaultEntityReferenceSerializer
{
    @Requirement
    private ModelContext modelContext;

    /**
     * {@inheritDoc}
     * @see DefaultEntityReferenceSerializer#serializeEntityReference(EntityReference, StringBuilder)
     */
    @Override
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation)
    {
        boolean shouldPrint = false;

        EntityReference wikiReference = this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI);
        if (wikiReference == null || currentReference.getChild() == null || representation.length() > 0) {
            shouldPrint = true;
        } else {
            switch (currentReference.getType()) {
                case WIKI:
                    if (!wikiReference.getName().equals(currentReference.getName())) {
                        shouldPrint = true;
                    }
                    break;
                default:
                    shouldPrint = true;
                    break;
            }
        }
        if (shouldPrint) {
            super.serializeEntityReference(currentReference, representation);
        }
    }
}
