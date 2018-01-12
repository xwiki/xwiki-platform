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
package com.xpn.xwiki.internal.doc;

import java.util.ArrayList;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * AttachmentList that holds elements in order of filename.
 * 
 * @version $Id$
 * @since 10.0RC1
 */
public class XWikiAttachmentList extends ArrayList<XWikiAttachment>
{

    private Map<String, XWikiAttachment> map;

    /**
     * Initializes the map.
     * 
     * @since 10.0RC1
     */
    public XWikiAttachmentList()
    {
        map = new TreeMap<String, XWikiAttachment>();
    }

    /**
     * Adds attachment to the list in order of filename.
     * 
     * @since 10.0RC1
     */
    @Override
    public boolean add(XWikiAttachment attachment)
    {
        map.put(attachment.getFilename(), attachment);
        super.clear();
        super.addAll(map.values());
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @since 10.0RC1
     */
    @Override
    public void clear()
    {
        super.clear();
        map.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * @since 10.0RC1
     */
    @Override
    public boolean addAll(Collection<? extends XWikiAttachment> c)
    {
        for (XWikiAttachment x : c) {
            map.put(x.getFilename(), x);
        }
        super.clear();
        super.addAll(map.values());
        return true;

    }

    /**
     * {@inheritDoc}
     * 
     * @since 10.0RC1
     */
    @Override
    public XWikiAttachment remove(int index)
    {

        XWikiAttachment removedAttachment = map.remove(super.get(index).getFilename());
        return removedAttachment == null ? null : super.remove(index);

    }

    /**
     * Removes XWikiAttachment.
     * 
     * @param attachment XWikiAttachment to remove.
     * @return XWikiAttachment that was removed or null if not found
     * @since 10.0RC1
     */
    public XWikiAttachment remove(XWikiAttachment attachment)
    {
        String filename = attachment.getFilename();
        XWikiAttachment removedAttachment = map.remove(filename);
        super.clear();
        super.addAll(map.values());
        return removedAttachment;

    }

    /**
     * Adds or replaces attachment with the same filename as the parameter.
     * 
     * @param attachment the attachment to add to the list
     * @return the attachment that was added to the list in order of filename
     * @since 10.0RC1
     */
    public XWikiAttachment set(XWikiAttachment attachment)
    {
        map.put(attachment.getFilename(), attachment);
        super.clear();
        super.addAll(map.values());
        return attachment;
    }

    /**
     * @param filename the filename of the attachment to be returned.
     * @return attachment with the given filename or null if not found.
     * @since 10.0RC1
     */
    public XWikiAttachment getByFilename(String filename)
    {
        return map.get(filename);
    }

}
