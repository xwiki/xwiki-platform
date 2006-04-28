package suites.xwiki.account ;

import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;
/**
 * May 21, 2004
 * @author: Tuan Nguyen
 * @email:   tuan08@users.sourceforge.net
 **/
public class XwikiCheckLoginSuite extends WebUnitSuite {
  public XwikiCheckLoginSuite() {
    super("XwikiCheckLoginSuite",  "Check login by new account") ;
    String[] roles = ["user", "admin"] ;
    init("admin", "admin", roles) ;
  }

  public XwikiCheckLoginSuite(String userName, String password, String[] roles) {
    super("XwikiCheckLoginSuite",  "Check login by new account") ;
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
    //User not registered or invalid password
    addWebUnit(
        new SubmitFormUnit("InvalidAccount", "Login with user name ${userName} and password ${password}").
        setFormId("loginForm").
        setField(j_username:userName).
        setField(j_password:"invalidPassword").
        addValidator(new ExpectFormIdValidator("loginForm"))
    );        
    //Username and password is verified
    addWebUnit(
        new SubmitFormUnit("ValidAccount", "Login with user name ${userName} and password ${password}").
        setFormId("loginForm").
        setField(j_username:userName).
        setField(j_password:password).
        addValidator(new ExpectTextValidator("Hello"))
    );
/*
		//what for???
    for(role in roles) {
      addWebUnit(
        new AddRoleUnit("AddRole: ${role}", "Add user role to the web client").
        setRoleToAdd(role)
      );
    }
*/
  }  
}
