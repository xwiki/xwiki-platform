/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author kaaloo
 */
/**
 * 
 */
package com.xpn.xwiki.atom.lifeblog;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.atom.WSSEHttpHeader;
import com.xpn.xwiki.atom.XWikiHelper;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;


/**
 * @author Luis Arias <luis.arias@xwiki.com>
 *
 */
public class LifeblogServices {

  private String userName;
  private static final long NONCE_TIMEOUT = 1200000L;
  private XWikiHelper xwikiHelper;

  public LifeblogServices(XWikiContext context) {
	xwikiHelper = new XWikiHelper(context);
  }

  public boolean isAuthenticated() throws XWikiException, IOException {
    return isAuthenticated(xwikiHelper.getWSSEHeader());
  }
	  
  public boolean isAuthenticated(String header) throws XWikiException, IOException {
    if (header != null) {
      // Interpret WSSE Header and Authenticate User
      WSSEHttpHeader wsseHeader = WSSEHttpHeader.parseHttpHeader(header);
      
      if (nonceIsNotTooOld(wsseHeader.parseCreated())
          && !nonceAlreadyUsedByUser(wsseHeader.getNonce())) {
        userName = "XWiki." + wsseHeader.getUserName();
        
        String authenticationToken = xwikiHelper.getAtomAuthenticationToken(userName);

        if (authenticationToken !=null ) {
          if (wsseHeader.isAuthenticated(authenticationToken)) {
            return true;
          }        
        }        
      }
    }
    return false;
  }

  public void listUserBlogs() throws IOException, XWikiException {
    List userBlogs = xwikiHelper.listUserBlogs(userName);
    HttpServletResponse response = xwikiHelper.getResponse();
    response.setContentType("application/x.atom+xml");
    PrintWriter writer = new PrintWriter(response.getOutputStream());
    writer.write(getAtomListUserBlogs(userBlogs));
  }

  public String getAtomListUserBlogs(List userBlogs) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    writer.println("<?xml version=\"1.0\"?>");
    writer.println("<feed xmlns=\"http://purl.org/atom/ns#\">");
    Iterator it = userBlogs.iterator();
    while (it.hasNext()) {
      UserBlog userBlog = (UserBlog)it.next();
      writer.print("<link type=\"application/atom+xml\" rel=\"service.post\" href=\"");
      writer.print(userBlog.getPostHref());
      writer.print("\" title=\"");
      writer.print(userBlog.getTitle());
      writer.println("\"/>");
      writer.print("<link type=\"application/atom+xml\" rel=\"service.feed\" href=\"");
      writer.print(userBlog.getFeedHref());
      writer.print("\" title=\"");
      writer.print(userBlog.getTitle());
      writer.println("\"/>");
      writer.print("<link type=\"application/atom+xml\" rel=\"service.alternate\" href=\"");
      writer.print(userBlog.getAlternateHref());
      writer.print("\" title=\"");
      writer.print(userBlog.getTitle());
      writer.println("\"/>");
    }
    writer.print("</feed>");
    writer.flush();
    return stringWriter.toString();
  }
  
  private boolean nonceAlreadyUsedByUser(String nonce) {
    boolean alreadyUsed = false;
    HttpSession session = xwikiHelper.getSession();
    String lastNonce = (String) session.getAttribute("lastNonce");
    if (lastNonce != null) {
      alreadyUsed = lastNonce.equals(nonce);
    }
    if (!alreadyUsed) {
      session.setAttribute("lastNonce", nonce);
    }
    return alreadyUsed;
  }

  private boolean nonceIsNotTooOld(Calendar createdDate) {
    return Calendar.getInstance().getTimeInMillis() - createdDate.getTimeInMillis() <= NONCE_TIMEOUT;
  }
}
