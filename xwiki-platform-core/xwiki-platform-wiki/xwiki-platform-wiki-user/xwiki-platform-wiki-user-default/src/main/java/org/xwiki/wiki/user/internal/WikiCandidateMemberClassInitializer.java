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
package org.xwiki.wiki.user.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update the WikiManager.WikiCandidateMemberClass document with all required information.
 *
 * @since 5.3RC1
 * @version $Id$
 */
@Component
@Named("WikiManager.WikiCandidateMemberClass")
@Singleton
public class WikiCandidateMemberClassInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * The name of the mandatory document.
     */
    public static final String DOCUMENT_NAME = "WikiCandidateMemberClass";

    /**
     * The space of the mandatory document.
     */
    public static final String DOCUMENT_SPACE = "WikiManager";

    /**
     * The local reference of the class.
     */
    public static final LocalDocumentReference REFERENCE = new LocalDocumentReference(DOCUMENT_SPACE, DOCUMENT_NAME);

    /**
     * Name of field <code>userName</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELD_USER = "userName";

    /**
     * Pretty name of field <code>userName</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDPN_USERNAME = "User Name";

    /**
     * Name of field <code>date</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELD_DATE_OF_CREATION = "date";

    /**
     * Pretty name of field <code>date</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDPN_DATE = "Date";

    /**
     * Name of field <code>userComment</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELD_USER_COMMENT = "userComment";

    /**
     * Pretty name of field <code>userComment</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDPN_USERCOMMENT = "User Comment";

    /**
     * Name of field <code>status</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELD_STATUS = "status";

    /**
     * Pretty name of field <code>status</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDPN_STATUS = "Status";

    /**
     * List of possible values for <code>status</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDL_STATUS = "pending=Pending|accepted=Accepted|rejected=Rejected";

    /**
     * Name of field <code>resolutionDate</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELD_DATE_OF_CLOSURE = "resolutionDate";

    /**
     * Pretty name of field <code>resolutionDate</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDPN_RESOLUTIONDATE = "Resolution Date";

    /**
     * Name of field <code>reviewer</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELD_ADMIN = "reviewer";

    /**
     * Pretty name of field <code>reviewer</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDPN_REVIEWER = "Reviewer";

    /**
     * Name of field <code>reviewerComment</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELD_ADMIN_COMMENT = "reviewerComment";

    /**
     * Pretty name of field <code>reviewerComment</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDPN_REVIEWERCOMMENT = "Reviewer's Comment";

    /**
     * Name of field <code>reviewerPrivateComment</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELD_ADMIN_PRIVATE_COMMENT = "reviewerPrivateComment";

    /**
     * Pretty name of field <code>reviewerPrivateComment</code> for the XWiki class
     * WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDPN_REVIEWERPRIVATECOMMENT = "Reviewer's Private Reason";

    /**
     * Name of field <code>type</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELD_TYPE = "type";

    /**
     * Pretty name of field <code>type</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDPN_TYPE = "Type";

    /**
     * List of possible values for <code>type</code> for the XWiki class WikiManager.WikiCandidateMemberClass.
     */
    public static final String FIELDL_TYPE = "request=Request|invitation=Invitation";

    /**
     * Constructor.
     */
    public WikiCandidateMemberClassInitializer()
    {
        super(REFERENCE);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        // Add missing class fields
        BaseClass baseClass = document.getXClass();

        needsUpdate |= baseClass.addTextField(FIELD_USER, FIELDPN_USERNAME, 30);
        needsUpdate |= baseClass.addDateField(FIELD_DATE_OF_CREATION, FIELDPN_DATE);
        needsUpdate |= baseClass.addTextAreaField(FIELD_USER_COMMENT, FIELDPN_USERCOMMENT, 40, 3);
        needsUpdate |= baseClass.addStaticListField(FIELD_STATUS, FIELDPN_STATUS, FIELDL_STATUS);
        needsUpdate |= baseClass.addDateField(FIELD_DATE_OF_CLOSURE, FIELDPN_RESOLUTIONDATE);
        needsUpdate |= baseClass.addTextField(FIELD_ADMIN, FIELDPN_REVIEWER, 30);
        needsUpdate |= baseClass.addTextAreaField(FIELD_ADMIN_COMMENT, FIELDPN_REVIEWERCOMMENT, 40, 3);
        needsUpdate |= baseClass.addTextAreaField(FIELD_ADMIN_PRIVATE_COMMENT, FIELDPN_REVIEWERPRIVATECOMMENT, 40, 3);
        needsUpdate |= baseClass.addStaticListField(FIELD_TYPE, FIELDPN_TYPE, FIELDL_TYPE);

        // Check if the document is hidden
        if (!document.isHidden()) {
            document.setHidden(true);
            needsUpdate = true;
        }

        // Mark this document as Wiki Class.
        if (document.isNew()) {
            needsUpdate |= setClassDocumentFields(document, "Wiki Candidate Member Class");
            document.setContent(document.getContent() + "\n\nClass that represents a candidacy to become a member "
                    + "of the wiki.");
        }

        return needsUpdate;
    }
}
