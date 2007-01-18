package api.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 19 nov. 2006
 * Time: 19:40:30
 * To change this template use File | Settings | File Templates.
 */
public interface XWikiServiceAsync {

    void getDocument(String fullName, AsyncCallback async);

    void getDocument(String fullName, boolean full, AsyncCallback async);

    void getDocument(String fullName, boolean full, boolean viewDisplayers, boolean editDisplayers, AsyncCallback async);

    void getUser(String fullName, AsyncCallback async);

    void getUserList(int nb, int start, AsyncCallback async);

    void searchDocuments(String sql, int nb, int start, AsyncCallback async);

    void getDocuments(String sql, int nb, int start, AsyncCallback async);

    void getDocuments(String sql, int nb, int start, boolean fullName, AsyncCallback async);

    void getDocuments(String sql, int nb, int start, boolean fullName, boolean viewDisplayers, boolean editDisplayers, AsyncCallback async);

    void updateProperty(String doc, String className, String propertyname, String value, AsyncCallback async);

    void updateProperty(String doc, String className, String propertyname, int value, AsyncCallback async);
}
