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
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.ratings.Rating;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

/**
 * @version $Id$
 * @see Rating
 * @since 6.4M3
 */
public class SeparatePageRating implements Rating
{
    private DocumentReference documentRef;

    private XWikiDocument document;

    private XWikiContext context;

    private SeparatePageRatingsManager ratingsManager;

    /**
     * SeparatePageRating constructor.
     * 
     * @param documentRef the reference of the document with which the rating is associated
     * @param author the author of the rating
     * @param vote the authors vote
     * @param context the context in which the rating took place
     * @param ratingsManager the RatingsManager to be used in processing the rating
     * @throws RatingsException when encountering an error while instantiating a SeparatePageRating
     */
    public SeparatePageRating(DocumentReference documentRef, DocumentReference author, int vote, XWikiContext context,
        SeparatePageRatingsManager ratingsManager) throws RatingsException
    {
        this(documentRef, author, new Date(), vote, context, ratingsManager);
    }

    /**
     * SeparatePageRating constructor.
     * 
     * @param documentRef the reference of the document with which the rating is associated
     * @param author the author of the rating
     * @param date the date when the rating took place
     * @param vote the authors vote
     * @param context the context in which the rating took place
     * @param ratingsManager the RatingsManager to be used in processing the rating
     * @throws RatingsException when encountering an error while instantiating a SeparatePageRating
     */
    public SeparatePageRating(DocumentReference documentRef, DocumentReference author, Date date, int vote,
        XWikiContext context, SeparatePageRatingsManager ratingsManager) throws RatingsException
    {
        this.context = context;
        this.ratingsManager = ratingsManager;
        this.documentRef = documentRef;
        this.document = addDocument(documentRef, author, date, vote);
    }

    /**
     * SeparatePageRating constructor.
     * 
     * @param documentRef the document reference of the document with which the rating is associated
     * @param doc the actual document which will store the rating information.
     * @param context the context in which the rating took place
     * @param ratingsManager the RatingsManager to be used in processing the rating
     * @throws RatingsException when encountering an error while instantiating a SeparatePageRating
     */
    public SeparatePageRating(DocumentReference documentRef, XWikiDocument doc, XWikiContext context,
        SeparatePageRatingsManager ratingsManager) throws RatingsException
    {
        this.context = context;
        this.ratingsManager = ratingsManager;
        this.documentRef = documentRef;
        this.document = doc;
    }

    @Override
    public String getRatingId()
    {
        return getDocument().getFullName();
    }

    @Override
    public String getGlobalRatingId()
    {
        return getRatingId();
    }

    @Override
    public BaseObject getAsObject()
    {
        return getDocument().getXObject(RatingsManager.RATINGS_CLASSREFERENCE);
    }

    private XWikiDocument getDocument()
    {
        // TODO This lazy loading doesn't feel right since the document is always supposed to be there.
        // Not changing it yet since the code is badly covered and I'm afraid of making a regression...
        if (document == null) {
            try {
                document = context.getWiki().getDocument(
                    this.ratingsManager.getRatingDocumentReference(this.documentRef), context);
            } catch (XWikiException e) {
                return null;
            }
        }
        return document;
    }

    @Override
    public DocumentReference getAuthor()
    {
        String objectVal = getAsObject().getStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR);
        return ratingsManager.userReferenceResolver.resolve(objectVal, documentRef);
    }

    @Override
    public Date getDate()
    {

        return getAsObject().getDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE);
    }

    @Override
    public void setAuthor(DocumentReference author)
    {
        getAsObject().setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR,
            ratingsManager.entityReferenceSerializer.serialize(author));
    }

    @Override
    public void setDate(Date date)
    {
        getAsObject().setDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE, date);
    }

    @Override
    public int getVote()
    {
        return getAsObject().getIntValue(RatingsManager.RATING_CLASS_FIELDNAME_VOTE);
    }

    @Override
    public void setVote(int vote)
    {
        getAsObject().setIntValue(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, vote);
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
        return document.display(propertyName, mode, getAsObject(), context);
    }

    @Override
    public void save() throws RatingsException
    {
        DocumentReference superadmin =
            new DocumentReference("xwiki", XWiki.SYSTEM_SPACE, XWikiRightService.SUPERADMIN_USER);

        try {
            if (document == null) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_SAVERATING_NULLDOCUMENT,
                    "Cannot save invalid separate page rating, the rating document is null");
            }

            document.setCreatorReference(superadmin);
            document.setAuthorReference(superadmin);
            ContextualLocalizationManager localization = Utils.getComponent(ContextualLocalizationManager.class);
            context.getWiki().saveDocument(getDocument(), localization.getTranslationPlain("rating.saveComment"), true,
                context);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public boolean remove() throws RatingsException
    {
        try {
            XWikiDocument doc = getDocument();
            // remove the rating
            context.getWiki().deleteDocument(doc, context);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
        return true;
    }

    /**
     * Adds a new document in which to store ratings.
     * 
     * @param documentRef reference to the document with which the rating is associated
     * @param author the author of the rating
     * @param date the date when the rating was done
     * @param vote the author's vote
     * @return the newly created document
     * @throws RatingsException when an error occurs while creating the document
     */
    private XWikiDocument addDocument(DocumentReference documentRef, DocumentReference author, Date date, int vote)
        throws RatingsException
    {
        try {
            DocumentReference pageRef = this.ratingsManager.getRatingDocumentReference(documentRef);
            String parentDocName = ratingsManager.entityReferenceSerializer.serialize(documentRef);
            XWiki xwiki = context.getWiki();
            XWikiDocument doc = xwiki.getDocument(pageRef, context);
            doc.setParent(parentDocName);
            doc.setHidden(true);
            BaseObject obj = doc.newXObject(RatingsManager.RATINGS_CLASSREFERENCE, context);
            obj.setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR,
                ratingsManager.entityReferenceSerializer.serialize(author));
            obj.setDateValue(RatingsManager.RATING_CLASS_FIELDNAME_DATE, date);
            obj.setIntValue(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, vote);
            obj.setStringValue(RatingsManager.RATING_CLASS_FIELDNAME_PARENT, parentDocName);

            return doc;
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

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
        if (getVote() != 0) {
            sb.append(shouldAddSpace ? " " : "");
            sb.append("\nVote=").append(getVote()).append("\n");
            shouldAddSpace = true;
        }

        return sb.toString();
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.documentRef;
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

        SeparatePageRating that = (SeparatePageRating) o;

        return new EqualsBuilder()
            .append(documentRef, that.documentRef)
            .append(document, that.document)
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
            .append(context)
            .append(ratingsManager)
            .toHashCode();
    }
}
