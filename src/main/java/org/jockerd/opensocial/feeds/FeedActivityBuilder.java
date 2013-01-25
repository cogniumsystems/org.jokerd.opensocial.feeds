package org.jockerd.opensocial.feeds;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jokerd.opensocial.api.model.ActivityEntry;
import org.jokerd.opensocial.api.model.ActivityObject;
import org.jokerd.opensocial.api.model.DomainName;
import org.jokerd.opensocial.api.model.ObjectId;
import org.jokerd.opensocial.cursors.AbstractActivityBuilder;
import org.jokerd.opensocial.cursors.ActivityEntryUtil;
import org.jokerd.opensocial.cursors.StreamException;
import org.ubimix.commons.json.ext.DateFormatter;
import org.ubimix.commons.json.ext.FormattedDate;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * @author kotelnikov
 */
public class FeedActivityBuilder extends AbstractActivityBuilder {

    public static List<ActivityEntry> readActivities(String uri)
        throws StreamException {
        List<ActivityEntry> list = new ArrayList<ActivityEntry>();
        readActivities(uri, list);
        return list;
    }

    public static void readActivities(String uri, List<ActivityEntry> list)
        throws StreamException {
        SyndFeed feed = FeedActivityBuilder.readRemoteFeed(uri);
        FeedActivityBuilder builder = new FeedActivityBuilder(feed);
        builder.loadActivities(list);
    }

    public static SyndFeed readRemoteFeed(String url) throws StreamException {
        try {
            URL u = new URL(url);
            XmlReader reader = new XmlReader(u);
            try {
                SyndFeed feed = new SyndFeedInput().build(reader);
                return feed;
            } finally {
                reader.close();
            }
        } catch (Throwable t) {
            throw new StreamException("Can not read a feed. URL: " + url, t);
        }
    }

    private final DomainName DOMAIN_NAME = new DomainName("feed");

    private final SyndFeed fFeed;

    private ActivityObject fFeedInfo;

    public FeedActivityBuilder(SyndFeed feed) {
        fFeed = feed;
    }

    private ObjectId buildActionId(
        ActivityObject author,
        ObjectId entryId,
        FormattedDate updateTime) {
        String authorId = author != null ? "" + author.getId() : "";
        String str = authorId + ";" + updateTime + ";" + entryId;
        return generateId(str);
    }

    public ActivityEntry buildActivityEntry(SyndEntry entry) {
        ActivityEntry result = new ActivityEntry();

        FormattedDate updateTime = getEntryUpdateTime(entry);
        String title = getEntryTitle(entry);
        ActivityObject author = getAuthorObject(entry);

        ObjectId entryId = generateId(entry.getUri());
        ObjectId actionId = buildActionId(author, entryId, updateTime);
        result.setId(actionId.toString());

        result.setPublished(updateTime);
        result.setTitle(title);
        result.setActor(author);

        result.setVerb("post");

        ActivityObject activityObject = buildActivityObject(entry);
        result.setObject(activityObject);

        ActivityObject targetObject = getFeedInfoObject();
        result.setTarget(targetObject);

        return result;
    }

    public ActivityObject buildActivityObject(SyndEntry entry) {
        ActivityObject obj = new ActivityObject();
        ObjectId id = getId(entry.getUri());
        obj.setId(id);
        obj.setObjectType("feed-entry");

        ActivityObject author = getAuthorObject(entry);
        if (author != null) {
            obj.setAuthor(author);
        }

        FormattedDate publishTime = getEntryPublishTime(entry);
        obj.setPublished(publishTime);

        FormattedDate updateTime = getEntryUpdateTime(entry);
        obj.setUpdated(updateTime);

        String title = getEntryTitle(entry);
        obj.setDisplayName(title);

        String url = getEntryUrl(entry);
        obj.setUrl(url);

        SyndContent content = getSyndContent(entry);
        if (content != null) {
            String str = content.getValue();
            obj.setContent(str);
        }

        return obj;
    }

