package suites.xwiki.share ;

import org.exoplatform.webunit.*;
import org.exoplatform.webunit.unit.*;
import org.exoplatform.webunit.validator.*;

/**
 * May 21, 2004
 * @author: Tuan Nguyen
 * @email:   tuan08@users.sourceforge.net
 **/

public class XwikiLogoutSuite extends WebUnitSuite {
  public XwikiLogoutSuite() {
    super("XwikiLogoutSuite","Log out current page");
	String COMMENT = null;
    COMMENT ="WebUnit cannot catch 'logout' text link even URL";
	addWebUnit(
	    new TODO("WebUnitCannotCatch'Logout'LinkEvenURL!!!",COMMENT).
	    setComment(COMMENT).
	    setStatus(WebUnit.STATUS_INCOMPLETE)
    );
    /*
	addWebUnit(
        new ClickLink("Logout", "Click on 'Log-out'").
        setTextLink("Log-out").
	    addValidator(new ExpectTextValidator("Log-in")).
	    addValidator(new ExpectNoTextValidator("Hello"))
    );
    */
  }
}


