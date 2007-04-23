/* Copyright 2006, XpertNet SARL, and individual contributors as indicated
* by the contributors.txt.
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
*
@author ludovic
*/

package com.xpn.xwiki.test;

import com.xpn.xwiki.web.XWikiRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletInputStream;
import javax.servlet.RequestDispatcher;
import javax.portlet.*;
import java.util.*;
import java.security.Principal;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.io.InputStream;

public class XWikiFakeRequest implements XWikiRequest {
    protected Map map;

    public XWikiFakeRequest(Map map) {
        this.map = map;
    }

    public boolean isWindowStateAllowed(WindowState windowState) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isPortletModeAllowed(PortletMode portletMode) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PortletMode getPortletMode() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public WindowState getWindowState() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PortletPreferences getPreferences() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PortletSession getPortletSession() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PortletSession getPortletSession(boolean b) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getProperty(String string) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Enumeration getProperties(String string) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Enumeration getPropertyNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PortalContext getPortalContext() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getAuthType() {
		// TODO Auto-generated method stub
		return null;
	}

	public Cookie[] getCookies() {
		// TODO Auto-generated method stub
		return null;
	}

	public long getDateHeader(String arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getHeader(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Enumeration getHeaders(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Enumeration getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getIntHeader(String arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPathInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPathTranslated() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getContextPath() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getQueryString() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isUserInRole(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRequestedSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRequestURI() {
		// TODO Auto-generated method stub
		return null;
	}

	public StringBuffer getRequestURL() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServletPath() {
		return "";
	}

	public HttpSession getSession(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public HttpSession getSession() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isRequestedSessionIdValid() {
		// TODO Auto-generated method stub
		return false;
	}

    public String getResponseContentType() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Enumeration getResponseContentTypes() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isRequestedSessionIdFromUrl() {
		// TODO Auto-generated method stub
		return false;
	}

	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Enumeration getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

    public InputStream getPortletInputStream() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setCharacterEncoding(String arg0)
			throws UnsupportedEncodingException {
		// TODO Auto-generated method stub

	}

	public int getContentLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	public ServletInputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

    public String getParameter(String arg0) {
        Object obj = map.get(arg0);
        if (obj instanceof String[])
            return ((String[])obj)[0];
        if (obj instanceof String)
            return (String)obj;
        else
            return obj.toString();
    }

	public Enumeration getParameterNames() {
	    Vector v = new Vector();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            v.add(it.next());
        }
       return v.elements();
	}

	public String[] getParameterValues(String arg0) {
        Object obj = map.get(arg0);
        if (obj instanceof String[])
            return (String[])obj;
        if (obj instanceof String) {
            String[] res = new String[1];
            res[0] = (String)obj;
            return res;
        }
        else {
            String[] res = new String[1];
            res[0] = obj.toString();
            return res;
        }
	}

	public Map getParameterMap() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getScheme() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServerName() {
		// TODO Auto-generated method stub
		return null;
	}

    public int getServerPort() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getServerort() {
		// TODO Auto-generated method stub
		return 0;
	}

	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRemoteAddr() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRemoteHost() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub

	}

	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	public Enumeration getLocales() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSecure() {
		// TODO Auto-generated method stub
		return false;
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRealPath(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getRemotePort() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getLocalAddr() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getLocalPort() {
		// TODO Auto-generated method stub
		return 0;
	}

    public String get(String name) {
        return getParameter(name);
    }

    public HttpServletRequest getHttpServletRequest() {
        return null;  
    }

    public Cookie getCookie(String cookieName) {
        return null;
    }
}

