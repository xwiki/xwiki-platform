/***************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.runner;

import suites.xwiki.userLink.*;
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
public class XwikiUserCheckLinkSuites  extends TestSuites {
  public XwikiUserCheckLinkSuites() {
    addSuite(new XwikiCreateAccountSuite());
  	String[] roles=["user"];
    addSuite(new XwikiLoginSuite("#{client.name}", "#{client.name}", roles)) ;
    addSuite(new XwikiUserCheckLinkMainPage()) ;
    addSuite(new XwikiCheckLinkHomePage()) ;
    addSuite(new XwikiCheckLinkBlogPage()) ;
    addSuite(new XwikiCheckLinkKnowledgeBasePage()) ;
    addSuite(new XwikiCheckLinkWhatNewPage()) ;
    addSuite(new XwikiCheckLinkDocPage()) ;
    addSuite(new XwikiCheckLinkPhotoAlbumsPage()) ;
    addSuite(new XwikiCheckLinkEventCalendarPage()) ;
    addSuite(new XwikiCheckLinkPresentationsPage()) ;
    addSuite(new XwikiNotAllowedEditSuite());
    addSuite(new XwikiLogoutSuite());            
  }
}
