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

public class XwikiCheckLinkBlogPage extends WebUnitSuite {
  static String DESC = "Go to Blog page and click on links" ;
  static String NAME = "XwikiCheckLinkBlogPage" ;
  String COMMENT = null;
  public XwikiCheckLinkBlogPage() {
    super(NAME, DESC) ;
		init("/Blog") ;		
	}
	public void  init(path) {
	//click on Comment link on Launch of XWiki
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickCommentLaunchOfXWiki", "Link to add comment form").
        setURL("/xwiki/bin/view/Main/LaunchOfXWiki?xpage=comments")
  	) ;
    //click on continue reading link or permalink on Launch of XWiki
    goToPage(path) ; 
	addWebUnit(
        new ClickURLLinkUnit("ClickPermalinkLaunchOfXWiki", "Link to continue reading on Launch of XWiki").
        setURL("/xwiki/bin/view/Main/LaunchOfXWiki").
		addValidator(new ExpectTextValidator("Launch of XWiki"))
  	) ;
    //click on Comment link on Another Article
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickCommentAnotherArticle", "Link to add comment form").
        setURL("/xwiki/bin/view/Blog/TestArticle?xpage=comments")
  	) ;
    //click on continue reading link or permalink on Another Article
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickPermalinkAnotherArticle", "Link to continue reading on Another Article").
        setURL("/xwiki/bin/view/Blog/TestArticle").
		addValidator(new ExpectTextValidator("Another Article"))
  	) ;
    //click on Comment link on RSS Feed
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickCommentRSSFeed", "Link to add comment form").
        setURL("/xwiki/bin/view/Main/FilsRss?xpage=comments")
  	) ;
  	goToPage(path);
  	COMMENT ="run well but wrong result screen";
	addWebUnit(
	    new TODO("PermalinkGoToCommentForm!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
    //click on permalink on RSS Feed 
    goToPage(path);
    addWebUnit(
        new ClickURLLinkUnit("ClickPermalinkRSSFeed", "Link to RSS Feed").
        setURL("/xwiki/bin/view/Main/FilsRss").
	    addValidator(new ExpectTextValidator("RSS Feed"))
  	) ;
    */
	//click on Xwiki admin link
	goToPage(path);
	addWebUnit(
        new ClickURLLinkUnit("ClickXwikiAdmin", "Link to user profile of admin").
        setURL("/xwiki/bin/view/XWiki/Admin").
		addValidator(new ExpectTextValidator("User Profile"))
  	) ;
  }
    public java.util.List getRequiredSuites() {
        String[] roles = ["admin"] ;
        return [new XwikiLoginSuite("admin", "admin", roles)] ;
    }
}
