/**
 * 
 */
package com.xpn.xwiki.atom.lifeblog;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.xpn.xwiki.atom.XWikiHelper;
import com.xpn.xwiki.web.XWikiAction;

/**
 * @author Luis
 *
 */
public class LifeblogAction extends XWikiAction {

  private static final Log log = LogFactory.getLog(LifeblogAction.class);

  /**
   * 
   */
  public LifeblogAction() {
    super();
    // TODO Auto-generated constructor stub
  }
  
  /**
   * Handle server requests.
   *
   * @param mapping The ActionMapping used to select this instance
   * @param form The optional ActionForm bean for this request (if any)
   * @param req The HTTP request we are processing
   * @param resp The HTTP response we are creating
   *
   * @exception IOException if an input/output error occurs
   * @exception ServletException if a servlet exception occurs
   */
  public ActionForward execute(ActionMapping mapping,
                               ActionForm form,
                               HttpServletRequest request,
                               HttpServletResponse response)
          throws Exception, ServletException {
    
    XWikiHelper xwikiHelper = new XWikiHelper();
    xwikiHelper.initXWikiContext(mapping.getName(), request, response, servlet.getServletContext());
    
    LifeblogContext lifeblogContext = new LifeblogContext(xwikiHelper);
    
    LifeblogServices services = new LifeblogServices(lifeblogContext);
    
    // Check Authentication
    if (!services.isAuthenticated()) {
      respondNotAuthorized(response);      
    } else if (request.getPathInfo().equals("/lifeblog")) {
      services.listUserBlogs();
    }

    return null;
  }


  /**
   * @param response
   * @throws IOException
   */
  private void respondNotAuthorized(HttpServletResponse response) throws IOException {
    response.setHeader("WWW-Authenticate", "WSSE realm=\"foo\", profile=\"UsernameToken\"");
    response.sendError(401, "Unauthorized");
  }
}
