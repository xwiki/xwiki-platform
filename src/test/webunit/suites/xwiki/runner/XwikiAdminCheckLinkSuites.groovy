/***************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.runner;

import suites.xwiki.adminLink.*;
import suites.xwiki.share.*;
import suites.xwiki.account.*;
import suites.xwiki.edit.*;

import org.exoplatform.webunit.*;

/**
 * Created by The eXo Platform SARL
 * Author : Hoa  Pham
 *          hoapham@exoplatform.com,phamvuxuanhoa@yahoo.com
 * Nov 8, 2005
**/
public class XwikiAdminCheckLinkSuites  extends TestSuites {
  public XwikiAdminCheckLinkSuites() {

  	String[] roles=["admin"];
    addSuite(new XwikiLoginSuite("admin", "admin", roles)) ;
    addSuite(new XwikiAdminCheckLinkMainPage()) ;
    addSuite(new XwikiCheckLinkHomePage()) ;
    addSuite(new XwikiCheckLinkBlogPage()) ;
    addSuite(new XwikiCheckLinkKnowledgeBasePage()) ;
    addSuite(new XwikiCheckLinkWhatNewPage()) ;
    //check link in What New must login with an user
    addSuite(new XwikiLoginSuite("admin", "admin", roles)) ;
    addSuite(new XwikiCheckLinkAdminPage()) ;
    addSuite(new XwikiCheckLinkDocPage()) ;
    addSuite(new XwikiCheckLinkPhotoAlbumsPage()) ;
    addSuite(new XwikiCheckLinkEventCalendarPage()) ;
    addSuite(new XwikiCheckLinkPresentationsPage()) ;
    //addSuite(new XwikiEditPageSuite());
    addSuite(new XwikiLogoutSuite());
  }
}
