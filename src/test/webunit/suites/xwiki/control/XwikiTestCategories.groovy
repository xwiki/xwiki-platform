/***************************************************************************
 * Copyright 2001-2005 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.control;

import org.exoplatform.webunit.webui.*;
/**
 * Nov 14, 2005
 * @author: Dung Hoang
 * @email:   dunghoang@exoplatform.com
 **/
public class XwikiTestCategories extends org.exoplatform.webunit.webui.TestCategories {

  public XwikiTestCategories() {    
    List categories = getCategories() ;
    TestCategoryDescription category = new TestCategoryDescription("User Check Link Suites") ;    
	category.addSuite(new TestSuiteDescription("suites.xwiki.runner.XwikiUserCheckLinkSuites", "Xwiki User Check Link Suites")) ;
    categories.add(category) ;

    category = new TestCategoryDescription("Admin Check Link Suites") ;
	category.addSuite(new TestSuiteDescription("suites.xwiki.runner.XwikiAdminCheckLinkSuites", "Xwiki Admin Check Link Suites")) ;
    categories.add(category) ;

    category = new TestCategoryDescription("Account Suite") ;
    category.addSuite(new TestSuiteDescription("suites.xwiki.share.XwikiLoginSuite", "Xwiki Login Suite")) ;
    category.addSuite(new TestSuiteDescription("suites.xwiki.share.XwikiCreateAccountSuite", "Xwiki Create Account Suite")) ;
    category.addSuite(new TestSuiteDescription("suites.xwiki.account.XwikiCheckLoginSuite", "Xwiki Check Login Suite")) ;
    category.addSuite(new TestSuiteDescription("suites.xwiki.account.XwikiCheckCreateAccountSuite", "Xwiki Check Create Account Suite")) ;
    category.addSuite(new TestSuiteDescription("suites.xwiki.account.XwikiCheckAccountSuite", "Xwiki Check Account Suite (wrong logout)")) ;
    categories.add(category) ;

    category = new TestCategoryDescription("User Check Link Suite") ;
	category.addSuite(new TestSuiteDescription("suites.xwiki.userLink.XwikiUserCheckLinkMainPage", "Xwiki USer Check All Link Main Page Suite (Wrong Log-out)")) ;
	category.addSuite(new TestSuiteDescription("suites.xwiki.userLink.XwikiCheckLinkHomePage", "Xwiki Check All Link In Home Page Suite"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.userLink.XwikiCheckLinkBlogPage", "Xwiki Check All Link In Blog Page Suite (result screen ?)"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.userLink.XwikiCheckLinkKnowledgeBasePage", "Xwiki Check All Link In Knowledge Base Page Suite"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.userLink.XwikiCheckLinkWhatNewPage", "Xwiki Check All Link In What New Page Suite (wrong function)"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.userLink.XwikiCheckLinkDocPage", "Xwiki Check All Link In Doc Page Suite (same text and same link)"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.userLink.XwikiCheckLinkPhotoAlbumsPage", "Xwiki Check All Link In Photo Albums Page Suite (same form id, same field name)"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.userLink.XwikiCheckLinkEventCalendarPage", "Xwiki Check All Link In Event Calendar Page Suite (no id to check add event)"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.userLink.XwikiCheckLinkPresentationsPage", "Xwiki Check All Link In Presentations Page Suite"));
	categories.add(category) ;

	category = new TestCategoryDescription("Admin Check Link Suite") ;
	category.addSuite(new TestSuiteDescription("suites.xwiki.adminLink.XwikiAdminCheckLinkMainPage", "Xwiki Admin Check All Link Main Page Suite (Wrong Log-out)")) ;
	category.addSuite(new TestSuiteDescription("suites.xwiki.adminLink.XwikiCheckLinkAdminPage", "Xwiki Check All Link In Admin Page Suite"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.adminLink.XwikiCheckLinkHomePage", "Xwiki Check All Link In Home Page Suite"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.adminLink.XwikiCheckLinkBlogPage", "Xwiki Check All Link In Blog Page Suite (result screen ?)"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.adminLink.XwikiCheckLinkKnowledgeBasePage", "Xwiki Check All Link In Knowledge Base Page Suite"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.adminLink.XwikiCheckLinkWhatNewPage", "Xwiki Check All Link In What New Page Suite (wrong function)"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.adminLink.XwikiCheckLinkDocPage", "Xwiki Check All Link In Doc Page Suite (same text and same link)"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.adminLink.XwikiCheckLinkPhotoAlbumsPage", "Xwiki Check All Link In Photo Albums Page Suite (same form id, same field name)"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.adminLink.XwikiCheckLinkEventCalendarPage", "Xwiki Check All Link In Event Calendar Page Suite (no id to check add event)"));
	category.addSuite(new TestSuiteDescription("suites.xwiki.adminLink.XwikiCheckLinkPresentationsPage", "Xwiki Check All Link In Presentations Page Suite"));
	categories.add(category) ;

    category = new TestCategoryDescription("Edit Page Suite") ;
	category.addSuite(new TestSuiteDescription("suites.xwiki.edit.XwikiEditPageSuite", "Xwiki Edit Page Suite(Wrong Edit Categories)")) ;
	category.addSuite(new TestSuiteDescription("suites.xwiki.edit.XwikiNotAllowedEditSuite", "Xwiki Not Allow User Edit Suite (wrong add news)")) ;
    categories.add(category) ;
  } 
}
