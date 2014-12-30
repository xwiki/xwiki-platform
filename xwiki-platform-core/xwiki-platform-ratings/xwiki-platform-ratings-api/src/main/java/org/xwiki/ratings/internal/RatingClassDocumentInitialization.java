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
package org.xwiki.ratings.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.ratings.RatingsManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Initialize rating class.
 * 
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Named(RatingsManager.RATINGS_CLASSNAME)
@Singleton
public class RatingClassDocumentInitialization extends AbstractMandatoryDocumentInitializer
{
    /**
     * Default constructor.
     */
    public RatingClassDocumentInitialization()
    {
        super(XWiki.SYSTEM_SPACE, RatingsManager.RATINGS_CLASSPAGE);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        BaseClass bclass = document.getXClass();

        needsUpdate |= bclass.addTextField(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR, "Author", 30);
        needsUpdate |= bclass.addNumberField(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, "Vote", 5, "integer");
        needsUpdate |= bclass.addDateField(RatingsManager.RATING_CLASS_FIELDNAME_DATE, "Date");
        needsUpdate |= bclass.addTextField(RatingsManager.RATING_CLASS_FIELDNAME_PARENT, "Parent", 30);

        needsUpdate = setClassDocumentFields(document, "XWiki Ratings Class");

        return needsUpdate;
    }
}
