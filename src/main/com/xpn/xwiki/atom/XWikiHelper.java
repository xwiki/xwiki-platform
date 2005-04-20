/**
 * 
 */
package com.xpn.xwiki.atom;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionMapping;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.atom.lifeblog.UserBlog;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;

/**
 * @author Luis
 *
 */
public class XWikiHelper {

  private XWikiContext xwikiContext;

  /**
   * @return Returns the xwikiContext.
   */
  public XWikiContext getXwikiContext() {
    return xwikiContext;
  }

  /**
   * 
   */
  public XWikiHelper() {
    super();
  }

  /**
   * This constructor has package visibility because it was initially
   * written for unit testing purposes.
   * 
   * @param xwikiContext
   */
  public XWikiHelper(XWikiContext xwikiContext) {
    this.xwikiContext = xwikiContext;
  }

  /**
   * The "Atom Authentication Token" is simply the password *as it is stored in the database".
   * This means that if passwords are encrypted, the token is the encrypted password.  Otherwise
   * the token corresponds to the clear text password.
   * 
   * @param mapping
   * @param request
   * @param response
   * @param wsseHeader
   * @return
   * @throws XWikiException
   */
  public String getAtomAuthenticationToken(String userName) throws XWikiException {
    XWiki xwiki = xwikiContext.getWiki();
    XWikiDocument doc = xwiki.getDocument(userName, xwikiContext);
    String passwd = null;
    if (doc.getObject("XWiki.XWikiUsers")!= null) {
       passwd = doc.getStringValue("XWiki.XWikiUsers", "password");
    }
    return passwd;
  }

  /**
   * Initializes the XWiki context from a Struts action.
   * 
   * @param context
   * @return
   * @throws XWikiException 
   */
  public void initXWikiContext(
      ActionMapping actionMapping, 
      HttpServletRequest request, 
      HttpServletResponse response,
      ServletContext servletContext) throws XWikiException {
    XWikiRequest xwikiRequest = new XWikiServletRequest(request);
    XWikiResponse xwikiResponse = new XWikiServletResponse(response);
    XWikiContext xwikiContext = Utils.prepareContext(
      actionMapping.getName(), 
      xwikiRequest, 
      xwikiResponse, 
      new XWikiServletContext(servletContext));
    this.xwikiContext = xwikiContext;
  }

  public URL getURL() {
    return xwikiContext.getURL();
  }

  public String getBlogTitle(String web, String doc) throws XWikiException {
    XWiki xwiki = xwikiContext.getWiki();
    XWikiDocument blogDoc = xwiki.getDocument(web + "." + doc, xwikiContext);
    BaseObject blogPrefs = blogDoc.getObject("XWiki.BlogPreferences");
    return blogPrefs.getStringValue("title");
  }

  public UserBlog [] getUserBlogs() throws XWikiException {
    XWiki xwiki = xwikiContext.getWiki();
    List list = xwiki.search("from ", xwikiContext);
    UserBlog [] blogs = new UserBlog[list.size()];
    Iterator it = list.iterator();
    while (it.hasNext()) {
      XWikiDocument doc = (XWikiDocument)it.next();
      // TODO: Fill blogs array
    }
    return blogs;
  }

  public HttpSession getSession() {
    return xwikiContext.getRequest().getSession();
  }

}
