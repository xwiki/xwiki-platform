/**
 * 
 */
package com.xpn.xwiki.atom.lifeblog;

import java.text.ParseException;
import java.util.Calendar;

import javax.servlet.http.HttpSession;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.atom.WSSEHttpHeader;
import com.xpn.xwiki.atom.XWikiHelper;

/**
 * @author Luis
 *
 */
public class LifeblogContext {
  
  private String userName;
  
  private XWikiHelper xwikiHelper;

  private static final long NONCE_TIMEOUT = 1200000L;

  /**
   * 
   */
  public LifeblogContext() {
    super();
  }

  public UserBlog getUserBlog(String web, String doc) throws XWikiException {
    UserBlog userBlog = new UserBlog();
    userBlog.setPostHref(getPostUrl(web, doc));
    userBlog.setFeedHref(getFeedUrl(web, doc));
    userBlog.setAlternateHref(getAlternateUrl(web, doc));
    userBlog.setTitle(getBlogTitle(web, doc));
    
    return userBlog;
  }
  
  private String getPostUrl(String web, String doc) {
    return xwikiHelper.getURL() + "/bin/lifeblog/" + web + "/" + doc;
  }

  private String getFeedUrl(String web, String doc) {
    return xwikiHelper.getURL() + "/bin/lifeblog/" + web + "/" + doc;
  }

  private String getAlternateUrl(String web, String doc) {
    return xwikiHelper.getURL() + "/bin/lifeblog/" + web + "/" + doc;
  }
  
  private String getBlogTitle(String web, String doc) throws XWikiException {
    return xwikiHelper.getBlogTitle(web, doc);
  }

  /**
   * @return Returns the userName.
   */
  public String getUserName() {
    return userName;
  }

  /**
   * @param userName The userName to set.
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setXWikiHelper(XWikiHelper xwikiHelper) {
    this.xwikiHelper = xwikiHelper;
    
  }

  public XWikiHelper getXWikiHelper() {
    return xwikiHelper;
  }

  public String getAtomAuthenticationToken() throws XWikiException {
    return xwikiHelper.getAtomAuthenticationToken(userName);
  }

  public boolean isAuthenticated(String header) throws LifeblogServiceException, XWikiException, ParseException {
    if (header != null) {
      // Interpret WSSE Header and Authenticate User
      WSSEHttpHeader wsseHeader = WSSEHttpHeader.parseHttpHeader(header);
      
      if (nonceIsNotTooOld(wsseHeader.parseCreated())
          && !nonceAlreadyUsedByUser(wsseHeader.getNonce())) {
        setUserName("XWiki." + wsseHeader.getUserName());
        
            String authenticationToken = getAtomAuthenticationToken();

            if (authenticationToken !=null ) {
              if (wsseHeader.isAuthenticated(authenticationToken)) {
                return true;
              }        
            }        
      }
    }
    return false;
  }

  private boolean nonceIsNotTooOld(Calendar createdDate) throws ParseException {
    return Calendar.getInstance().getTimeInMillis() - createdDate.getTimeInMillis() <= NONCE_TIMEOUT;
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
  
}
