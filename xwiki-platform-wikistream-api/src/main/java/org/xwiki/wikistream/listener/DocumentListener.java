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
package org.xwiki.wikistream.listener;

import org.xwiki.rendering.listener.MetaData;

/**
 * Contains call back events for document
 * 
 * @version $Id$
 */
public interface DocumentListener
{
    void beginDocument(String name, MetaData metadata);

    void endDocument(String name, MetaData metadata);

    void beginRevision(String version, MetaData metadata);

    void beginObject(MetaData metadata);

    void beginProperty(String propertyName, MetaData metadata);

    void endProperty(String propertyName, MetaData metadata);

    void endObject(MetaData metadata);

    void beginClass(MetaData metadata);

    void endClass(MetaData metadata);

    void beginAttachment(String attachmentName, MetaData metadata);

    void endAttachment(String attachmentName, MetaData metadata);

    void endRevision(String version, MetaData metadata);
}
