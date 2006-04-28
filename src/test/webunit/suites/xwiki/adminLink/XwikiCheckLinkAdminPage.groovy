/***************************************************************************
 * Copyright 2001-2005 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.adminLink;

import suites.xwiki.share.XwikiLoginSuite;
import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;

/**
 * Nov 14, 2005
 * @author: Dung Hoang
 * @email:   dunghoang@exoplatform.com
 **/

public class XwikiCheckLinkAdminPage extends WebUnitSuite {
  static String DESC = "Go to admin page and click on links" ;
  static String NAME = "XwikiCheckLinkAdminPage" ;
  String COMMENT = null;
  
  public XwikiCheckLinkAdminPage() {
    super(NAME, DESC) ;
		init("/Admin") ;		
	}
	public void  init(path) {

	//click on users link on User Administration
	goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickUsers", "Link to users on User Administration").
        setURL("/xwiki/bin/view/XWiki/XWikiUsers").
	    addValidator(new ExpectTextValidator("Users"))
  	) ;
	//click on groups link on User Administration
	goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickGroups", "Link to groups on User Administration").
        setURL("/xwiki/bin/view/XWiki/XWikiGroups").
		addValidator(new ExpectTextValidator("Current XWiki Groups"))
  	) ;

	//click on Categories link on Blog
	goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickCategories", "Link to Categories on Blog").
        setURL("/xwiki/bin/view/Blog/Categories").
		addValidator(new ExpectTextValidator("Categories"))
  	) ;		
    COMMENT ="Article Sheet show code on screen";
	addWebUnit(
	    new TODO("ArticleSheetCodeOnScreen(not comment out)!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
	//click on Article Sheet link on Blog
	goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickArticleSheet", "Link to Article Sheet on Blog").
        setURL("/xwiki/bin/view/XWiki/ArticleClassSheet")
  	) ;

    //click on Article Template link on Blog
	goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickArticleTemplate", "Link to Article Template on Blog").
        setURL("/xwiki/bin/view/XWiki/ArticleClassTemplate")
  	) ;
    COMMENT ="Photo Album Sheet show code on screen";
	addWebUnit(
	    new TODO("PhotoAlbumSheetCodeOnScreen(not comment out)!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
	//click on Photo Album Sheet link on Photos (show code on screen)
	goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickPhotoAlbumSheet", "Link to Photo Album Sheet on Photos").
        setURL("/xwiki/bin/view/XWiki/PhotoAlbumClassSheet")
  	) ;
    //click on Photo Album Template link on Photos
	goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickPhotoAlbumTemplate", "Link to Photo Album Template on Photos").
        setURL("/xwiki/bin/view/XWiki/PhotoAlbumClassTemplate")
  	) ;

	//click on Menu link on Presentation
	goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickMenu", "Link to Menu on Presentation").
        setURL("/xwiki/bin/view/XWiki/Menu")
  	) ;
    //click on Toolbar link on Presentation
	goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickToolbar", "Link to Toolbar on Presentation").
        setURL("/xwiki/bin/view/XWiki/Toolbar")
  	) ;
    //click on Skin link on Presentation
	goToPage(path) ;
	addWebUnit(
        new ClickURLLinkUnit("ClickSkin", "Link to Skin on Presentation").
        setURL("/xwiki/bin/view/XWiki/MySkin")
  	) ;

  }
 	public java.util.List getRequiredSuites() {
   	    String[] roles = ["admin"] ;
   	    return [new XwikiLoginSuite("admin","admin",roles)] ;	
 	}
}
