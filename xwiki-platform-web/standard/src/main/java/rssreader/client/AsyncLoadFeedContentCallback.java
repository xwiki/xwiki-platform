package rssreader.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.History;

import java.util.List;

import api.client.Document;
import api.client.XObject;


/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 2 déc. 2006
 * Time: 17:12:15
 * To change this template use File | Settings | File Templates.
 */
public class AsyncLoadFeedContentCallback implements AsyncCallback {
    private RSSReader reader;
    private String keyword;

    public AsyncLoadFeedContentCallback(RSSReader reader) {
        this.reader =  reader;
        this.keyword = "";
    }

    public AsyncLoadFeedContentCallback(RSSReader reader, String keyword) {
        this.reader =  reader;
        this.keyword = keyword;
    }

    public void onSuccess(Object object) {
            reader.showArticle((List) object, keyword);
    }

    public void onFailure(Throwable caught) {
        reader.showError(caught);
    }
}
