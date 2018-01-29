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
import java.util.concurrent.ConcurrentSkipListMap;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import org.apache.commons.collections4.list.AbstractListDecorator;

/**
 * AttachmentList that holds elements in order of filename.
 * 
 * @version $Id$
 * @param <E>
 * @since 11.0RC1
 */
public class XWikiAttachmentList extends AbstractListDecorator<XWikiAttachment>
{

    private final Map<String, XWikiAttachment> map = new ConcurrentSkipListMap<String, XWikiAttachment>();

    private final XWikiDocument document;

    /**
     * Initializes the map.
     * 
     * @since 10.0RC1
     */
    public XWikiAttachmentList(XWikiDocument document)
    {
        super(new ArrayList<XWikiAttachment>());
        this.document = document;
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
        XWikiAttachment set = set(attachment);
        return set != attachment;
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
        add(attachment);
    }

    @Override
    public void clear()
    {
        map.clear();
        updatedMap();
    }

    /**
     * Adds all attachments to the list in order of filename.
     * 
     * @param index index is ignored as list is reordered based on filename
     * @param c Collection that contains XWikiAttachment objects
     * @since 10.0RC1
     */
    @Override
    public boolean addAll(Collection<? extends XWikiAttachment> c)
    {
        boolean changed = false;
        for (XWikiAttachment x : c) {
            XWikiAttachment put = map.put(x.getFilename(), x);
            if (put != x) {
                changed = true;
                added(x);
            }
        }
        if (changed) {
            updatedMap();
        }
        return changed;
    }

    /**
     * Adds all attachments to the list in order of filename.
     * 
     * @param index index is ignored as list is reordered based on filename
     * @param c Collection that contains XWikiAttachment objects
     * @since 10.0RC1
     */
    @Override
    public boolean addAll(int index, Collection<? extends XWikiAttachment> c)
    {
        return addAll(c);
    }

    @Override
    public XWikiAttachment remove(int index)
    {
        XWikiAttachment removedAttachment = map.remove(this.decorated().get(index).getFilename());
        if (removedAttachment != null) {
            updatedMap();
        }
        return removedAttachment;
    }

    /**
     * Removes XWikiAttachment.
     * 
     * @param attachment XWikiAttachment to remove.
     * @return true unless the attachment is not found
     * @since 10.0RC1
     */
    @Override
    public boolean remove(Object attachment)
    {
        XWikiAttachment xwikiAttachment = (XWikiAttachment) attachment;
        if (map.remove(xwikiAttachment.getFilename()) == null) {
            return false;
        }
        updatedMap();
        return true;
    }

    /**
     * Adds or replaces attachment with the same filename as the parameter.
     * 
     * @param attachment the attachment to add to the list
     * @return the attachment that was previously matched to the same filename or null if no attachment was matched to
     *         it
     * @since 10.0RC1
     */
    public XWikiAttachment set(XWikiAttachment attachment)
    {
        XWikiAttachment put = map.put(attachment.getFilename(), attachment);
        if (put != attachment) {
            added(attachment);
            updatedMap();
        }
        return put;
    }

    /**
     * Adds or replaces attachment with the same filename as the parameter.
     * 
     * @param index this parameter is not used but is needed to override the method
     * @param attachment the attachment to add to the list
     * @return the attachment that was previously matched to the same filename or null if no attachment was matched to
     *         it
     * @since 10.0RC1
     */
    @Override
    public XWikiAttachment set(int index, XWikiAttachment attachment)
    {
        return set(attachment);
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

    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean changed = false;
        for (XWikiAttachment x : (Collection<? extends XWikiAttachment>) c) {
            if (map.get(x.getFilename()) == x) {
                map.remove(x.getFilename());
                changed = true;
            }
        }
        if (changed) {
            updatedMap();
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean changed = false;
        Collection<XWikiAttachment> values = map.values();
        for (XWikiAttachment x : values) {
            if (!c.contains(x)) {
                map.remove(x.getFilename());
                changed = true;
            }
        }
        if (changed) {
            updatedMap();
        }
        return changed;
    }

    /** Sets MetaDataDirty to true and resets the list with the values in the map */
    private void updatedMap()
    {
        document.setMetaDataDirty(true);
        this.setCollection(new ArrayList<>(map.values()));
    }

    /**
     * @param element XWikiAttachment that was added to the list
     * @since 10.0RC1
     */
    protected void added(XWikiAttachment element)
    {
        element.setDoc(document);
    }

}