    public List<ActivityEntry> getActivities() {
        List<ActivityEntry> result = new ArrayList<ActivityEntry>();
        loadActivities(result);
        return result;
    }

    private ActivityObject getAuthorObject(SyndEntry entry) {
        String author = entry.getAuthor();
        if (author == null || "".equals(author)) {
            String feedAuthor = fFeed.getAuthor();
            author = feedAuthor;
        }
        if (author == null) {
            author = "Anonymous";
        }

        ActivityObject obj = new ActivityObject();
        ObjectId authorId = generateId(author);
        obj.setId(authorId);
        obj.setObjectType("person");
        obj.setDisplayName(author);
        return obj;
    }

    @Override
    protected DomainName getDomainName() {
        return DOMAIN_NAME;
    }

    private FormattedDate getEntryPublishTime(SyndEntry entry) {
        Date date = entry.getPublishedDate();
        if (date == null) {
            date = fFeed.getPublishedDate();
        }
        return DateFormatter.formatDate(date);
    }

    private String getEntryTitle(SyndEntry entry) {
        return entry.getTitle();
    }

    private FormattedDate getEntryUpdateTime(SyndEntry entry) {
        FormattedDate result = null;
        Date date = entry.getUpdatedDate();
        if (date == null) {
            result = getEntryPublishTime(entry);
        } else {
            result = DateFormatter.formatDate(date);
        }
        return result;
    }

    private String getEntryUrl(SyndEntry entry) {
        String str = entry.getLink();
        return str;
    }

    private ActivityObject getFeedInfoObject() {
        if (fFeedInfo == null) {
            fFeedInfo = new ActivityObject();
            ObjectId id = generateId(null);
            fFeedInfo.setId(id);
            String uri = getGeneratedIdBase();
            fFeedInfo.setUrl(uri);
            String title = getFeedTitle();
            fFeedInfo.setDisplayName(title);

            FormattedDate publishTime = getFeedPublishedDate();
            if (publishTime == null) {
                publishTime = getFeedUpdateTime(-1);
            }
            fFeedInfo.setPublished(publishTime);

            FormattedDate updateTime = getFeedUpdateTime(1);
            fFeedInfo.setUpdated(updateTime);

        }
        return fFeedInfo;
    }

    private FormattedDate getFeedPublishedDate() {
        Date date = fFeed.getPublishedDate();
        return DateFormatter.formatDate(date);
    }

    private String getFeedTitle() {
        return fFeed.getTitle();
    }

    private FormattedDate getFeedUpdateTime(int m) {
        List<SyndEntry> entries = fFeed.getEntries();
        FormattedDate result = getFeedPublishedDate();
        for (SyndEntry entry : entries) {
            FormattedDate time = getEntryUpdateTime(entry);
            if (time == null) {
                continue;
            }
            if (result == null || m * result.compareTo(time) > 0) {
                result = time;
            }
        }
        return result;
    }

    @Override
    protected String getGeneratedIdBase() {
        String uri = fFeed.getUri();
        if (uri == null) {
            uri = fFeed.getLink();
        }
        return uri;
    }

    private SyndContent getSyndContent(SyndEntry entry) {
        SyndContent result = null;
        @SuppressWarnings("unchecked")
        List<SyndContent> contentList = entry.getContents();
        for (SyndContent content : contentList) {
            if (content != null) {
                result = content;
                break;
            }
        }
        if (result == null) {
            result = entry.getDescription();
        }
        return result;
    }

    public void loadActivities(List<ActivityEntry> list) {
        @SuppressWarnings("unchecked")
        List<SyndEntry> entries = fFeed.getEntries();
        for (SyndEntry entry : entries) {
            ActivityEntry activityEntry = buildActivityEntry(entry);
            list.add(activityEntry);
        }
        Collections.sort(list, ActivityEntryUtil.ENTRY_COMPARATOR);
    }

}