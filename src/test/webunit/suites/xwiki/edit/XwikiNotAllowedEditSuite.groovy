/***************************************************************************
 * Copyright 2001-2005 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.edit;

import suites.xwiki.share.XwikiLoginSuite;
import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;

/**
 * Nov 14, 2005
 * @author: Dung Hoang
 * @email:   dunghoang@exoplatform.com
 **/

public class XwikiNotAllowedEditSuite extends WebUnitSuite {
  //static java.util.Date date = new java.util.Date() ;
  //static String uid = date.toString() +  date.hashCode() ;

  static String DESC = "Go to main page and check edit page" ;
  static String NAME = "XwikiNotAllowedEditSuite" ;
  String COMMENT = null;

  public XwikiNotAllowedEditSuite() {
    super(NAME, DESC) ;

	//create new name page then click on 'Add this news'
	addWebUnit(
        new SubmitFormUnit("TypeTitleAndAddThisNews", "Add this news").
        setFormId("newdoc").
        setField("title","my test page").
        setAction("../../inline/Main/").
        addValidator(new ExpectTextValidator("You are not allowed to view this document or perform this action."))        
    ) ;
    //go back home
    addWebUnit(
        new ClickURLLinkUnit("GoHome", "Link to Home").
        setURL("/xwiki/bin/view/Main/")
  	) ;
	//click on icon to edit menu
	addWebUnit(
      	new ClickURLLinkUnit("ClickEditMenu", "Click on icon to edit menu").
      	setURL("/xwiki/bin/edit/XWiki/Menu").
		addValidator(new ExpectTextValidator("You are not allowed to view this document or perform this action."))
  	) ;
    //go back home
    addWebUnit(
        new ClickURLLinkUnit("GoHome", "Link to Home").
        setURL("/xwiki/bin/view/Main/")
  	) ;
  	COMMENT ="Edit categories shows code on screen";
	addWebUnit(
	    new TODO("EditCategoriesCodeInScreen(only my pc)!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
  	//click on 'Edit Categories' link
	addWebUnit(
      	new ClickURLLinkUnit("ClickEditCategories", "Click on icon to edit Categories").
      	setURL("/xwiki/bin/inline/Blog/Categories").
		addValidator(new ExpectTextValidator("You are not allowed to view this document or perform this action."))
  	) ;
	//Click on 'Add a category' link
	addWebUnit(
        new ClickLinkWithText("ClickAddCategory", "Click on 'Add a category' link").
        setTextLink("Add a category")
    );
    //go back home
    addWebUnit(
        new ClickURLLinkUnit("GoHome", "Link to Home").
        setURL("/xwiki/bin/view/Main/")
  	) ;
  	*/
    //click on icon to edit toolbar
	addWebUnit(
      	new ClickURLLinkUnit("ClickEditToolbar", "Click on icon to edit toolbar").
      	setURL("/xwiki/bin/edit/XWiki/Toolbar").
		addValidator(new ExpectTextValidator("You are not allowed to view this document or perform this action."))
  	) ;
  	/*
    //go back home
    addWebUnit(
        new ClickURLLinkUnit("GoHome", "Link to Home").
        setURL("/xwiki/bin/view/Main/")
  	) ;
  	*/
  }
 	public java.util.List getRequiredSuites() {
		String[] roles = ["user"] ;
    return [new XwikiLoginSuite("test","test",roles)] ;
    }
}
