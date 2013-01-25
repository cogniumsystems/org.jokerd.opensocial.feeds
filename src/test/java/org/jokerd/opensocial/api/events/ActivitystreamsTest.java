/**
 * 
 */
package org.jokerd.opensocial.api.events;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jockerd.opensocial.feeds.FeedActivityBuilder;
import org.jokerd.opensocial.api.events.Activitystreams.Get;
import org.jokerd.opensocial.api.model.ActivityEntry;
import org.jokerd.opensocial.api.model.Collection;
import org.jokerd.opensocial.api.model.DomainName;
import org.jokerd.opensocial.api.model.GroupId;
import org.ubimix.commons.events.calls.CallListener;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * @author kotelnikov
 */
public class ActivitystreamsTest extends ServiceCallTest {

    public static class FeedActivityGenerator
        extends
        CallListener<Activitystreams.Get> {

        public static SyndFeed readRemoteFeed(String url)
            throws IOException,
            IllegalArgumentException,
            FeedException {
            URL u = new URL(url);
            XmlReader reader = new XmlReader(u);
            try {
                SyndFeed feed = new SyndFeedInput().build(reader);
                return feed;
            } finally {
                reader.close();
            }
        }

        public FeedActivityGenerator() {
        }

        @Override
        protected void handleRequest(Activitystreams.Get event) {
            List<ActivityEntry> activities = new ArrayList<ActivityEntry>();
            try {
                GroupId groupId = event.getGroupId();
                String feedUri = groupId.getLocalIdDecoded();
                SyndFeed feed = readRemoteFeed(feedUri);
                FeedActivityBuilder builder = new FeedActivityBuilder(feed);
                activities = builder.getActivities();
            } catch (Throwable t) {
                event.onError(t);
            }
            event.setActivities(activities);
        }
    }

    /**
     * @param name
     */
    public ActivitystreamsTest(String name) {
        super(name);
    }

    protected String getMessage(String firstName, String lastName) {
        return "Hello, " + firstName + " " + lastName + "!";
    }

    public void test() throws Exception {
        DomainName feedDomain = new DomainName("feed");
        String uri = "http://www.nytimes.com/services/xml/rss/nyt/HomePage.xml";
        // uri = "https://twitter.com/statuses/user_timeline/mkotelnikov.rss";
        GroupId feedId = new GroupId(feedDomain, uri);
        assertEquals(uri, feedId.getLocalIdDecoded());

        fServerEventManager.addListener(
            Activitystreams.Get.class,
            new FeedActivityGenerator());

        Activitystreams.Get get = new Activitystreams.Get();
        get.setGroupId(feedId);
        String str = get.toString();
        System.out.println(str);

        fClientEventManager.fireEvent(
            get,
            new CallListener<Activitystreams.Get>() {
                @Override
                protected void handleResponse(Get event) {
                    Collection<ActivityEntry> activities = event
                        .getActivities();
                    List<ActivityEntry> list = activities.getEntries();
                    for (ActivityEntry activityEntry : list) {
                        System.out
                            .println("=============================================");
                        System.out.println(activityEntry.getUpdated()
                            + "\t-\t"
                            + activityEntry.getTarget());
                    }
                }
            });
        Set<Throwable> errors = get.getErrors();
        assertNull(errors);
    }
}
