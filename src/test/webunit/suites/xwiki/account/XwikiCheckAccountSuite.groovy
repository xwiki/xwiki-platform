package suites.xwiki.account ;

import suites.xwiki.account.XwikiCheckCreateAccountSuite;
import suites.xwiki.account.XwikiCheckLoginSuite;
import suites.xwiki.account.XwikiCheckLogoutSuite;
import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;

/**
 * May 21, 2004
 * @author: Tuan Nguyen
 * @email:   tuan08@users.sourceforge.net
 **/

public class XwikiCheckAccountSuite extends WebUnitSuite {
  public XwikiCheckAccountSuite() {
    super("XwikiCheckAccountSuite",  "Create a new account, then login and logout") ;
    addWebSuite(new XwikiCheckCreateAccountSuite()) ;
    String[] roles = ["user"] ;
    addWebSuite(new XwikiCheckLoginSuite("#{client.name}", "#{client.name}", roles)) ;
    addWebSuite(new XwikiCheckLogoutSuite()) ;
  }
}
