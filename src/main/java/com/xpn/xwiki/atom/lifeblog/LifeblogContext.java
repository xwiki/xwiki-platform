/**
 * 
 */
package com.xpn.xwiki.atom.lifeblog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.atom.XWikiHelper;

/**
 * @author Luis
 *
 */
public class LifeblogContext {
  
  private String userName;
  
  private XWikiHelper xwikiHelper;

  private HttpServletRequest request;

  private HttpServletResponse response;

  /**
   * @param response 
   * 
   */
  public LifeblogContext(XWikiHelper xwikiHelper) {
    super();
    this.xwikiHelper = xwikiHelper;
    this.request = xwikiHelper.getRequest();
    this.response = xwikiHelper.getResponse();
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

  public XWikiHelper getXWikiHelper() {
    return xwikiHelper;
  }

  public String getAtomAuthenticationToken() throws XWikiException {
    return xwikiHelper.getAtomAuthenticationToken(userName);
  }

  public String getWSSEHeader() {
    // TODO Auto-generated method stub
    return request.getHeader("X-WSSE");
  }

}
