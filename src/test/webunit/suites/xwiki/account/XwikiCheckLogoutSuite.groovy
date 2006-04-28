package suites.xwiki.account ;

import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;

/**
 * May 21, 2004
 * @author: Tuan Nguyen
 * @email:   tuan08@users.sourceforge.net
 **/

public class XwikiCheckLogoutSuite extends WebUnitSuite {
  public XwikiCheckLogoutSuite() {
    super("XwikiCheckLogoutSuite",  "Log out current page") ;
    String COMMENT = null;
    COMMENT ="WebUnit cannot catch 'logout' text link even URL";
	addWebUnit(
	    new TODO("Cannot Logout",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
    addWebUnit(
        new ClickLinkWithText("Log-out", "Click on 'Log-out'").
        setTextLink("Log-out").
	    addValidator(new ExpectNoTextValidator("Hello"))
    ) ;
    */
  }
}
