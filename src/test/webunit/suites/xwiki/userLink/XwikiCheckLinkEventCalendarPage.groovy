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

public class XwikiCheckLinkEventCalendarPage extends WebUnitSuite {
  static String DESC = "Go to Event Calendar page and click on links" ;
  static String NAME = "XwikiCheckLinkEventCalendarPage" ;
  String COMMENT = null;
  
  public XwikiCheckLinkEventCalendarPage() {
    super(NAME, DESC) ;
		init("/Event Calendar") ;		
	}
	public void  init(path) {
	goToPage(path);
	COMMENT ="Previous Month link and Next Month link wrong after some times";
	addWebUnit(
	    new TODO("WrongShowMonth!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
    //click on Previous Month
	goToPage(path);
	addWebUnit(
    	new ClickLinkWithText("ClickPreviousMonth", "Link to Previous Month'").
        setTextLink("Previous Month")
    );
    //click on Next Month
	//goToPage(path);
	addWebUnit(
    	new ClickLinkWithText("ClickNextMonth", "Link to Next Month'").
        setTextLink("Next Month")
    );
    */
    //click on edit object

	addWebUnit(
    	new ClickLinkWithText("ClickEditOject", "Link to edit object'").
        setTextLink("edit object").
		addValidator(new ExpectTextValidator("Welcome to the objects editor. Choose an object to edit or add an object to the document."))
    );
    goToPage(path);
    COMMENT ="NoFormIdToAddEvent";
	addWebUnit(
	    new TODO("NoFormIdToAddEvent!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
  }
 	public java.util.List getRequiredSuites() {
        String[] roles = ["user"] ;
        return [new XwikiCreateAccountSuite(), new XwikiLoginSuite("#{client.name}", "#{client.name}", roles)] ;
    }
}
