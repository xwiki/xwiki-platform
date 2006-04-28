/***************************************************************************
 * Copyright 2001-2005 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.edit;

import suites.xwiki.share.XwikiLoginSuite;
import suites.xwiki.share.XwikiLogoutSuite;
import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;

/**
 * Nov 14, 2005
 * @author: Dung Hoang
 * @email:   dunghoang@exoplatform.com
 **/


public class XwikiEditPageSuite extends WebUnitSuite {
  static String DESC = "Go to main page and check edit page" ;
  static String NAME = "XwikiEditPageSuite" ;
  String COMMENT = null;

  public XwikiEditPageSuite() {
    super(NAME, DESC) ;
    init("/Home") ;
	}
	public void  init(path) {
    goToPage(path) ;
    COMMENT ="Add this news give really strange result";
	addWebUnit(
	    new TODO("AddThisNewsBROKEN!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
	addWebUnit(
        new SubmitFormUnit("TypeTitleAndAddThisNews", "Add this news").
        setFormId("newdoc").
        setField("title","my test page").
        setAction("../../inline/Main/")
        //addValidator(new ExpectTextValidator("Enter your title here"))
    ) ;

    COMMENT ="Only search after adding id for form in screen, wrong if rollback database";
	addWebUnit(
	    new TODO("NoSearchFormId!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    */
	//search
    addWebUnit(
        new SubmitFormUnit("SearchText", "Search a text").
        setFormId("searchForm").
        setField("text","test").
        //xwiki/bin/view/Main/WebSearch
        addValidator(new ExpectTextValidator("Search on this Wiki"))
    ) ;

	//click on Edit this Page link then click on Cancel
	addWebUnit(
        new ClickLinkWithText("ClickEditThisPage", "Click on 'Edit this Page'").
        setTextLink("Edit this Page").
        addValidator(new ExpectFormIdValidator("edit"))
    );
	addWebUnit(
        new ClickLinkWithText("ClickCancel", "Click on 'Cancel'").
        setTextLink("Cancel").
        addValidator(new ExpectTextValidator("Welcome to your Wiki"))
	);

		//click on Edit this Page link, set value to title field then click on Preview and Save
	addWebUnit(
        new ClickLinkWithText("ClickEditThisPage", "Click on 'Edit this Page'").
        setTextLink("Edit this Page").
        addValidator(new ExpectFormIdValidator("edit"))
    );
		//preview
	addWebUnit(
        new SubmitFormUnit("EditAndPreview", "Edit Form").
        setFormId("edit").
        setAction("/xwiki/bin/preview/Main/WebHome").
        setField("title","my name is Huyen")
	);
	COMMENT ="WUT parse string and leave out space so make content page wrong";
	addWebUnit(
	    new TODO("WrongSaveInWUT(not comment out)!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
	//wrong save
	//save
	addWebUnit(
         new SubmitFormUnit("SaveForm", "SaveForm").
         setFormId("edit").
         setAction("/xwiki/bin/save/Main/WebHome")
	);
	COMMENT ="Edit categories shows code in screen";
	addWebUnit(
	    new TODO("EditCategoriesWrongInMyPC!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
    //click on Edit Categories then Cancel (when click on edit categories, it shows code in screen !!!)
    addWebUnit(
        new ClickLinkWithText("ClickEditCategories", "Click on 'Edit Categories'").
        setTextLink("Edit Categories").
        addValidator(new ExpectFormIdValidator("edit")).
        addValidator(new ExpectTextValidator("Categories"))
    ) ;
	addWebUnit(
        new ClickLinkWithText("ClickCancel", "Click on 'Cancel'").
        setTextLink("Cancel").
        addValidator(new ExpectNoTextValidator("Cancel"))
    ) ;
	//click on Edit Categories (it cannot find field, name of the field change )
    addWebUnit(
        new ClickLinkWithText("ClickEditCategories", "Click on 'Edit Categories'").
        setTextLink("Edit Categories").
        addValidator(new ExpectFormIdValidator("edit")).
        addValidator(new ExpectTextValidator("Categories"))
    ) ;
	addWebUnit(
        new SubmitFormUnit("EditAndClickPreview", "Edit Form").
        setFormId("edit").
		setField("Blog.Categories_0_name","test")
		//setField("Blog.Categories_4_description","test")//Blog.Categories_6_description
	);
	addWebUnit(
        new SubmitFormUnit("SaveForm", "Save Form").
        setFormId("edit").
		addValidator(new ExpectTextValidator("test"))
	);
	*/
  }
  public java.util.List getRequiredSuites() {
	String[] roles = ["admin"] ;    
    return [new XwikiLoginSuite("admin","admin",roles)] ;
  }
}
