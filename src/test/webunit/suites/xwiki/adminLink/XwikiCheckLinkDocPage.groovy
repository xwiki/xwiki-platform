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

public class XwikiCheckLinkDocPage extends WebUnitSuite {
  static String DESC = "Go to Doc page and click on links" ;
  static String NAME = "XwikiCheckLinkDocPage" ;
  String COMMENT = null;
  
  public XwikiCheckLinkDocPage() {
    super(NAME, DESC) ;
		init("/Doc") ;		
	}
	public void  init(path) {
	goToPage(path);
	COMMENT ="this link has the same text with a link and has the same URL with another so they confound WUT";
	addWebUnit(
	    new TODO("ConfoundedSameTextLinkAndURL!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
    //click to see admin function but go to admin profile
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickAdministrationFunction", "Link to see admin function").
        setURL("/xwiki/bin/view/XWiki").
		addValidator(new ExpectTextValidator("Administration"))
  	) ;
    */
	//click on search page link
	addWebUnit(
    	new ClickLinkWithText("ClickSearchPageLink", "Link to search pages'").
        setTextLink("search page").
		addValidator(new ExpectTextValidator("Search on this Wiki"))
    );
	}
 	public java.util.List getRequiredSuites() {
        String[] roles = ["admin"] ;
        return [new XwikiLoginSuite("admin", "admin", roles)] ;
    }
}
