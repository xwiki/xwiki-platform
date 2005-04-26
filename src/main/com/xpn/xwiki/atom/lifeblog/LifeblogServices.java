/**
 * 
 */
package com.xpn.xwiki.atom.lifeblog;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.atom.WSSEHttpHeader;
import com.xpn.xwiki.atom.XWikiHelper;


/**
 * @author Luis
 *
 */
public class LifeblogServices {

  private LifeblogContext lifeblogContext;
  private static final long NONCE_TIMEOUT = 1200000L;

  /**
   * @param context 
   * @param xwikiHelper 
   * 
   */
  public LifeblogServices(LifeblogContext lifeblogContext) {
    super();
    this.lifeblogContext = lifeblogContext;
  }

  public boolean isAuthenticated() throws LifeblogServiceException, XWikiException, ParseException {
    return isAuthenticated(lifeblogContext.getWSSEHeader());
  }
  
  public boolean isAuthenticated(String header) throws LifeblogServiceException, XWikiException, ParseException {
    if (header != null) {
      // Interpret WSSE Header and Authenticate User
      WSSEHttpHeader wsseHeader = WSSEHttpHeader.parseHttpHeader(header);
      
      if (nonceIsNotTooOld(wsseHeader.parseCreated())
          && !nonceAlreadyUsedByUser(wsseHeader.getNonce())) {
        String userName = "XWiki." + wsseHeader.getUserName();
        lifeblogContext.setUserName(userName);
        
        String authenticationToken = lifeblogContext.getXWikiHelper().getAtomAuthenticationToken(userName);

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
    List userBlogs = lifeblogContext.getXWikiHelper().listUserBlogs(lifeblogContext.getUserName());
    HttpServletResponse response = lifeblogContext.getXWikiHelper().getResponse();
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
      writer.println(userBlog.getPostHref());
      writer.println(userBlog.getFeedHref());
      writer.println(userBlog.getAlternateHref());
    }
    writer.append("</feed>");
    writer.flush();
    return stringWriter.toString();
  }
  
  private boolean nonceAlreadyUsedByUser(String nonce) {
    boolean alreadyUsed = false;
    HttpSession session = lifeblogContext.getXWikiHelper().getSession();
    String lastNonce = (String) session.getAttribute("lastNonce");
    if (lastNonce != null) {
      alreadyUsed = lastNonce.equals(nonce);
    }
    if (!alreadyUsed) {
      session.setAttribute("lastNonce", nonce);
    }
    return alreadyUsed;
  }

  private boolean nonceIsNotTooOld(Calendar createdDate) throws ParseException {
    return Calendar.getInstance().getTimeInMillis() - createdDate.getTimeInMillis() <= NONCE_TIMEOUT;
  }

}
