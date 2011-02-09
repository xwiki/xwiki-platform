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
package org.xwiki.rendering.listener;

import java.util.Map;

/**
 * Wrap a listener and skip begin/endDocument events.
 * 
 * @version $Id$
 * @since 3.0M3
 */
public class InlineFilterListener extends WrappingListener
{
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.WrappingListener#beginDocument(org.xwiki.rendering.listener.MetaData)
     */
    @Override
    public void beginDocument(MetaData metaData)
    {
        // Disable this event
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.WrappingListener#endDocument(org.xwiki.rendering.listener.MetaData)
     */
    @Override
    public void endDocument(MetaData metaData)
    {
        // Disable this event
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.WrappingListener#beginSection(java.util.Map)
     */
    @Override
    public void beginSection(Map<String, String> parameters)
    {
        // Disable this event
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.WrappingListener#endSection(java.util.Map)
     */
    @Override
    public void endSection(Map<String, String> parameters)
    {
        // Disable this event
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.WrappingListener#beginParagraph(java.util.Map)
     */
    @Override
    public void beginParagraph(Map<String, String> parameters)
    {
        // Disable this event
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.WrappingListener#endParagraph(java.util.Map)
     */
    @Override
    public void endParagraph(Map<String, String> parameters)
    {
        // Disable this event
    }
}
