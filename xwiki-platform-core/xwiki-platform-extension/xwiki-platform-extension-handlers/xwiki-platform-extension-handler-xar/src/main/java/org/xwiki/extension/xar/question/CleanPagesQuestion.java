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
package org.xwiki.extension.xar.question;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;

/**
 * The pages planned for deleted.
 * <p>
 * Expect a confirmation for each one.
 * 
 * @version $Id$he
 * @since 5.4M1
 */
public class CleanPagesQuestion
{
    private Map<DocumentReference, Boolean> pages;

    /**
     * @param pages the pages planned for deletion
     */
    public CleanPagesQuestion(Collection<DocumentReference> pages)
    {
        this.pages = new HashMap<DocumentReference, Boolean>(pages.size());

        for (DocumentReference page : pages) {
            this.pages.put(page, true);
        }
    }

    /**
     * @return the pages planned for deletion
     */
    public Map<DocumentReference, Boolean> getPages()
    {
        return this.pages;
    }
}
