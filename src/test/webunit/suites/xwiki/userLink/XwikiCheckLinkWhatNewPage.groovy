/***************************************************************************
 * Copyright 2001-2005 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.userLink;

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

public class XwikiCheckLinkWhatNewPage extends WebUnitSuite {
  static String DESC = "Go to WhatNew page and click on links" ;
  static String NAME = "XwikiCheckLinkWhatNewPage" ;
  String COMMENT = null;
  
  public XwikiCheckLinkWhatNewPage() {
    super(NAME, DESC) ;
		init("/What's New") ;		
  }
	public void  init(path) {
	goToPage(path) ;
	COMMENT ="Wrong function";
	addWebUnit(
	    new TODO("ViewChangeIsWrong!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
    //click to view change
	addWebUnit(
        new ClickURLLinkUnit("ClickViewChange", "Link to view change").
        setURL("/xwiki/bin/view/Main/WhatsNew?diff=1")
  	) ;	
    */
    addWebSuite(new XwikiCreateAccountSuite()) ;
    String[] roles = ["user"] ;
    addWebSuite(new XwikiLoginSuite("#{client.name}", "#{client.name}", roles)) ;
    //check exist of new user link
    goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickCurrentUser", "Click to see current user profile").
        setURL("/xwiki/bin/view/XWiki/"+"#{client.name}").
	    addValidator(new ExpectTextValidator("User Profile"))
  	) ;
    }
 	public java.util.List getRequiredSuites() {
        String[] roles = ["user"] ;
        return [new XwikiCreateAccountSuite(), new XwikiLoginSuite("#{client.name}", "#{client.name}", roles)] ;
    }
}
