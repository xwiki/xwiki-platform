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


public class XwikiCreateAccountSuite extends WebUnitSuite {
  static String DESC = "Go to xwiki register page and create an account" ;
  static String NAME = "XwikiCreateAccountSuite" ;
  public XwikiCreateAccountSuite() {    
    super(NAME, DESC) ;
    addWebUnit(
        new NewSessionUnit("NewSession", "Create new session and go to the home page for the first time")      
    );
    addWebUnit(
        new ClickLinkWithText("Register", "Click on 'register'").
        setTextLink("register").
        addValidator(new ExpectFormIdValidator("register"))
    ) ;  
 	addWebUnit(
     	new SubmitFormUnit("CreateNewAccount", "Create xwiki account").
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
  	    setField("register_comment", "#{client.name}")
  	    //addValidator(new ExpectTextValidator("The user '#{client.name}' has been correctly registered."))
    ) ;    
  }  
  	static WebUnit createNewAccountUnit(String userName , String password) {
    WebUnit unit =
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
  	    setField("register_comment", "#{client.name}")
  	    //addValidator(new ExpectTextValidator("The user '#{client.name}' has been correctly registered."))
  	  	return unit ;  	
  	} 
}
