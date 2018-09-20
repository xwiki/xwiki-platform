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

import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.captcha.Captcha;
import org.xwiki.captcha.CaptchaConfiguration;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(CommentAddAction.class);

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
            getCurrentScriptContext().setAttribute("captchaAnswerWrong", Boolean.TRUE, ScriptContext.ENGINE_SCOPE);
        } else {
            // className = XWiki.XWikiComments
            String className = baseclass.getName();
            // Create a new comment object and mark the document as dirty.
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
                // We need to make sure the author will fit in a String property, this is mostly a protection against
                // spammers who try to put large texts in this field
                author = author.substring(0, Math.min(author.length(), 255));
                object.set(AUTHOR_PROPERTY_NAME, author, context);
            } else {
                // A registered user must always post with his name.
                object.set(AUTHOR_PROPERTY_NAME, context.getUser(), context);
            }
            doc.setAuthorReference(context.getUserReference());

            // Save the new comment.
            xwiki.saveDocument(doc, localizePlainOrKey("core.comment.addComment"), true, context);
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
     * Checks the request and validates the CAPTCHA answer, if needed, against the CAPTCHA module. This makes xwiki-core
     * dependent on xwiki-captcha and should be removed as soon as possible.
     *
     * @param context The XWikiContext for getting the request and whether guest comment requires a CAPTCHA
     * @return true if the CAPTCHA answer is correct or if CAPTCHA is not required
     * @throws XWikiException if something goes wrong in the CAPTCHA module
     * @since 2.3M1
     */
    private boolean checkCaptcha(XWikiContext context) throws XWikiException
    {
        if (context.getWiki().getSpacePreferenceAsInt("guest_comment_requires_captcha", 0, context) == 1) {
            CaptchaConfiguration captchaConfiguration =
                Utils.getComponent(org.xwiki.captcha.CaptchaConfiguration.class);
            String defaultCaptchaName = captchaConfiguration.getDefaultName();
            try {
                Captcha captcha = Utils.getComponent(org.xwiki.captcha.Captcha.class, defaultCaptchaName);

                return captcha.isValid();
            } catch (Exception e) {
                LOGGER.error("Failed to verify CAPTCHA of type [{}]. Assuming wrong answer.", defaultCaptchaName, e);
                return false;
            }
        } else {
            return true;
        }
    }
}
