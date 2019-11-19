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
package com.xpn.xwiki.plugin.feed.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Create or update the {@code XWiki.FeedEntryClass} document with all required information.
 * 
 * @version $Id$
 * @since 11.10
 */
@Component
@Singleton
@Named("XWiki.FeedEntryClass")
public class FeedEntryClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The local reference of the target class.
     */
    public static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "FeedEntryClass");

    private static final String INTEGER = "integer";

    /**
     * Default constructor.
     */
    public FeedEntryClassDocumentInitializer()
    {
        super(REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.setCustomMapping("internal");

        xclass.addTextField("title", "Title", 80);
        xclass.addUsersField("author", "Author", 40, false);
        xclass.addTextField("feedurl", "Feed URL", 80);
        xclass.addTextField("feedname", "Feed Name", 40);
        xclass.addTextField("url", "URL", 80);
        xclass.addTextField("category", "Category", 255);
        xclass.addTextAreaField("content", "Content", 80, 10);
        xclass.addTextAreaField("fullContent", "Full Content", 80, 10);
        xclass.addTextAreaField("xml", "XML", 80, 10);
        xclass.addDateField("date", "Date", "dd/MM/yyyy HH:mm:ss");
        xclass.addNumberField("flag", "Flag", 5, INTEGER);
        xclass.addNumberField("read", "Read", 5, INTEGER);
        xclass.addStaticListField("tags", "Tags", 1, true, true, "", null, null);
    }
}
