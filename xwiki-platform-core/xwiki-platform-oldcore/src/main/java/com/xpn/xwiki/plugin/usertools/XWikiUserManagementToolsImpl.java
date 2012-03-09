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
package com.xpn.xwiki.plugin.usertools;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.PluginException;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import org.apache.velocity.VelocityContext;
import org.xwiki.rendering.syntax.Syntax;

public class XWikiUserManagementToolsImpl extends XWikiDefaultPlugin implements XWikiPluginInterface,
    XWikiUserManagementTools
{
    public static final String DEFAULT_USER_SPACE = "XWiki";

    public static final String DEFAULT_INVITATION_MESSAGE_PAGE = "XWiki.InvitationMessage";

    public static final String DEFAULT_REINVITATION_MESSAGE_PAGE = "XWiki.ReinvitationMessage";

    public static final String DEFAULT_USER_CLASS = "XWiki.XWikiUsers";

    public static final String DEFAULT_USERTEMPLATE_CLASS = "XWiki.XWikiUsersTemplate";

    public static final int ERROR_XWIKI_EMAIL_INVALID_EMAIL = 1;

    public static final int ERROR_XWIKI_EMAIL_INVALID_ADMIN_EMAIL = 2;

    public static final int ERROR_XWIKI_USER_PAGE_ALREADY_EXIST = 3;

    public static final int ERROR_XWIKI_EMAIL_CANNOT_PREPARE_VALIDATION_EMAIL = 4;

    public XWikiUserManagementToolsImpl(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    @Override
    public String getName()
    {
        return "usermanagementtools";
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new XWikiUserManagementToolsAPI((XWikiUserManagementTools) plugin, context);
    }

    @Override
    public String inviteUser(String name, String email, XWikiContext context) throws XWikiException
    {
        if (!isValidEmail(email))
            throw new PluginException(getName(), ERROR_XWIKI_EMAIL_INVALID_EMAIL, "The email is not valid");

        Document userdoc = createUserDocument(name, email, context);
        userdoc.use(DEFAULT_USER_CLASS);
        userdoc.set("last_name", name);
        userdoc.set("email", email);

        userdoc.saveWithProgrammingRights();
        context.getWiki().setUserDefaultGroup(userdoc.getFullName(), context);
        BaseObject userObj = userdoc.getObject(DEFAULT_USER_CLASS).getXWikiObject();
        XWikiDocument doc = context.getWiki().getDocument(getInvitationMessageDocument(context), context);
        String message =
            prepareInvitationMessage(doc, name, userObj.getStringValue("password"), email,
                userObj.getStringValue("validkey"), userdoc.getURL(), context);

        String sender;
        try {
            sender = context.getWiki().getXWikiPreference("admin_email", context);
        } catch (Exception e) {
            throw new PluginException(getName(), ERROR_XWIKI_EMAIL_INVALID_ADMIN_EMAIL,
                "Exception while reading the validation email config", e, null);

        }

        context.getWiki().sendMessage(sender, email, message, context);
        return userdoc.getFullName();
    }

    @Override
    public boolean isValidEmail(String email)
    {
        if ((email == null) || (email == "") || (email.indexOf('@') == -1)) {
            return false;
        }
        return true;
    }

    protected Document createUserDocument(String name, String email, XWikiContext context) throws XWikiException
    {
        String pageName;
        if (context.getWiki().getConvertingUserNameType(context).equals("0"))
            pageName = getUserPage(name, context);
        else
            pageName = getUserPage(email, context);
        XWikiDocument userDoc = context.getWiki().getDocument(pageName, context);
        if (!userDoc.isNew()) {
            throw new PluginException(getName(), ERROR_XWIKI_USER_PAGE_ALREADY_EXIST,
                "This document already exist, try another name");
        }
        Document userApiDoc = userDoc.newDocument(context);

        if (!context.getWiki().getDefaultDocumentSyntax().equals(Syntax.XWIKI_1_0.toIdString())) {
            userApiDoc.setContent("{{include document=\"XWiki.XWikiUserSheet\"/}}");
            userApiDoc.setSyntax(Syntax.XWIKI_2_0);
        } else {
            userApiDoc.setContent("#includeForm(\"XWiki.XWikiUserSheet\")");
            userApiDoc.setSyntax(Syntax.XWIKI_1_0);
        }

        String template = DEFAULT_USERTEMPLATE_CLASS;
        if ((template != null) && (!template.equals(""))) {
            XWikiDocument tdoc = context.getWiki().getDocument(template, context);
            if ((!tdoc.isNew())) {
                userApiDoc.setContent(tdoc.getContent());
                userApiDoc.setSyntaxId(tdoc.getSyntaxId());
            }
        }

        String password = getRandomPassword();
        String validkey = context.getWiki().generateValidationKey(16);

        userApiDoc.addObjectFromRequest(DEFAULT_USER_CLASS);
        userApiDoc.set("active", "0");
        userApiDoc.set("password", password);
        userApiDoc.set("validkey", validkey);
        com.xpn.xwiki.api.Object rightobj = userApiDoc.newObject("XWiki.XWikiRights");
        rightobj.set("users", pageName);
        rightobj.set("allow", "1");
        rightobj.set("levels", "edit");
        rightobj.set("groups", "");
        return userApiDoc;
    }

    @Override
    public String getUserPage(String email, XWikiContext context)
    {
        String pageName = context.getWiki().convertUsername(email, context);

        return getUserSpace(context) + "." + pageName;
    }

    @Override
    public String getUserName(String userPage, XWikiContext context) throws XWikiException
    {
        Document doc = context.getWiki().getDocument(userPage, context).newDocument(context);
        doc.use(DEFAULT_USER_CLASS);
        return doc.get("first_name") + " " + doc.get("last_name");
    }

    @Override
    public String getEmail(String userPage, XWikiContext context) throws XWikiException
    {
        Document doc = context.getWiki().getDocument(userPage, context).newDocument(context);
        doc.use(DEFAULT_USER_CLASS);
        return (String) doc.get("email");
    }

    private String getRandomPassword()
    {
        return org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(8);
    }

    private String prepareInvitationMessage(XWikiDocument doc, String name, String password, String email,
        String validationUrl, String validKey, XWikiContext context) throws XWikiException
    {
        String content = doc.getContent();

        try {
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put("name", name);
            vcontext.put("password", password);
            vcontext.put("validationUrl", validationUrl);
            vcontext.put("validKey", validKey);
            vcontext.put("email", email);
            content = context.getWiki().parseContent(content, context);
        } catch (Exception e) {
            throw new PluginException(getName(), ERROR_XWIKI_EMAIL_CANNOT_PREPARE_VALIDATION_EMAIL,
                "Exception while preparing the validation email", e, null);
        }
        return content;
    }

    @Override
    public String getUserSpace(XWikiContext context)
    {
        return DEFAULT_USER_SPACE;
    }

    protected String getInvitationMessageDocument(XWikiContext context)
    {
        return DEFAULT_INVITATION_MESSAGE_PAGE;
    }

    protected String getReinvitationMessageDocument(XWikiContext context)
    {
        return DEFAULT_REINVITATION_MESSAGE_PAGE;
    }

    @Override
    public boolean resendInvitation(String email, XWikiContext context) throws XWikiException
    {
        XWikiDocument userdoc = context.getWiki().getDocument(getUserPage(email, context), context);
        BaseObject userObj = userdoc.getObject(DEFAULT_USER_CLASS);
        XWikiDocument doc = context.getWiki().getDocument(getReinvitationMessageDocument(context), context);
        String message =
            prepareInvitationMessage(doc, getUserName(userdoc.getFullName(), context),
                userObj.getStringValue("password"), email, "", userObj.getStringValue("validkey"), context);

        String sender;
        try {
            sender = context.getWiki().getXWikiPreference("admin_email", context);
        } catch (Exception e) {
            throw new PluginException(getName(), ERROR_XWIKI_EMAIL_INVALID_ADMIN_EMAIL,
                "Exception while reading the validation email config", e, null);

        }

        context.getWiki().sendMessage(sender, email, message, context);
        return true;
    }
}
