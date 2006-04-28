/***************************************************************************
 * Copyright 2001-2003 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package suites.xwiki.account ;

import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;

/**
 * May 21, 2004
 * @author: Tuan Nguyen
 * @email:   tuan08@users.sourceforge.net
 * @version: $Id: NewAccountSuite.java,v 1.1 2004/10/11 23:27:29 tuan08 Exp $
 **/

public class XwikiCheckCreateAccountSuite extends WebUnitSuite {
  static String DESC="Check create a new user account";
  static String NAME="XwikiCheckCreateAccountSuite";
    
  public XwikiCheckCreateAccountSuite() {
    super(NAME, DESC);    
    addWebUnit(
      	new NewSessionUnit("NewSession", "Create new session and go to the home page for the first time")
    );    
    addWebUnit(
        new ClickLinkWithText("ClickRegister", "Click on 'Register'").
        setTextLink("Register").
        addValidator(new ExpectFormIdValidator("register"))
    ) ;    
    addWebUnit(
      	new SubmitFormUnit("CreateBlankAccount", "All fields are blank").
	    setFormId("register").
	    setField("register_first_name", "").
	    setField("register_last_name", "").
	    setField("xwikiname", "").
	    setField("register_email", "").
	    setField("register_password", "").
	    setField("register2_password", "").
	    setField("register_company", "").
	    setField("register_city", "").
	    setField("register_country", "").
	    setField("register_blog", "").
	    setField("register_blogfeed", "").
	    setField("register_comment", "").
	    addValidator(new ExpectFormIdValidator("register"))
    ) ;        
    addWebUnit(
  	    new SubmitFormUnit("CreateNotSamePasswordAccount", "Two password are not same").
		setFormId("register").
  	    setField("register_first_name", "#{client.name}").
  	    setField("register_last_name", "#{client.name}").
  	    setField("xwikiname", "#{client.name}").
  	    setField("register_email", "user@localhost.net").
  	    setField("register_password", "client.password1").
  	    setField("register2_password", "client.password2").
  	    setField("register_company", "#{client.name}").
  	    setField("register_city", "#{client.name}").
  	    setField("register_country", "#{client.name}").
  	    setField("register_blog", "#{client.name}").
  	    setField("register_blogfeed", "#{client.name}").
  	    setField("register_comment", "#{client.name}").
  	    addValidator(new ExpectFormIdValidator("register"))     
    ) ;        

  	addWebUnit(
  	    new SubmitFormUnit("CreateValidAccount", "Create xwiki account").
		setFormId("register").
  	    setField("register_first_name", "#{client.name}").
  	    setField("register_last_name", "#{client.name}").
  	    setField("xwikiname", "#{client.name}").
  	    setField("register_email", "user@localhost.net").
  	    setField("register_password", "#{client.name}").
  	    setField("register2_password", "#{client.name}").
  	    setField("register_company", "#{client.name}").
  	    setField("register_city", "#{client.name}").
  	    setField("register_country", "#{client.name}").
  	    setField("register_blog", "#{client.name}").
  	    setField("register_blogfeed", "#{client.name}").
  	    setField("register_comment", "#{client.name}").
  	    addValidator(new ExpectFormIdValidator("newdoc"))
  	    //addValidator(new ExpectTextValidator("The user '#{client.name}' has been correctly registered."))
    ) ;    
  }
}
