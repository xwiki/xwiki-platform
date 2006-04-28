/***************************************************************************
 * Copyright 2001-2005 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.userLink;

import suites.xwiki.share.XwikiLoginSuite;
import suites.xwiki.share.XwikiLogoutSuite;
import suites.xwiki.share.XwikiCreateAccountSuite;
import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;

/**
 * Nov 14, 2005
 * @author: Dung Hoang
 * @email:   dunghoang@exoplatform.com
 **/


public class XwikiUserCheckLinkMainPage extends WebUnitSuite {
  static String DESC = "Go to main page and click on links" ;
  static String NAME = "XwikiUserCheckLinkMainPage" ;
  String COMMENT = null;
  
  public XwikiUserCheckLinkMainPage() {
    super(NAME, DESC) ;
    //click on Home tab
    addWebUnit(
        new ClickLinkWithText("GoToHome", "Link to Home page").
        setTextLink("Home").
        addValidator(new ExpectTextValidator("Welcome to your Wiki"))
    ) ;
	//click on Blog tab
    addWebUnit(
        new ClickLinkWithText("GoToBlog", "Link to Blog page").
        setTextLink("Blog").
        addValidator(new ExpectTextValidator("Latest Articles"))
    ) ;
	//click on Knowledge Base tab
	addWebUnit(
        new ClickLinkWithText("GoToKnowledgeBase", "Link to Knowledge Base page").
        setTextLink("Knowledge Base").
        addValidator(new ExpectTextValidator("Wiki Knowledge Base"))
    ) ;
	//click on What'sNew tab
	addWebUnit(
        new ClickLinkWithText("GoToWhat'sNew", "Link to What's New page").
        setTextLink("What's New").
        addValidator(new ExpectTextValidator("What's New"))
    ) ;
	//click on Search tab
	addWebUnit(
        new ClickLinkWithText("GoToSearch", "Link to Search page").
        setTextLink("Search").
        addValidator(new ExpectTextValidator("Search on this Wiki"))
    ) ;
	//click on Doc tab
	addWebUnit(
        new ClickLinkWithText("GoToDoc", "Link to Doc page").
        setTextLink("Doc").
        addValidator(new ExpectTextValidator("Documentation"))
    ) ;
	//click on Register tab
	addWebUnit(
        new ClickLinkWithText("GoToRegister", "Link to register page").
        setTextLink("Register").
        addValidator(new ExpectTextValidator("Information about you"))
    ) ;
	//click on Photo Albums link
    addWebUnit(
        new ClickLinkWithText("GoToPhotoAlbums", "Link to Photo Albums page").
        setTextLink("Photo Albums").
        addValidator(new ExpectTextValidator("Photo Albums"))
    ) ;
	//click on Event Calendar link
    addWebUnit(
        new ClickLinkWithText("GoToEventCalendar", "Link to Event Calendar page").
        setTextLink("Event Calendar").
        addValidator(new ExpectTextValidator("Event Calendar"))
    ) ;
	//click on Presentation link
    addWebUnit(
        new ClickLinkWithText("GoToPresentation", "Link to Presentation page").
        setTextLink("Presentations").
        addValidator(new ExpectTextValidator("Presentation"))
    ) ;
    //click on News link(note: if being in home page, there are more than one 'News' word on screen so cannot setTextLink)
    addWebUnit(
      new ClickURLLinkUnit("GoToNews", "Link to News page").
      setURL("/xwiki/bin/view/Blog/Category?category=News").
      addValidator(new ExpectTextValidator("Entries for category News"))
    ) ;
	//click on Personal link
    addWebUnit(
        new ClickLinkWithText("GoToPersonal", "Link to Personal page").
        setTextLink("Personal").
        addValidator(new ExpectTextValidator("Entries for category Personal"))
    ) ;
	//click on Other link
    addWebUnit(
        new ClickLinkWithText("GoToOther", "Link to Other page").
        setTextLink("Other").
        addValidator(new ExpectTextValidator("Entries for category Other"))
    ) ;
	//click on Add Comment link
    addWebUnit(
        new ClickLinkWithText("ClickAddComment", "Click on 'Add comment'").
        setTextLink("Add comment").
        addValidator(new ExpectTextValidator("New Comment"))
    ) ;
 	//click on History link (it connect to another window that has no base link so make it the latest)
    addWebUnit(
        new ClickLinkWithText("ClickHistory", "Click on 'History'").
        setTextLink("History").
        addValidator(new ExpectTextValidator("Document History"))
    ) ;
		//PDF link is not supported to test by WebUnit		
		//click on 'more actions' then on 'Code'
    addWebUnit(
        new ClickLinkWithText("ClickMoreActions", "Click on 'More Actions'").
        setTextLink("More Actions")
    ) ;     //if run this unit after all, the expected text is not right but it is also runnable
    addWebUnit(
        new ClickLinkWithText("ClickCode", "Click on 'Code'").
        setTextLink("Code")
    );
    //click on 'more actions' then on 'Xwiki syntax'
    addWebUnit(
        new ClickLinkWithText("ClickMoreActions", "Click on 'More Actions'").
        setTextLink("More Actions")
    ) ;
    addWebUnit(
        new ClickLinkWithText("ClickXwikiSyntax", "Click on 'Xwiki Syntax'").
        setTextLink("Xwiki Syntax").
        addValidator(new ExpectTextValidator("XWiki Syntax"))
    );
	//view user profile
	addWebUnit(
        new ClickLinkWithText("ViewUSerProfile", "View an user profile").
        setTextLink("Xwiki Admin").        
        addValidator(new ExpectTextValidator("User Profile"))
    );
    addWebSuite(new XwikiLogoutSuite());
  }
 	public java.util.List getRequiredSuites() {
        String[] roles = ["user"] ;
        return [new XwikiCreateAccountSuite(), new XwikiLoginSuite("#{client.name}", "#{client.name}", roles)] ;
    }
}
