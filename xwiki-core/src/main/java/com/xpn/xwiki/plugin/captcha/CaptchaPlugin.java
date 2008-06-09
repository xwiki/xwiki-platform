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
 *
 */
package com.xpn.xwiki.plugin.captcha;

import com.octo.captcha.module.config.CaptchaModuleConfigHelper;
import com.octo.captcha.module.struts.CaptchaServicePlugin;
import com.octo.captcha.service.CaptchaService;
import com.octo.captcha.service.CaptchaServiceException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.XWikiRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CaptchaPlugin extends XWikiDefaultPlugin
{
    private static Log mLogger =
        LogFactory.getLog(com.xpn.xwiki.plugin.captcha.CaptchaPlugin.class);

    public CaptchaPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    public String getName()
    {
        return "jcaptcha";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new CaptchaPluginApi((CaptchaPlugin) plugin, context);
    }

    public void flushCache()
    {
    }

    public void init(XWikiContext context)
    {
        super.init(context);
    }

    public String displayCaptcha(String action, String classname, XWikiContext context)
        throws XWikiException
    {
        StringBuffer output = new StringBuffer();
        String user = context.getUser();
        String actionuser = "";
        if (user.equals("XWiki.XWikiGuest")) {
            actionuser = action + "_anonymous";
        } else if (user.equals("XWiki.Admin")) {
            return ""; // not captcha with admin
        } else {
            actionuser = action + "_registered";
        }

        String typecaptcha = context.getWiki().getWebPreference(actionuser, context);

        output.append("<div id=\"captcha\">");
        if (typecaptcha.equalsIgnoreCase("image")) {
            output.append(
                "<table id=\"captchaimage\" border=\"0\" cellspacing=\"0\" cellpadding=\"5\">");
            output.append("<tr><td align=\"left\" width=\"150\">Enter the code shown :</td>");
            output.append(
                "<td><input type=\"text\" name=\"jcaptcha_response\" size=\"29\"></td></tr>");
            output.append(
                "<tr><td nowrap=\"nowrap\" width=\"right\"><spacer type=\"horizontal\" width=\"150\"></td>");
            output.append("<td><img class=\"");
            output.append(classname);
            output.append("\" ");
            output.append("src=\"" + context.getDoc().getURL("jcaptcha", context) + "\">");
            output.append("</td></tr>");
            output.append("</table>");
        } else if (typecaptcha.equalsIgnoreCase("text")) {
            int term1 = (int) Math.floor(Math.random() * 100);
            int term2 = (int) Math.floor(Math.random() * 100);
            int sum = term1 + term2;
            output.append("Please answer this simple math question: ");
            output.append(term1 + " + " + term2 + " = ");
            output.append("<input type='text' name='sum_answer' size='10' >");
            output.append("<input type='hidden' name='sum_result' value='" + sum + "'>");
        }
        output.append("</div>");

        return output.toString();
    }

    public Boolean verifyCaptcha(String action, XWikiContext context) throws XWikiException
    {
        String user = context.getUser();
        String actionuser;
        if (user.equals("XWiki.XWikiGuest")) {
            actionuser = action + "_anonymous";
        } else if (user.equals("XWiki.Admin")) {
            return Boolean.TRUE;  // If is admin then return TRUE for confirm captcha
        } else {
            actionuser = action + "_registered";
        }
        String typecaptcha = context.getWiki().getWebPreference(actionuser, context);

        XWikiRequest request = context.getRequest();
        Boolean isResponseCorrect = Boolean.FALSE;
        if (typecaptcha.equalsIgnoreCase("image")) {
            CaptchaService service = CaptchaServicePlugin.getInstance().getService();
            String responseKey = CaptchaServicePlugin.getInstance().getResponseKey();
            String captchaID = CaptchaModuleConfigHelper.getId(request);
            String challengeResponse = request.getParameter(responseKey);
            request.removeAttribute(responseKey);
            if (challengeResponse != null) {
                try {
                    isResponseCorrect = service.validateResponseForID(captchaID, challengeResponse);
                } catch (CaptchaServiceException e) {
                    request.setAttribute(CaptchaServicePlugin.getInstance().getMessageKey(),
                        CaptchaModuleConfigHelper.getMessage(request));
                }
            } else {
                isResponseCorrect = Boolean.TRUE;
            }
        } else if (typecaptcha.equalsIgnoreCase("text")) {
            String sum = request.getParameter("sum_result");
            String sum_answer = request.getParameter("sum_answer");
            if ((sum != null) && (sum_answer != null)) {
                try {
                    if (Integer.parseInt(sum) == Integer.parseInt(sum_answer)) {
                        isResponseCorrect = Boolean.TRUE;
                    }
                } catch (NumberFormatException e) {
                    isResponseCorrect = Boolean.FALSE;
                }
            } else {
                isResponseCorrect = Boolean.TRUE;
            }
        } else {
            isResponseCorrect = Boolean.TRUE;
        }

        return isResponseCorrect;
    }
}
