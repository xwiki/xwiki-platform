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

public class XwikiCheckLinkPhotoAlbumsPage extends WebUnitSuite {
  static String DESC = "Go to Photo Albums page and click on links" ;
  static String NAME = "XwikiCheckLinkPhotoAlbumsPage" ;
  String COMMENT = null;
  
  public XwikiCheckLinkPhotoAlbumsPage() {
    super(NAME, DESC) ;
		init("/Photo Albums") ;		
  }
  public void  init(path) {
    //click on Seychelles1999
	goToPage(path);
	addWebUnit(
    	new ClickLinkWithText("ClickSeychelles1999", "Link to Seychelles1999'").
        setTextLink("Seychelles1999")
    );
    //click on Vietnam 2001
	goToPage(path);
	addWebUnit(
    	new ClickLinkWithText("ClickVietnam2001", "Link to Vietnam 2001'").
        setTextLink("Vietnam 2001")
    );
    //click on Cat Photo Album
	goToPage(path);
	addWebUnit(
    	new ClickLinkWithText("ClickCatPhotoAlbum", "Link to Cat Photo Album'").
        setTextLink("Cat Photo Album")
    );
    goToPage(path);
    COMMENT ="2 same form id, 2 same text field";
	addWebUnit(
	    new TODO("SameFormIdNoEffectCreateAlbum!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
	//set name and create new album (2 same form id, 2 same text field, run but wrong result screen)

	addWebUnit(
        new SubmitFormUnit("TypeNameAndAddAlbum", "Add this album").
        setFormId("newdoc").
        setAction("../../Photos/").
        setField("title","my test album")
        //addValidator(new ExpectTextValidator("You are not allowed to view this document or perform this action."))
        //addValidator(new ExpectFormIdValidator("edit"))
    ) ;
    */
	}
 	public java.util.List getRequiredSuites() {
        String[] roles = ["admin"] ;
        return [new XwikiLoginSuite("admin", "admin", roles)] ;
    }
}
