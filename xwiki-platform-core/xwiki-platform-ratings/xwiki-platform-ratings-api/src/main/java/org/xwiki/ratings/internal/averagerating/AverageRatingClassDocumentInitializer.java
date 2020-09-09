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
package org.xwiki.ratings.internal.averagerating;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

import org.xwiki.ratings.internal.averagerating.AverageRatingManager.AverageRatingQueryField;

/**
 * Initialize average rating class.
 * 
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Named(AverageRatingClassDocumentInitializer.AVERAGE_RATINGS_CLASSNAME)
@Singleton
public class AverageRatingClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    static final String AVERAGE_RATINGS_CLASSPAGE = "AverageRatingsClass";

    static final String AVERAGE_RATINGS_CLASSNAME = XWiki.SYSTEM_SPACE + '.' + AVERAGE_RATINGS_CLASSPAGE;

    static final LocalDocumentReference AVERAGE_RATINGS_CLASSREFERENCE =
        new LocalDocumentReference("XWiki", AVERAGE_RATINGS_CLASSPAGE);

    private static final String INTEGER_TYPE = "integer";

    /**
     * Default constructor.
     */
    public AverageRatingClassDocumentInitializer()
    {
        super(AVERAGE_RATINGS_CLASSREFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addNumberField(AverageRatingQueryField.TOTAL_VOTE.getFieldName(), "Number of Votes", 5, INTEGER_TYPE);
        xclass.addNumberField(AverageRatingQueryField.AVERAGE_VOTE.getFieldName(), "Average Vote", 5, "float");
        xclass.addTextField(AverageRatingQueryField.MANAGER_ID.getFieldName(), "Manager Identifier", 100);
        xclass.addTextField(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), "Voted Entity Reference", 255);
        xclass.addTextField(AverageRatingQueryField.ENTITY_TYPE.getFieldName(), "Voted entity type", 100);
        xclass.addNumberField(AverageRatingQueryField.SCALE.getFieldName(), "Scale of votes", 3, INTEGER_TYPE);
        xclass.addDateField(AverageRatingQueryField.UPDATED_AT.getFieldName(), "Date of last update");
    }
}
