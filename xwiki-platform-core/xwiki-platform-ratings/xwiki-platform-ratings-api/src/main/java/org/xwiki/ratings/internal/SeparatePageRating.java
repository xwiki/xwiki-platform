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
     * @param doc the document with which the rating is associated
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
        if (document == null) {
            try {
                document = context.getWiki().getDocument(getPageReference(this.documentRef), context);
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
            document.setCreatorReference(superadmin);
            document.setAuthorReference(superadmin);
            context.getWiki().saveDocument(getDocument(), context.getMessageTool().get("rating.saveComment"), true, context);
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
     * Generate page name from the container page We add Rating and getUniquePageName will add us a counter to our page.
     * 
     * @param documentRef reference to the document with which the rating is associated
     * @return a reference to the document in which the rating is stored
     */
    private DocumentReference getPageReference(DocumentReference documentRef) throws XWikiException
    {
        XWikiDocument doc = context.getWiki().getDocument(documentRef, context);
        String ratingsSpace = ratingsManager.getRatingsSpaceName(documentRef);
        String pageSufix = "R";
        boolean hasRatingsSpaceForeachSpace = ratingsManager.hasRatingsSpaceForeachSpace(documentRef);
        if (hasRatingsSpaceForeachSpace) {
            return new DocumentReference(context.getWikiId(), doc.getSpace() + ratingsSpace, getUniquePageName(
                ratingsSpace, doc.getName(), pageSufix, true));
        } else if (ratingsSpace == null) {
            return new DocumentReference(context.getWikiId(), doc.getSpace(), getUniquePageName(doc.getSpace(),
                doc.getName() + pageSufix, "", true));
        } else {
            return new DocumentReference(context.getWikiId(), ratingsSpace, getUniquePageName(ratingsSpace,
                doc.getSpace() + "_" + doc.getName(), pageSufix, true));
        }
    }

    /**
     * Gets a unique page name.
     * 
     * @param space the space in which the document should be
     * @param name the name of the document
     * @param postfix post fix to add to the document name
     * @param forcepostfix force post fix or not
     * @return the unique document name
     */
    private String getUniquePageName(String space, String name, String postfix, boolean forcepostfix)
    {
        String pageName = context.getWiki().clearName(name, context);
        if (forcepostfix || context.getWiki().exists(space + '.' + pageName, context)) {
            int i = 1;
            while (context.getWiki().exists(space + '.' + pageName + postfix + i, context)) {
                i++;
            }
            return pageName + postfix + i;
        }
        return pageName;
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
            DocumentReference pageRef = getPageReference(documentRef);
            String parentDocName = ratingsManager.entityReferenceSerializer.serialize(documentRef);
            XWiki xwiki = context.getWiki();
            XWikiDocument doc = xwiki.getDocument(pageRef, context);
            doc.setParent(parentDocName);
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
}
