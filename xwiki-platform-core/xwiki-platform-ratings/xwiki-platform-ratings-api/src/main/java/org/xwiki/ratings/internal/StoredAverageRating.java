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

import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @see AverageRating
 */
public class StoredAverageRating implements AverageRating
{
    private XWikiDocument document;

    private BaseObject object;

    private XWikiContext context;

    /**
     * StoredAverageRating constructor.
     * 
     * @param document the document with which the rating is associated
     * @param ratingObject the Rating object
     * @param context the context
     */
    public StoredAverageRating(XWikiDocument document, BaseObject ratingObject, XWikiContext context)
    {
        this.document = document;
        this.context = context;
        this.object = ratingObject;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#getNbVotes()
     */
    public int getNbVotes()
    {
        return object.getIntValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_NBVOTES);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#setNbVotes()
     */
    public void setNbVotes(int nbVotes)
    {
        object.setIntValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_NBVOTES, nbVotes);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#getAverageVote()
     */
    public float getAverageVote()
    {
        return object.getFloatValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#setAverageVote()
     */
    public void setAverageVote(float averageVote)
    {
        object.setFloatValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE, averageVote);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#getMethod()
     */
    public String getMethod()
    {
        return object.getStringValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#setMethod()
     */
    public void setMethod(String method)
    {
        object.setStringValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD, method);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.ratings.AverageRating#save()
     */
    public void save() throws RatingsException
    {
        try {
            // Force content dirty to false, so that the content update date is not changed when saving the document.
            // This should not be handled there, since it is not the responsibility of this plugin to decide if
            // the content has actually been changed or not since current revision, but the implementation of
            // this in XWiki core is wrong. See http://jira.xwiki.org/jira/XWIKI-2800 for more details.
            // There is a draw-back to doing this, being that if the document content is being changed before
            // the document is rated, the contentUpdateDate will not be modified. Although possible, this is very
            // unlikely to happen, or to be a use case. The default rating application will use an asynchronous service
            // to
            // note a document, which service will only set the rating, so the behavior will be correct.
            document.setContentDirty(false);
            context.getWiki().saveDocument(document, context);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }
}
