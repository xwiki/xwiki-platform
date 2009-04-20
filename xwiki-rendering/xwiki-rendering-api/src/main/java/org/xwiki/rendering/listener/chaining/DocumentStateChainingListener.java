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
package org.xwiki.rendering.listener.chaining;

import java.util.Map;

/**
 * Provides information on document state: whether we are inside a document, an embedded document and the depth of
 * embedding. Note that this listener is separated from the
 * {@link org.xwiki.rendering.listener.chaining.BlockStateChainingListener} class because we don't want this listener to
 * be stackable (since we need to know the embedding depth even if we're inside an embedded document.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class DocumentStateChainingListener extends AbstractChainingListener
{
    private int documentDepth = 0;

    public DocumentStateChainingListener(ListenerChain listenerChain)
    {
        super(listenerChain);
    }

    public void setDocumentDepth(int documentDepth)
    {
        this.documentDepth = documentDepth;
    }

    public int getDocumentDepth()
    {
        return this.documentDepth;
    }

    public boolean isInDocument()
    {
        return this.documentDepth > 0;
    }

    // Events

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#beginDocument(java.util.Map)
     */
    @Override
    public void beginDocument(Map<String, String> parameters)
    {
        ++this.documentDepth;

        super.beginDocument(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.chaining.AbstractChainingListener#endDocument(java.util.Map)
     */
    @Override
    public void endDocument(Map<String, String> parameters)
    {
        super.endDocument(parameters);

        --this.documentDepth;
    }
}
