package org.xwiki.plugin.spacemanager.api;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 11 déc. 2007
 * Time: 15:39:05
 * To change this template use File | Settings | File Templates.
 */
public class SpaceManagers {
    protected static Map spacemanagers  = new HashMap();

    public static void addSpaceManager(SpaceManager sm) {
          spacemanagers.put(sm.getSpaceTypeName(), sm);
    }

    public static SpaceManager findSpaceManagerForSpace(String space, XWikiContext context) throws SpaceManagerException {
        XWikiDocument doc;
        try {
            doc = context.getWiki().getDocument(space, "WebPreferences", context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
        String type = doc.getStringValue(SpaceManager.SPACE_CLASS_NAME, "type");
        if (type==null)
            type = SpaceManager.SPACE_DEFAULT_TYPE;
        return findSpaceManagerForType(type);
    }

    public static SpaceManager findSpaceManagerForType(String type) {
       return (SpaceManager) spacemanagers.get(type);
    }
}
