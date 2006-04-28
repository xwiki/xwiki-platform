/***************************************************************************
 * Copyright 2001-2005 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.share;

import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;

/**
 * Nov 14, 2005
 * @author: Dung Hoang
 * @email:   dunghoang@exoplatform.com
 **/
public class XwikiLoginSuite extends WebUnitSuite {  
  public XwikiLoginSuite() {
    super("XwikiLoginSuite",  "Go to the home page and login, using web client name for user name and password") ;
    String[] roles = ["user","admin"] ;
    init("admin", "admin", roles) ;
  }
  public XwikiLoginSuite(String userName, String password, String[] roles) {
    super("UserLoginSuite",  "Go to the home page and login, using web client name for user name and password") ;
    init(userName, password, roles) ;
  }
  public void init(String userName, String password, String[] roles) {      
	addWebUnit(
        new NewSessionUnit("NewSession", "Create new session and go to the home page for the first time")      
    );
    addWebUnit(
        new ClickLinkWithText("ClickLogin", "Click on 'Log-in'").
        setTextLink("Log-in").
        addValidator(new ExpectFormIdValidator("loginForm"))
    ) ;
    addWebUnit(
        new SubmitFormUnit("LoginWithUsername", "Login with username").
        setFormId("loginForm").
        setField("j_username", userName).
        setField("j_password", password).
        addValidator(new ExpectTextValidator("Hello"))
    ) ;
/*
	for(role in roles) {
      addWebUnit(
        new AddRoleUnit("AddRole: ${role}", "Add user role to the web client").
        setRoleToAdd(role)
      );
    }
*/
  }
}
