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
package com.xpn.xwiki.web;



import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Action used to edit+save an existing comment in a page, saves the comment
 * object in the document, requires comment right but not edit right.
 * 
 * @version $Id$
 */
public class CommentSaveAction extends XWikiAction
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(CommentSaveAction.class);

    /** The name of the XWikiComments property identifying the author. */
    private static final String AUTHOR_PROPERTY_NAME = "author";

    /** The name of the space where user profiles are kept. */
    private static final String USER_SPACE_PREFIX = "XWiki.";


    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {

        LOGGER.info("Action! " + context.getAction());
        // dit wil je:
        // doc.readObjectsFromForm(EditForm eform, XWikiContext context) throws XWikiException

        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        EditForm eform = (EditForm) context.getForm();

        // Make sure this class exists
        BaseClass baseclass = xwiki.getCommentsClass(context);
        if (doc.isNew()) {
            return true;
        } else {
            doc.readObjectsFromForm(eform, context);
            xwiki.saveDocument(doc, context.getMessageTool().get("core.comment.addComment"), true, context);
        }
        // If xpage is specified then allow the specified template to be parsed.
        if (context.getRequest().get("xpage") != null) {
            return true;
        }
        // forward to edit
        String redirect = Utils.getRedirect("edit", context);
        sendRedirect(response, redirect);
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        if (context.getDoc().isNew()) {
            context.put("message", "nocommentwithnewdoc");
            return "exception";
        }
        return "";
    }

}
