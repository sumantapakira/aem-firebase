package sumanta.aem.firebase.core.services;

import com.day.cq.workflow.event.WorkflowEvent;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.model.WorkflowNode;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component(service = EventHandler.class, immediate = true,
        property = {
                EventConstants.EVENT_FILTER + "= (!(event.application=*))",
                EventConstants.EVENT_TOPIC + "= com/day/cq/workflow/event",
                EventConstants.EVENT_TOPIC + "= com/adobe/granite/taskmanagement/event"
        }
)
public class WorkflowPushNotificationEventHandler implements EventHandler{

    private final BlockingQueue<WorkflowEvent> queue;
    private EventProcessor processor;
    private boolean notifyOnComplete;
    private boolean notifyOnContainerComplete;
    private boolean notifyUserOnly;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    PushNotificationFirebaseService pushNotificationFirebaseService;

    public WorkflowPushNotificationEventHandler() {
         this.queue = (BlockingQueue<WorkflowEvent>)new LinkedBlockingQueue();
    }

    @Override
    public void handleEvent(final Event event) {
        if ("com/day/cq/workflow/event".equals((Object)event.getTopic()) && event.getProperty("WorkflowInstanceId") != null) {
            try {
                this.queue.put((WorkflowEvent)event);
                logger.debug(" handleEvent: put event [{}] in queue, size now [{}]", event.getProperty("EventType"), this.queue.size());
            }
            catch (InterruptedException ex) {}
        }
        else {
            logger.debug("bbbb skipping non-workflow event : {}", event.toString());
        }
    }

    @Activate
    protected void activate(final ComponentContext context) {
        this.notifyOnComplete = true; // Should read from OSGI prop
        this.notifyOnContainerComplete = true;
        this.notifyUserOnly = true;

        this.processor = new EventProcessor();
        final Thread thread = new Thread((Runnable)this.processor, this.getClass().getSimpleName() + "-Processor");
        thread.setDaemon(true);
        thread.start();
    }

    @Deactivate
    protected void deactivate(final ComponentContext context) {
        this.processor.stop();
    }

    private boolean doNotifyOnEvent(final WorkflowEvent event) {
        final String eventType = (String)event.getProperty("EventType");
        if (!"WorkflowCompleted".equals((Object)eventType)) {
            return true;
        }
        final String parentId = (String)event.getProperty("ParentWorkflowId");
        if (StringUtils.isNotEmpty(parentId) && StringUtils.startsWithIgnoreCase(parentId, "/etc")) {
            logger.debug("Is container workflow - notifyOnContainerComplete: {}", this.notifyOnContainerComplete);
            return this.notifyOnContainerComplete;
        }
        logger.debug("Not a container workflow - notifyOnComplete: {}", this.notifyOnComplete);
        return this.notifyOnComplete;
    }

    private boolean doNotifyParticipants(final WorkflowNode node, final String eventType) {
        if (node != null && eventType.equals("NodeTransition")) {
            final MetaDataMap metadata = node.getMetaDataMap();
            return metadata != null && metadata.get("DO_NOTIFY_FIREBASE", String.class) != null && (((String)metadata.get("DO_NOTIFY_FIREBASE", String.class)).equals("on") || ((String)metadata.get("DO_NOTIFY_FIREBASE", String.class)).equals("true"));
        }
        return true;
    }

    public void sendNotification(final WorkflowEvent event) {
        final String eventType = (String)event.getProperty("EventType");
        logger.debug("eventType: "+ eventType);
        final WorkItem item = (WorkItem)event.getProperty("Workitem");
        Workflow workflow = (item == null) ? null : item.getWorkflow();
        WorkflowData workflowData = (workflow == null) ? null : workflow.getWorkflowData() ;
        String payload = (workflowData == null) ? StringUtils.EMPTY : workflowData.getPayload().toString();
        logger.debug("payload: "+ payload);

        if (this.doNotifyParticipants((item == null) ? null : item.getNode(), eventType)) {
        NotificationItem notificationItem = new NotificationItem();
        notificationItem.setId(UUID.randomUUID().toString());
        notificationItem.setName("Workflow Review");
        notificationItem.setShouldSendNotification(true);
        notificationItem.setStatus("pending");
        notificationItem.setPageUrl("http://localhost:4502/"+payload + ".html");
        notificationItem.setTopic("workflow-review");
        notificationItem.setMessage("A workitem has been assigned for you to review. \n "+"http://localhost:4502/"+payload + ".html");
        notificationItem.setNotificationType("workflow");

        pushNotificationFirebaseService.sendPushNotification(notificationItem);

        }

    }

    private class EventProcessor implements Runnable
    {
        private volatile boolean isRunning;

        private EventProcessor() {
            this.isRunning = true;
        }

        public void stop() {
            this.isRunning = false;
        }

        public void run() {
            logger.debug("ccccc Processor started");
            while (this.isRunning) {
                try {
                    final WorkflowEvent event = (WorkflowEvent)WorkflowPushNotificationEventHandler.this.queue.take();
                    if (WorkflowPushNotificationEventHandler.this.doNotifyOnEvent(event)) {
                        logger.debug("sending notification for: {}", event.getProperty("EventType"));
                        WorkflowPushNotificationEventHandler.this.sendNotification(event);
                    }
                    else {
                        logger.debug("ignoring notification for: {}", event.getProperty("EventType"));
                    }
                }
                catch (InterruptedException ex) {}
                catch (Exception e) {
                    logger.error("Error while sending email.",e);
                }
            }
            logger.debug("Processor done");
        }
    }
}
