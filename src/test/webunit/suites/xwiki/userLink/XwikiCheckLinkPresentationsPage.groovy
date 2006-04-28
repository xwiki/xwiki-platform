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

public class XwikiCheckLinkPresentationsPage extends WebUnitSuite {
  static String DESC = "Go to Presentations page and click on links" ;
  static String NAME = "XwikiCheckLinkPresentationsPage" ;
  String COMMENT = null;
  
  public XwikiCheckLinkPresentationsPage() {
    super(NAME, DESC) ;
		init("/Presentations") ;		
	}
	public void  init(path) {   

	//set name and create a presentation (2 same form id)
	goToPage(path);
	addWebUnit(
        new SubmitFormUnit("TypeNameAndAddPresentation", "Add this presentation").
        setFormId("newdoc").
        setAction("../../inline/Demo/Name of your presentation").
        setField("name","my test presentation").
        addValidator(new ExpectTextValidator("You are not allowed to view this document or perform this action."))
    ) ;
    addWebUnit(
        new ClickURLLinkUnit("BackToHome", "Back to home page").
        setURL("/xwiki/bin/view/Main/")
  	) ;
	//click on preview to see existing presentation
	goToPage(path);
	addWebUnit(
        new ClickLinkWithText("ClickPreview", "Click on preview").
        setTextLink("Preview").
        addValidator(new ExpectTextValidator("Presentation"))
    );

	//click on edit to modify existing presentation
	goToPage(path);
	addWebUnit(
        new ClickLinkWithText("ClickEdit", "Click on edit").
        setTextLink("Edit").
        addValidator(new ExpectTextValidator("You are not allowed to view this document or perform this action."))
    );
    goToPage(path);
  	COMMENT ="WUT cannot click on launch";
	addWebUnit(
	    new TODO("WUTCanNotClickLaunch!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
  	//click on launch

	addWebUnit(
        new ClickURLLinkUnit("ClickLaunch", "Click on launch").
        setURL("/xwiki/bin/view/Main/WhatIsAWiki?xpage=s5b")
  	) ;
  	*/
    addWebUnit(
        new ClickURLLinkUnit("BackToHome", "Back to home page").
        setURL("/xwiki/bin/view/Main/")
  	) ;
	}
 	public java.util.List getRequiredSuites() {
        String[] roles = ["user"] ;
        return [new XwikiCreateAccountSuite(), new XwikiLoginSuite("#{client.name}", "#{client.name}", roles)] ;
    }
}
