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
package com.xpn.xwiki.internal.mandatory;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.RegexEntityReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Update XWiki.XWikiComments document with all required informations.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named(XWikiCommentsDocumentInitializer.LOCAL_REFERENCE_STRING)
@Singleton
public class XWikiCommentsDocumentInitializer extends AbstractCommentsDocumentInitializer
{
    /**
     * The name of the initialized document.
     * 
     * @since 9.4RC1
     */
    public static final String NAME = "XWikiComments";

    /**
     * The local reference of the initialized document as String.
     * 
     * @since 9.4RC1
     */
    public static final String LOCAL_REFERENCE_STRING = XWiki.SYSTEM_SPACE + '.' + NAME;

    /**
     * The local reference of the initialized document as String.
     * 
     * @since 9.4RC1
     */
    public static final LocalDocumentReference LOCAL_REFERENCE = new LocalDocumentReference(XWiki.SYSTEM_SPACE, NAME);

    /**
     * A regex to match any object reference with initialized class.
     * 
     * @since 9.4RC1
     */
    public static final RegexEntityReference OBJECT_REFERENCE = BaseObjectReference.any(LOCAL_REFERENCE_STRING);

    /**
     * Default constructor.
     */
    public XWikiCommentsDocumentInitializer()
    {
        super(LOCAL_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        super.createClass(xclass);

        xclass.addTextAreaField("highlight", "Highlighted Text", 40, 2);
        xclass.addNumberField("replyto", "Reply To", 5, "integer");

        String commentPropertyName = "comment";
        xclass.addTextAreaField(commentPropertyName, "Comment", 40, 5, true);

        // FIXME: Ensure that the comment text editor is set to its default value after an upgrade. This should be
        // handled in a cleaner way in BaseClass#addTextAreaField. See: https://jira.xwiki.org/browse/XWIKI-17605
        TextAreaClass comment =  (TextAreaClass) xclass.getField(commentPropertyName);
        comment.setEditor((String) null);
    }
}
