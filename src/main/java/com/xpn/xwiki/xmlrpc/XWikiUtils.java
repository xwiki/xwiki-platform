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
package com.xpn.xwiki.xmlrpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * This is an helper class containing some utility method for handling and setting up the XWiki and
 * XMLRPC data objects needed to serve XMLRPC requests.
 *
 * @author fmancinelli
 */
public class XWikiUtils
{
    private static final SimpleDateFormat dateFormat =
        new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);

    public static Object mock(Class someClass)
    {
        ClassLoader loader = someClass.getClassLoader();

        InvocationHandler handler = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                return null;
            }
        };
        Class[] interfaces = new Class[]{someClass};

        return Proxy.newProxyInstance(loader, interfaces, handler);
    }

    public static XWikiXmlRpcContext getXWikiXmlRpcContext(String token, XWikiRequest request,
        XWikiResponse response, XWikiServletContext servletContext) throws XWikiException,
        XmlRpcException
    {

        XWikiContext context = getXWikiContext(request, response, servletContext);
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, context);
        com.xpn.xwiki.api.XWiki xwiki = new com.xpn.xwiki.api.XWiki(context.getWiki(), context);

        return new XWikiXmlRpcContext(context, context.getWiki(), xwiki, user);
    }

    public static XWikiContext getXWikiContext(XWikiRequest request, XWikiResponse response,
        XWikiServletContext servletContext) throws XWikiException
    {
        XWikiContext context = Utils.prepareContext("", request, response, servletContext);
        XWiki xwiki = XWiki.getXWiki(context);
        XWikiURLFactory urlf =
            xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
        context.setURLFactory(urlf);
        XWikiVelocityRenderer.prepareContext(context);

        return context;
    }

    public static Map getTokens(XWikiContext context)
    {
        Map tokens = (Map) context.getEngineContext().getAttribute("xmlrpc_tokens");
        if (tokens == null) {
            tokens = new HashMap();
            context.getEngineContext().setAttribute("xmlrpc_tokens", tokens);
        }

        return tokens;
    }

    public static XWikiXmlRpcUser getUserForToken(XWikiContext context, String token)
    {
        return (XWikiXmlRpcUser) getTokens(context).get(token);
    }

    public static XWikiXmlRpcUser checkToken(String token, XWikiContext context)
        throws XmlRpcException
    {
        XWikiXmlRpcUser user = null;
        String ip = context.getRequest().getRemoteAddr();

        if (token != null) {
            if (token.equals("")) {
                user = new XWikiXmlRpcUser("XWiki.Guest", ip);
            } else {
                user = (XWikiXmlRpcUser) getTokens(context).get(token);
            }
        }

        if ((user == null) || (!user.getRemoteIp().equals(ip))) {
            throw new XmlRpcException(String.format(
                "[Access Denied: authentication token '%s' for IP %s is invalid]", token, ip));
        }

        context.setUser(user.getName());

        return user;
    }

    public static Object xmlRpcConvert(Object object)
    {
        if (object == null) {
            return "__NULL__VALUE__";
        }

        if (object.getClass().isArray()) {
            Object[] objects = (Object[]) object;
            List result = new ArrayList();
            for (int i = 0; i < objects.length; i++) {
                result.add(xmlRpcConvert(objects[i]));
            }

            return result;
        } else if (object instanceof List) {
            List list = (List) object;
            List result = new ArrayList();
            for (int i = 0; i < list.size(); i++) {
                result.add(xmlRpcConvert(list.get(i)));
            }

            return result;
        } else if (object instanceof Map) {
            Map map = (Map) object;
            Map result = new HashMap();
            Iterator i = map.keySet().iterator();
            while (i.hasNext()) {
                Object key = i.next();
                Object value = map.get(key);
                result.put(key, xmlRpcConvert(value));
            }

            return result;
        } else if (object instanceof Date) {
            return object; // dateFormat.format((Date) object);
        }

        return object.toString();
    }
}
