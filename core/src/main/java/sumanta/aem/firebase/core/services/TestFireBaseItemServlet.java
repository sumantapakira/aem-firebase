package sumanta.aem.firebase.core.services;

import com.day.cq.commons.jcr.JcrConstants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;


@Component(service = Servlet.class,
        property = {
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
                "sling.servlet.paths=" + "/bin/firebase/sendpushnotification",
                "sling.servlet.extensions=" + "json"
        })
public class TestFireBaseItemServlet extends SlingSafeMethodsServlet {

    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth firebaseAuth;

    @Reference
    PushNotificationFirebaseService pushNotificationFirebaseService;

    private static final String DATABASE_URL = "https://aem-voice.firebaseio.com/";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void doGet(final SlingHttpServletRequest req,
                         final SlingHttpServletResponse resp) throws ServletException, IOException {
        final Resource resource = req.getResource();
        resp.setContentType("application/json");
        try {
            String name = req.getParameter("name");
            String status = req.getParameter("status");
            String topic = req.getParameter("topic");
            String id = req.getParameter("id");

            NotificationItem item = new NotificationItem();
            item.setId(id);
            item.setName(name);
            item.setShouldSendNotification(true);
            item.setStatus(status);
            item.setPageUrl("https://facebook.com");
            item.setTopic(topic);
            item.setMessage("Some Message");

            pushNotificationFirebaseService.sendPushNotification(item);

            resp.getWriter().write("Title = " + resource.getValueMap().get(JcrConstants.JCR_TITLE));
        } catch (Exception e1) {
            logger.error("Error 4: ", e1);
        }
    }


}
