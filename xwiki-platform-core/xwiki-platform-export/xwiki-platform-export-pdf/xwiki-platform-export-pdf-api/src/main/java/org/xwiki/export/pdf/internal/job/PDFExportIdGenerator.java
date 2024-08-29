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
package org.xwiki.export.pdf.internal.job;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.xwiki.rendering.util.IdGenerator;

/**
 * The id generator used when rendering wiki pages for PDF export. It collects a map of {@code localId -> globalId} for
 * each rendered page (see {@link #resetLocalIds()}) that can be used on the client side to refactor external links into
 * internal links. The local id is the id that is generated when rendering a single page, while the global id is the id
 * generated when rendering multiple pages (and thus the generated id needs to be unique across all these pages).
 * 
 * @version $Id$
 * @since 14.10.6
 * @since 15.1RC1
 */
public class PDFExportIdGenerator extends IdGenerator
{
    private IdGenerator localIdGenerator = new IdGenerator();

    /**
     * Maps local IDs to global IDs.
     */
    private Map<String, String> idMap = new HashMap<>();

    @Override
    public String generateUniqueId(String prefix, String text)
    {
        String globalId = super.generateUniqueId(prefix, text);
        String localId = this.localIdGenerator.generateUniqueId(prefix, text);
        this.idMap.put(localId, globalId);
        return globalId;
    }

    @Override
    public void remove(String globalId)
    {
        super.remove(globalId);
        this.idMap.entrySet().stream().filter(entry -> Objects.equals(entry.getValue(), globalId)).findFirst()
            .ifPresent(entry -> {
                String localId = entry.getKey();
                this.localIdGenerator.remove(localId);
                this.idMap.remove(localId);
            });
    }

    @Override
    public void reset()
    {
        super.reset();
        resetLocalIds();
    }

    /**
     * Reset the collected local IDs. Call this before each page rendering.
     * 
     * @return the mapping between the local IDs and the global IDs
     */
    public Map<String, String> resetLocalIds()
    {
        Map<String, String> idMapCopy = new HashMap<>(this.idMap);
        this.localIdGenerator.reset();
        this.idMap.clear();
        return idMapCopy;
    }
}
