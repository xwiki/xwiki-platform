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
 * Initialize average rating class.
 * 
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Named(RatingsManager.AVERAGE_RATINGS_CLASSNAME)
@Singleton
public class AverageRatingClassDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * Default constructor.
     */
    public AverageRatingClassDocumentInitializer()
    {
        super(XWiki.SYSTEM_SPACE, RatingsManager.AVERAGE_RATINGS_CLASSPAGE);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        BaseClass bclass = document.getXClass();

        needsUpdate |=
            bclass
                .addNumberField(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_NBVOTES, "Number of Votes", 5, "integer");
        needsUpdate |=
            bclass.addNumberField(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE, "Average Vote", 5, "float");
        needsUpdate |=
            bclass.addTextField(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD, "Average Vote method",
                10);

        needsUpdate = setClassDocumentFields(document, "XWiki Average Ratings Class");

        return needsUpdate;
    }
}
