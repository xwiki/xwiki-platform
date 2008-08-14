package org.xwiki.rendering;

/**
 * Mock DocumentManager implementation used for testing, since we don't want to pull any dependency
 * on the Model/Skin/etc for the Rendering module's unit tests.
 *
 * @version $Id: MockVelocityManager.java 10176 2008-06-09 16:11:28Z vmassol $
 * @since 1.6M1
 */
public class MockDocumentManager implements DocumentManager
{
    public String getDocumentContent(String documentName) throws Exception
    {
        return "Some content";
    }

    public boolean exists(String documentName) throws Exception
    {
        return documentName.equals("Space.ExistingPage");
    }

    public String getURL(String documentName, String action, String queryString, String anchor) throws Exception
    {
        String result = "/xwiki/bin/view/" + documentName.replace(".", "/");
        if (anchor != null) {
            result = result + "#" + anchor;
        }
        if (queryString != null) {
            result = result + "?" + queryString;
        }
        return result;
    }
}
