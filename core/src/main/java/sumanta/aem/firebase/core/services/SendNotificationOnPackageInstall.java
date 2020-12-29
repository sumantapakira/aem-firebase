package sumanta.aem.firebase.core.services;


import org.apache.commons.lang3.StringUtils;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import java.util.*;

@Component(immediate = true)
public class SendNotificationOnPackageInstall {
    private static final Logger LOG = LoggerFactory.getLogger(SendNotificationOnPackageInstall.class);

    private static final String ETC_NODE_PATH = "/etc";
    private static final String SYSTEM_ENV_NODE_PATH = "/etc/packages";

    @Reference
    PushNotificationFirebaseService pushNotificationFirebaseService;

    private Session session = null;
    private ObservationManager observationManager = null;
    private EventListener sysEnvPropertiesListener = null;
    private EventListener etcNewChildrenListener = null;

    @Reference
    private SlingRepository repository;

    @Activate
    public void activate(@SuppressWarnings("rawtypes") final Map properties)
            throws Exception {
        try {
            session = repository.loginService("firebaseauth", null);
            LOG.info("User name:" +session.getUserID());

            observationManager = session.getWorkspace().getObservationManager();

            if (!session.nodeExists(SYSTEM_ENV_NODE_PATH)) {
                LOG.debug("Node at {} does not exist, listening for it to be created", SYSTEM_ENV_NODE_PATH);
                listenForNewChildrenAtEtc();
                return;
            }

            listenForChangesAtEtcSystemEnv();
            LOG.info("Listener for JCR path /etc/packages is active");
        }catch(Exception e) {
            LOG.error("Error: ",e);
        }
    }

    private void listenForChangesAtEtcSystemEnv() throws RepositoryException {
        sysEnvPropertiesListener = new SystemEnvPropertyChangedListener();
        observationManager.addEventListener(sysEnvPropertiesListener, Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                SYSTEM_ENV_NODE_PATH, true, null, null, true);
        LOG.debug("Listening for changes at {}", SYSTEM_ENV_NODE_PATH);
    }

    private void stopListeningForChangesAtEtcSystemEnv() {
        if (observationManager != null && sysEnvPropertiesListener != null) {
            try {
                observationManager.removeEventListener(sysEnvPropertiesListener);
                LOG.info("Stopped listening for changes at {}", SYSTEM_ENV_NODE_PATH);
            } catch (RepositoryException e) {
                LOG.error("Could not deregister event listener: " + e, e);
            }
        }
    }

    private void listenForNewChildrenAtEtc() throws RepositoryException {
        etcNewChildrenListener = new SystemEnvNodeAddedListener();
        observationManager.addEventListener(etcNewChildrenListener, Event.NODE_ADDED,
                ETC_NODE_PATH, true, null, null, true);
        LOG.debug("Listening for new nodes at {}", ETC_NODE_PATH);


    }

    private void stopListeningForForNewChildrenAtEtc() {
        if (observationManager != null && etcNewChildrenListener != null) {
            try {
                observationManager.removeEventListener(etcNewChildrenListener);
                LOG.info("Stopped listening for new nodes at {}", ETC_NODE_PATH);
            } catch (RepositoryException e) {
                LOG.error("Could not deregister event listener: " + e, e);
            }
        }
    }

    @Deactivate
    public void deactivate() {
        stopListeningForChangesAtEtcSystemEnv();
        stopListeningForForNewChildrenAtEtc();

        if (session != null) {
            session.logout();
            session = null;
        }
    }

    private final class SystemEnvPropertyChangedListener implements EventListener {
        @Override
        public void onEvent(EventIterator events) {


            List<String> changedProperties = new ArrayList<String>();
            String userId = StringUtils.EMPTY;
            String packageName = StringUtils.EMPTY;
            String envName = StringUtils.EMPTY;

           while (events.hasNext()) {
                Event event = events.nextEvent();

                try {
                    if(event.getPath().endsWith("/lastUnpacked")) {
                        changedProperties.add(event.getPath());
                        userId = event.getUserID();
                        String path = event.getPath();
                        if(path.contains(".zip")) {
                            String path1 = event.getPath().substring(0, path.lastIndexOf(".zip"));
                            packageName = StringUtils.substringAfterLast(path1, "/");
                        }
                        break;
                    }
                } catch (RepositoryException e) {
                    LOG.debug("Could not get path from event: " + e, e);
                }
            }

            if (!changedProperties.isEmpty()) {
                LOG.info("Reinstalling env-specific packages since the following properties have changed\n{}",
                        StringUtils.join(changedProperties, "\n"));
                NotificationItem item = new NotificationItem();
                item.setId(UUID.randomUUID().toString());
                item.setMessage(packageName + " was installed by "+ userId);
                item.setName("AEM Package installation status");
                item.setShouldSendNotification(true);
                item.setStatus("approved");
                item.setTopic(userId);

                pushNotificationFirebaseService.sendPushNotification(item);

            }

        }
    }

    private final class SystemEnvNodeAddedListener implements EventListener {
        @Override
        public void onEvent(EventIterator events) {

            while (events.hasNext()) {
                Event event = events.nextEvent();

                try {
                    if (StringUtils.equals(event.getPath(), SYSTEM_ENV_NODE_PATH)) {
                        LOG.info("Node {} was created, starting listener for it.", SYSTEM_ENV_NODE_PATH);
                        stopListeningForForNewChildrenAtEtc();
                        listenForChangesAtEtcSystemEnv();
                    }
                } catch (RepositoryException e) {
                    LOG.error("Could not get path from event: ", e);
                }

            }

        }
    }

}


