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

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Default implementation of the Rating interface.
 * 
 * @see Rating
 * @version $Id$
 * @since 6.4M3
 */
public class DefaultRating implements Rating
{
    private DocumentReference documentRef;

    private XWikiDocument document;

    private BaseObject object;

    private XWikiContext context;

    private DefaultRatingsManager ratingsManager;

    /**
     * DefaultRating constructor.
     * 
     * @param documentRef the document to which the rating is linked
     * @param author the user that created the rating
     * @param vote the author's vote
     * @param context the context in which the rating was created
     * @param ratingsManager the ratings manager used when the rating was created
     * @throws XWikiException when failing to create object
     */
    public DefaultRating(DocumentReference documentRef, DocumentReference author, int vote, XWikiContext context,
        DefaultRatingsManager ratingsManager) throws XWikiException
    {
        this(documentRef, author, new Date(), vote, context, ratingsManager);
    }

    /**
     * DefaultRating constructor.
     * 
     * @param documentRef the document to which the rating is linked
     * @param author the user that created the rating
     * @param date the date when the rating took place
     * @param vote the author's vote
     * @param context the context in which the rating was created
     * @param ratingsManager the ratings manager used when the rating was created
     * @throws XWikiException when failing to create object
     */
    public DefaultRating(DocumentReference documentRef, DocumentReference author, Date date, int vote,
        XWikiContext context, DefaultRatingsManager ratingsManager) throws XWikiException
    {
        this.context = context;
        this.documentRef = documentRef;
        this.ratingsManager = ratingsManager;

        createObject(this.ratingsManager.entityReferenceSerializer.serialize(documentRef),
            this.ratingsManager.entityReferenceSerializer.serialize(author), date, vote);
    }

    /**
     * DefaultRating constructor.
     * 
     * @param documentRef the document to which the rating is linked
     * @param obj the rating object containing the rating information
     * @param context the context in which the rating was created
     * @param ratingsManager the ratings manager used when the rating was created
     */
    public DefaultRating(DocumentReference documentRef, BaseObject obj, XWikiContext context,
        DefaultRatingsManager ratingsManager)
    {
        this.ratingsManager = ratingsManager;
        this.context = context;
        this.documentRef = documentRef;
        this.document = getDocument();
        this.object = obj;
    }

    /**
     * RatingId represents the rating ID. It is the object number in the default ratings case.
     * 
     * @return the rating id
     */
    @Override
    public String getRatingId()
    {
        return "" + object.getNumber();
    }

    /**
     * RatingId represents the rating ID. It is the object number in the default ratings case.
     * 
     * @return the global rating id
     */
    @Override
    public String getGlobalRatingId()
    {
        return document.getFullName() + ":" + object.getNumber();
    }

    /**
     * Gets the object representation of the rating.
     * 
     * @return an object containing the rating information
     */
    @Override
    public BaseObject getAsObject()
    {
        return object;
    }

    /**
     * Gets the document to which the rating is linked from the document reference.
     * 
     * @return the document to which the rating belongs to
     */
    public XWikiDocument getDocument()
    {
        if (document == null) {
            try {
                document = context.getWiki().getDocument(this.documentRef, context);
            } catch (XWikiException e) {
                // do nothing
            }
        }
        return document;
    }

    @Override
    public DocumentReference getAuthor()
    {
        String objectVal = object.getStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR);
        return this.ratingsManager.userReferenceResolver.resolve(objectVal, documentRef);
    }

    @Override
    public void setAuthor(DocumentReference author)
    {
        object.setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR,
            this.ratingsManager.entityReferenceSerializer.serialize(author));
    }

    @Override
    public Date getDate()
    {
        return object.getDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE);
    }

    @Override
    public void setDate(Date date)
    {
        object.setDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE, date);
    }

    @Override
    public int getVote()
    {
        return object.getIntValue(RatingsManager.RATING_CLASS_FIELDNAME_VOTE);
    }

    @Override
    public void setVote(int vote)
    {
        object.setIntValue("vote", vote);
    }

    @Override
    public Object get(String propertyName)
    {
        try {
            return ((BaseProperty) getAsObject().get(propertyName)).getValue();
        } catch (XWikiException e) {
            return null;
        }
    }

    @Override
    public String display(String propertyName, String mode)
    {
        return document.display(propertyName, mode, object, context);
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.documentRef;
    }

    @Override
    public void save() throws RatingsException
    {
        try {
            // Force content dirty to false, so that the content update date is not changed when saving the document.
            // This should not be handled there, since it is not the responsibility of this plugin to decide if
            // the content has actually been changed or not since current revision, but the implementation of
            // this in XWiki core is wrong. See https://jira.xwiki.org/XWIKI-2800 for more details.
            // There is a draw-back to doing this, being that if the document content is being changed before
            // the document is rated, the contentUpdateDate will not be modified. Although possible, this is very
            // unlikely to happen, or to be a use case. The default rating application will use an asynchronous service
            // to
            // note a document, which service will only set the rating, so the behavior will be correct.
            getDocument().setContentDirty(false);
            context.getWiki().saveDocument(getDocument(), context);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public boolean remove()
    {
        return remove(true);
    }

    /**
     * Remove the rating.
     * 
     * @param withSave save the document after removing the rating or not
     * @return the status of the action
     */
    protected boolean remove(boolean withSave)
    {
        try {
            XWikiDocument doc = getDocument();
            if (!doc.removeXObject(object)) {
                return false;
            } else {
                // save is needed to remove effectively
                if (withSave) {
                    context.getWiki().saveDocument(doc, context);
                }
                return true;
            }
        } catch (XWikiException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Creates a new ratings object.
     * 
     * @param documentName the document being rated
     * @param author the author of the rating
     * @param date the date when the rating took place
     * @param vote the vote that the author gave
     * @throws XWikiException when failig to create new object
     */
    private void createObject(String documentName, String author, Date date, int vote) throws XWikiException
    {
        XWikiDocument doc = getDocument();

        BaseObject obj = doc.newXObject(RatingsManager.RATINGS_CLASSREFERENCE, context);

        // read data from map
        obj.setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR, author);
        obj.setDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE, date);
        obj.setIntValue(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, vote);
        obj.setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_PARENT, documentName);

        // set the internal variable
        object = obj;
    }

    /**
     * Creates a string representation of the rating.
     * 
     * @return the string representation of the rating
     */
    @Override
    public String toString()
    {
        boolean shouldAddSpace = false;
        StringBuffer sb = new StringBuffer();
        if (getAuthor() != null) {
            sb.append("\nAuthor=").append(getAuthor());
            shouldAddSpace = true;
        }
        if (getDate() != null) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("\nDate=").append(getDate());
            shouldAddSpace = true;
        }
        if (getVote() != -1) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("\nVote=").append(getVote()).append("\n");
            shouldAddSpace = true;
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultRating that = (DefaultRating) o;

        return new EqualsBuilder()
            .append(documentRef, that.documentRef)
            .append(document, that.document)
            .append(object, that.object)
            .append(context, that.context)
            .append(ratingsManager, that.ratingsManager)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(documentRef)
            .append(document)
            .append(object)
            .append(context)
            .append(ratingsManager)
            .toHashCode();
    }
}
