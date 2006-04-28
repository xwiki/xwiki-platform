/**************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved. *
 * Please look at license.txt in info directory for more license detail.  *
 **************************************************************************/
package suites.xwiki.share ;

import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;
/**  
 * May 21, 2004
 * @author: Tuan Nguyen
 * @email:   tuan08@users.sourceforge.net
 * @version: $Id: CommonUnit.java,v 1.2 2004/10/21 15:21:49 tuan08 Exp $
 **/
public class XwikiCommonUnit extends WebUnitSuite {
  
  final static public WebUnit GO_HOME_UNIT =
    new ClickURLLinkUnit("GoHome", "Link to Home page").
    setURL("/xwiki/bin/view/Main/")
    /*
    new ClickLinkWithText("GoToHome", "Tell the web client click on the Home tab").
    setTextLink("Home") ;
     */

  final static public WebUnit GO_BLOG_UNIT = 
    new ClickLinkWithText("GoToBlog", "Tell the web client click on the Blog tab").
    setTextLink("Blog") ;

  final static public WebUnit GO_KNOWLEDGE_BASE_UNIT = 
    new ClickLinkWithText("GoToKnowledgeBase", "Tell the web client click on the Knowledge Base tab").
    setTextLink("Knowledge Base") ;

  final static public WebUnit GO_WHAT'SNEW_UNIT = 
    new ClickLinkWithText("GoToWhat'sNew", "Tell the web client click on the What's New tab").
    setTextLink("What's New") ;

  final static public WebUnit GO_SEARCH_UNIT = 
    new ClickLinkWithText("GoToSearch", "Tell the web client click on the Search tab").
    setTextLink("Search") ;

  final static public WebUnit GO_ADMIN_UNIT = 
    new ClickLinkWithText("GoToAdmin", "Tell the web client click on the Admin tab").
    setTextLink("Admin") ;  

  final static public WebUnit GO_DOC_UNIT = 
    new ClickLinkWithText("GoToDoc", "Tell the web client click on the Doc tab").
    setTextLink("Doc") ;  

  final static public WebUnit GO_REGISTER_UNIT = 
    new ClickLinkWithText("GoToRegister", "Tell the web client click on the Register tab").
    setTextLink("Register") ;

  final static public WebUnit GO_PHOTOALBUMS_UNIT = 
    new ClickLinkWithText("GoToPhotoAlbums", "Tell the web client click on the Photo Albums link").
    setTextLink("Photo Albums") ;

  final static public WebUnit GO_EVENT_CALENDAR_UNIT = 
    new ClickLinkWithText("GoToEventCalendar", "Tell the web client click on the Event Calendar link").
    setTextLink("Event Calendar") ;

  final static public WebUnit GO_PRESENTATION_UNIT = 
    new ClickLinkWithText("GoToPresentation", "Tell the web client click on the Presentation link").
    setTextLink("Presentation") ;

  final static public WebUnit CLICK_NEWS = 
  	new ClickURLLinkUnit("ClickNews", "Tell the web client click on News link").
    setURL("/xwiki/bin/view/Blog/Category?category=News").    
		addValidator(new ExpectTextValidator("Entries for category News"));

  final static public WebUnit GO_PERSONAL_UNIT = 
    new ClickLinkWithText("GoToPersonal", "Tell the web client click on the Personal link").
    setTextLink("Personal") ;

  final static public WebUnit GO_OTHER_UNIT = 
    new ClickLinkWithText("GoToOther", "Tell the web client click on the Other link").
    setTextLink("Other") ;

  final static public WebUnit GO_PERSONAL_UNIT = 
    new ClickLinkWithText("GoToPersonal", "Tell the web client click on the Personal link").
    setTextLink("Personal") ;

  static public void addClickUnits(WebUnitSuite suite , String path) {
    String[] links = path.split("/") ;
    for(text  in  links) {
      suite.addWebUnit(new ClickLinkWithText("Click:" + text, "Go to " + text + "page").
                      setTextLink(text)) ;
    }
  }
	
}
