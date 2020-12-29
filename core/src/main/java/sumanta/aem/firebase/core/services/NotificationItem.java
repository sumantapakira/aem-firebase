package sumanta.aem.firebase.core.services;

public class NotificationItem {

    private String id;
    private String name;
    private String status;
    private String message;
    private boolean shouldSendNotification;
    private String pageUrl;
    private String topic;

    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }
    public String getPageUrl() {
        return pageUrl;
    }
    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public boolean isShouldSendNotification() {
        return shouldSendNotification;
    }
    public void setShouldSendNotification(boolean shouldSendNotification) {
        this.shouldSendNotification = shouldSendNotification;
    }
}
