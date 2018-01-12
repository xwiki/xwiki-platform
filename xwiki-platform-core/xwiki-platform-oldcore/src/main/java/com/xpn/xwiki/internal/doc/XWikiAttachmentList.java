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
import java.util.ListIterator;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * AttachmentList that holds elements in order of filename.
 * 
 * @version $Id$
 * @since 10.0RC1
 */
public class XWikiAttachmentList extends ArrayList<XWikiAttachment>
{

    private Map<String, XWikiAttachment> map;

    private XWikiDocument document;

    /**
     * Initializes the map.
     * 
     * @since 10.0RC1
     */
    public XWikiAttachmentList(XWikiDocument document)
    {
        map = new ConcurrentSkipListMap<String, XWikiAttachment>();
        this.document = document;
        document.setMetaDataDirty(true);
    }

    /**
     * Adds attachment to the list in order of filename.
     * 
     * @param attachment XWikiAttachment to add to the list
     * @since 10.0RC1
     */
    @Override
    public boolean add(XWikiAttachment attachment)
    {
        map.put(attachment.getFilename(), attachment);
        super.clear();
        super.addAll(map.values());
        onUpdate();
        added(attachment);
        return true;
    }

    /**
     * Adds attachment to the list in order of filename.
     * 
     * @param index index is ignored as list is reordered based on filename
     * @param attachment XWikiAttachment to add to the list
     * @since 10.0RC1
     */
    @Override
    public void add(int index, XWikiAttachment attachment)
    {
        map.put(attachment.getFilename(), attachment);
        super.clear();
        super.addAll(map.values());
        onUpdate();
        added(attachment);
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
        onUpdate();
    }

    /**
     * {@inheritDoc}
     * 
     * @since 10.0RC1
     */
    @Override
    public boolean addAll(Collection<? extends XWikiAttachment> c)
    {
        onUpdate();
        for (XWikiAttachment x : c) {
            map.put(x.getFilename(), x);
            added(x);
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
        onUpdate();
        return removedAttachment == null ? null : super.remove(index);

    }

    /**
     * Removes XWikiAttachment.
     * 
     * @param attachment XWikiAttachment to remove.
     * @return XWikiAttachment that was removed or null if not found
     * @since 10.0RC1
     */
    @Override
    public boolean remove(Object attachment)
    {
        String filename = ((XWikiAttachment) (attachment)).getFilename();
        XWikiAttachment removedAttachment = map.remove(filename);
        super.clear();
        super.addAll(map.values());
        return removedAttachment == null ? false : true;
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

    /** Called when the list is updated. The method will be called at least once, but may be called several times */
    public void onUpdate()
    {
        document.setMetaDataDirty(true);
    }

    /**
     * @param element XWikiAttachment that was added to the list
     * @since 10.0RC1
     */
    protected void added(XWikiAttachment element)
    {
        element.setDoc(document);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 10.0RC1
     */
    @Override
    public boolean contains(Object x)
    {
        return super.contains((XWikiAttachment) x);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 10.0RC1
     */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        return super.containsAll(c);
    }

}
