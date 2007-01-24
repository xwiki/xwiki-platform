package rssreader.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.Window;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 21 janv. 2007
 * Time: 23:35:31
 * To change this template use File | Settings | File Templates.
 */
public class AsyncLoadTagsListCallback implements AsyncCallback {
    RSSReader reader;

    public AsyncLoadTagsListCallback(RSSReader rssReader) {
        this.reader = rssReader;
    }

    public void onFailure(Throwable throwable) {
         reader.showError(throwable);
    }

    public void onSuccess(Object object) {
        reader.showTagsList((List) object);
    }
}
