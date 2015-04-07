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
package org.xwiki.watchlist.internal.documents;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Document initializer for {@value #DOCUMENT_FULL_NAME}.
 * 
 * @version $Id$
 */
@Component
@Named(WatchListJobClassDocumentInitializer.DOCUMENT_FULL_NAME)
@Singleton
public class WatchListJobClassDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * Class document name.
     */
    public static final String DOCUMENT_NAME = "WatchListJobClass";

    /**
     * Class document full name.
     */
    public static final String DOCUMENT_FULL_NAME = XWiki.SYSTEM_SPACE + "." + DOCUMENT_NAME;

    /**
     * Class document reference.
     */
    public static final LocalDocumentReference DOCUMENT_REFERENCE = new LocalDocumentReference(XWiki.SYSTEM_SPACE,
        DOCUMENT_NAME);

    /**
     * Email template field name.
     */
    public static final String TEMPLATE_FIELD = "template";

    /**
     * Last fire time field name.
     */
    public static final String LAST_FIRE_TIME_FIELD = "last_fire_time";

    /**
     * Default constructor.
     */
    public WatchListJobClassDocumentInitializer()
    {
        super(DOCUMENT_REFERENCE);
    }

    @Override
    protected boolean isMainWikiOnly()
    {
        // Main wiki document.
        return true;
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;
        BaseClass bclass = document.getXClass();

        // Class properties
        needsUpdate |=
            bclass.addTextField(TEMPLATE_FIELD, "Document holding the notification message template to use", 80);
        needsUpdate |= bclass.addDateField(LAST_FIRE_TIME_FIELD, "Last notifier fire time", "dd/MM/yyyy HH:mm:ss", 1);

        // Handle the fields and the sheet of the document containing the class.
        needsUpdate |= setClassDocumentFields(document, "XWiki WatchList Notifier Class");

        return needsUpdate;
    }
}
