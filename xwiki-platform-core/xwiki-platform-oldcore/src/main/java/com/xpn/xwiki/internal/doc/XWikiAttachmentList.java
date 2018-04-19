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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.collections4.list.AbstractListDecorator;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachment.AttachmentContainer;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * AttachmentList that holds elements in order of filename.
 * 
 * @version $Id$
 * @since 10.1RC1
 */
public class XWikiAttachmentList extends AbstractListDecorator<XWikiAttachment> implements AttachmentContainer
{
    private final Map<String, XWikiAttachment> map = new ConcurrentSkipListMap<>();

    /**
     * Retro compatibility for very bad old code.
     */
    private final List<XWikiAttachment> emptyNameAttachments = new CopyOnWriteArrayList<>();

    private final XWikiDocument document;

    /**
     * Initializes the map.
     * 
     * @param document the document where this attachment is located
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
     */
    @Override
    public void add(int index, XWikiAttachment attachment)
    {
        add(attachment);
    }

    @Override
    public void clear()
    {
        this.map.clear();

        updateList();
    }

    /**
     * Adds all attachments to the list in order of filename.
     * 
     * @param c Collection that contains XWikiAttachment objects
     */
    @Override
    public boolean addAll(Collection<? extends XWikiAttachment> c)
    {
        boolean changed = false;
        for (XWikiAttachment x : c) {
            XWikiAttachment put = this.map.put(x.getFilename(), x);
            if (put != x) {
                changed = true;
                added(x);
            }
        }

        if (changed) {
            updateList();
        }

        return changed;
    }

    /**
     * Adds all attachments to the list in order of filename.
     * 
     * @param index index is ignored as list is reordered based on filename
     * @param c Collection that contains XWikiAttachment objects
     */
    @Override
    public boolean addAll(int index, Collection<? extends XWikiAttachment> c)
    {
        return addAll(c);
    }

    @Override
    public XWikiAttachment remove(int index)
    {
        XWikiAttachment removedAttachment = this.map.remove(this.decorated().get(index).getFilename());
        if (removedAttachment != null) {
            updateList();
        }

        return removedAttachment;
    }

    /**
     * Removes XWikiAttachment.
     * 
     * @param attachment XWikiAttachment to remove.
     * @return true unless the attachment is not found
     */
    @Override
    public boolean remove(Object attachment)
    {
        XWikiAttachment xwikiAttachment = (XWikiAttachment) attachment;
        if (xwikiAttachment != null) {
            if (xwikiAttachment.getFilename() != null) {
                if (this.map.remove(xwikiAttachment.getFilename()) != null) {
                    updateList();

                    return true;
                }
            } else if (this.emptyNameAttachments.remove(xwikiAttachment)) {
                updateList();

                return true;
            }
        }

        return false;
    }

    /**
     * Adds or replaces attachment with the same filename as the parameter.
     * 
     * @param attachment the attachment to add to the list
     * @return the attachment that was previously matched to the same filename or null if no attachment was matched to
     *         it
     */
    public XWikiAttachment set(XWikiAttachment attachment)
    {
        XWikiAttachment previous;

        if (attachment.getFilename() != null) {
            previous = this.map.put(attachment.getFilename(), attachment);
            if (previous != attachment) {
                added(attachment);
                updateList();
            }
        } else {
            this.emptyNameAttachments.add(attachment);

            previous = null;
        }

        return previous;
    }

    /**
     * Adds or replaces attachment with the same filename as the parameter.
     * 
     * @param index this parameter is not used but is needed to override the method
     * @param attachment the attachment to add to the list
     * @return the attachment that was previously matched to the same filename or null if no attachment was matched to
     *         it
     */
    @Override
    public XWikiAttachment set(int index, XWikiAttachment attachment)
    {
        return set(attachment);
    }

    /**
     * @param filename the filename of the attachment to be returned.
     * @return attachment with the given filename or null if not found.
     */
    public XWikiAttachment getByFilename(String filename)
    {
        // Null key is forbidden in ConcurrentSkipListMap
        return filename != null ? this.map.get(filename) : null;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean changed = false;
        for (XWikiAttachment x : (Collection<? extends XWikiAttachment>) c) {
            if (this.map.get(x.getFilename()) == x) {
                this.map.remove(x.getFilename());
                changed = true;
            }
        }
        if (changed) {
            updateList();
        }

        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean changed = false;
        Collection<XWikiAttachment> values = this.map.values();
        for (XWikiAttachment x : values) {
            if (!c.contains(x)) {
                this.map.remove(x.getFilename());
                changed = true;
            }
        }

        if (changed) {
            updateList();
        }

        return changed;
    }

    /**
     * Sets MetaDataDirty to true and resets the list with the values in the map.
     */
    private void updateList()
    {
        this.document.setMetaDataDirty(true);

        List<XWikiAttachment> list = new ArrayList<>(this.map.values());
        list.addAll(this.emptyNameAttachments);

        this.setCollection(list);
    }

    /**
     * @param element XWikiAttachment that was added to the list
     */
    protected void added(XWikiAttachment element)
    {
        element.setDoc(this.document);
        element.setAttachmentContainer(this);
    }

    protected void removed(XWikiAttachment element)
    {
        element.setAttachmentContainer(null);
    }

    @Override
    public void onAttachmentNameModified(String previousAttachmentName, XWikiAttachment attachment)
    {
        // Remove the attachment from the previous location
        boolean removed;
        if (previousAttachmentName != null) {
            removed = this.map.remove(previousAttachmentName) == attachment;
        } else {
            removed = this.emptyNameAttachments.remove(attachment);
        }

        if (removed) {
            // Add in the right place
            set(attachment);
        }
    }
}
