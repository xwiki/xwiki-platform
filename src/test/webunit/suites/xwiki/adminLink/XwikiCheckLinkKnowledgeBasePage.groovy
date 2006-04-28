/***************************************************************************
 * Copyright 2001-2005 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.adminLink;

import suites.xwiki.share.XwikiLoginSuite;
import suites.xwiki.share.XwikiCreateAccountSuite;
import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;

/**
 * Nov 14, 2005
 * @author: Dung Hoang
 * @email:   dunghoang@exoplatform.com
 **/

public class XwikiCheckLinkKnowledgeBasePage extends WebUnitSuite {
  static String DESC = "Go to Knowledge Base page and click on links" ;
  static String NAME = "XwikiCheckLinkKnowledgeBasePage" ;
  
  public XwikiCheckLinkKnowledgeBasePage() {
    super(NAME, DESC) ;
		init("/Knowledge Base") ;		
	}
	public void  init(path) {
    goToPage(path) ;  
	//click on ExampleLink1
	addWebUnit(
        new ClickURLLinkUnit("ClickExampleLink1", "Link to ExampleLink1").
        setURL("/xwiki/bin/view/Main/ExampleLink1")
  	) ;
	//click on ExampleLink2
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickExampleLink2", "Link to ExampleLink2").
        setURL("/xwiki/bin/edit/Main/Example+Link+2?parent=Main.KnowledgeBase")		
  	) ;
  	addWebUnit(
        new ClickURLLinkUnit("GoHome", "Link to Home page").
        setURL("/xwiki/bin/view/Main/")		
  	) ;
	}
 	public java.util.List getRequiredSuites() {
        String[] roles = ["admin"] ;
        return [new XwikiLoginSuite("admin", "admin", roles)] ;
    }
}
