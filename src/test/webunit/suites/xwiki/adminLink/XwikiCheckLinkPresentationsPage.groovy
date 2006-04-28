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

public class XwikiCheckLinkPresentationsPage extends WebUnitSuite {
  static String DESC = "Go to Presentations page and click on links" ;
  static String NAME = "XwikiCheckLinkPresentationsPage" ;
  String COMMENT = null;
  
  public XwikiCheckLinkPresentationsPage() {
    super(NAME, DESC) ;
		init("/Presentations") ;		
	}
	public void  init(path) {   
    goToPage(path);
    COMMENT ="AddPresentationStrangeResult";
	addWebUnit(
	    new TODO("AddPresentationStrangeResult!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
	//set name and create a presentation (2 same form id)
	goToPage(path);
	addWebUnit(
        new SubmitFormUnit("TypeNameAndAddPresentation", "Add this presentation").
        setFormId("newdoc").
        setAction("../../inline/Demo/Name of your presentation").
        setField("name","my test presentation").
        addValidator(new ExpectFormIdValidator("edit"))
    ) ;
    addWebUnit(
        new ClickURLLinkUnit("BackToHome", "Back to home page").
        setURL("/xwiki/bin/view/Main/")
  	) ;
  	*/
	//click on preview to see existing presentation

	addWebUnit(
        new ClickLinkWithText("ClickPreview", "Click on preview").
        setTextLink("Preview").
        addValidator(new ExpectTextValidator("Presentation"))
    );
	//click on edit to modify existing presentation
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickEdit", "Click on edit").
        setURL("/xwiki/bin/inline/Main/WhatIsAWiki").
		addValidator(new ExpectFormIdValidator("edit"))
  	) ;
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
	}
 	public java.util.List getRequiredSuites() {
        String[] roles = ["admin"] ;
        return [new XwikiLoginSuite("admin", "admin", roles)] ;
    }
}
