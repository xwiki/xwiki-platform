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

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Action used to post a comment on a page, adds a comment object to the document and saves it, requires comment right
 * but not edit right.
 * 
 * @version $Id$
 */
public class CommentAddAction extends XWikiAction
{
    /** The name of the XWikiComments property identifying the author. */
    private static final String AUTHOR_PROPERTY_NAME = "author";

    /** The name of the space where user profiles are kept. */
    private static final String USER_SPACE_PREFIX = "XWiki.";

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        ObjectAddForm oform = (ObjectAddForm) context.getForm();

        // Make sure this class exists
        BaseClass baseclass = xwiki.getCommentsClass(context);
        if (doc.isNew()) {
            return true;
        } else if (context.getUser().equals(XWikiRightService.GUEST_USER_FULLNAME) && !checkCaptcha(context)) {
            ((VelocityContext) context.get("vcontext")).put("captchaAnswerWrong", Boolean.TRUE);
        } else {
            // className = XWiki.XWikiComments
            String className = baseclass.getName();
            BaseObject object = doc.newObject(className, context);
            // TODO The map should be pre-filled with empty strings for all class properties, just like in
            // ObjectAddAction, so that properties missing from the request are still added to the database.
            baseclass.fromMap(oform.getObject(className), object);
            // Comment author checks
            if (XWikiRightService.GUEST_USER_FULLNAME.equals(context.getUser())) {
                // Guests should not be allowed to enter names that look like real XWiki user names.
                String author = ((BaseProperty) object.get(AUTHOR_PROPERTY_NAME)).getValue() + "";
                author = StringUtils.remove(author, ':');
                while (author.startsWith(USER_SPACE_PREFIX)) {
                    author = StringUtils.removeStart(author, USER_SPACE_PREFIX);
                }
                object.set(AUTHOR_PROPERTY_NAME, author, context);
            } else {
                // A registered user must always post with his name.
                object.set(AUTHOR_PROPERTY_NAME, context.getUser(), context);
            }
            doc.setAuthor(context.getUser());
            // Consider comments not being content.
            doc.setContentDirty(false);
            // if contentDirty is false, in order for the change to create a new version metaDataDirty must be true.
            doc.setMetaDataDirty(true);
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

    /**
     * Checks the request parameter captcha_answer against the captcha module. This makes xwiki-core dependant on
     * xwiki-captcha and should be removed as soon as possible.
     * 
     * @param context The XWikiContext for getting the request and whether guest comment requires a captcha.
     * @return true if the captcha answer is correct or if no captcha answer and captcha is not required.
     * @throws XWikiException if something goes wrong in the captcha module.
     * @since 2.3M1
     */
    private boolean checkCaptcha(XWikiContext context) throws XWikiException
    {
        String answer = context.getRequest().get("captcha_answer");
        if (answer != null && answer.length() > 0) {
            org.xwiki.captcha.CaptchaVerifier cv =
                Utils.getComponent(org.xwiki.captcha.CaptchaVerifier.class, context.getRequest().get("captcha_type"));
            try {
                return cv.isAnswerCorrect(cv.getUserId(context.getRequest()), answer);
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Exception while attempting to verify captcha", e);
            }
        } else {
            return (context.getWiki().getSpacePreferenceAsInt("guest_comment_requires_captcha", 0, context) != 1);
        }
    }
}
