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

public class XwikiCheckLinkHomePage extends WebUnitSuite {
  static String DESC = "Go to main page and click on links" ;
  static String NAME = "XwikiCheckLinkHomePage" ;
  String COMMENT = null;
    public XwikiCheckLinkHomePage() {
        super(NAME, DESC) ;
		init("/Home") ;		
	}
	public void  init(path) {
    goToPage(path) ;  
    COMMENT ="Not support connect to other pages";
	addWebUnit(
	    new TODO("WUTNotSupportConnectOtherPages!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /* 
	addWebUnit(
        new ClickURLLinkUnit("ClickXWikiBlog", "Link to XWiki Blog page").
        setURL("http://www.xwiki.com/xwiki/bin/view/Blog/WebHome").
		addValidator(new ExpectTextValidator("Welcome to the XWiki Blog"))
  	) ;
	addWebUnit(
        new ClickLinkWithText("ClickXWikiBlog", "Link to XWiki Blog page").
        setTextLink("XWiki Blog").
        addValidator(new ExpectTextValidator("Welcome to the XWiki Blog"))
    ) ;
    */
    //click on continue reading link or permalink on Launch of XWiki
	addWebUnit(
        new ClickURLLinkUnit("ClickPermalinkLaunchOfXWiki", "Link to continue reading on Launch of XWiki").
        setURL("/xwiki/bin/view/Main/LaunchOfXWiki").
		addValidator(new ExpectTextValidator("Launch of XWiki"))
  	) ;
    //click on continue reading link or permalink on Another Article
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickPermalinkAnotherArticle", "Link to continue reading on Another Article").
        setURL("/xwiki/bin/view/Blog/TestArticle").
		addValidator(new ExpectTextValidator("Another Article"))
  	) ;
	//click on Xwiki admin link
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickXwikiAdmin", "Link to user profile of admin").
        setURL("/xwiki/bin/view/XWiki/Admin").
		addValidator(new ExpectTextValidator("User Profile"))
  	) ;
	//click on Comment link on Launch of XWiki
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickCommentLaunchOfXWiki", "Link to add comment form").
        setURL("/xwiki/bin/view/Main/LaunchOfXWiki?xpage=comments")
  	) ;
	//click on Comment link on Another Article
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickCommentAnotherArticle", "Link to add comment form").
        setURL("/xwiki/bin/view/Blog/TestArticle?xpage=comments")
  	) ;
	//click on Comment link on RSS Feed
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickCommentRSSFeed", "Link to add comment form").
        setURL("/xwiki/bin/view/Main/FilsRss?xpage=comments")
  	) ;
  	COMMENT ="run well but wrong result screen";
	addWebUnit(
	    new TODO("PermalinkGoToCommentForm!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    goToPage(path);
    /*
	//click on permalink on RSS Feed
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickPermalinkRSSFeed", "Link to RSS Feed").
        setURL("/xwiki/bin/view/Main/FilsRss")
  	) ;
  	*/
	}
 	public java.util.List getRequiredSuites() {
        String[] roles = ["user"] ;
        return [new XwikiCreateAccountSuite(), new XwikiLoginSuite("#{client.name}", "#{client.name}", roles)] ;
    }
}
