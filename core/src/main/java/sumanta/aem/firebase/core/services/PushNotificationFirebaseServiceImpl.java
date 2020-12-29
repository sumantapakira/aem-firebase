package sumanta.aem.firebase.core.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;

@Component(service = PushNotificationFirebaseService.class,configurationPolicy= ConfigurationPolicy.OPTIONAL,  immediate = true, enabled = true)

public class PushNotificationFirebaseServiceImpl implements PushNotificationFirebaseService{

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private FirebaseDatabase firebaseDatabase;
    private  FirebaseAuth firebaseAuth;
    private static final String DATABASE_URL = "https://aem-voice.firebaseio.com/";


    @Override
    public void sendPushNotification(NotificationItem item) {

        if (item != null) {
            /* Get database root reference */
            DatabaseReference databaseReference =firebaseDatabase.getReference("/");;
            logger.debug(databaseReference.getKey());

            /* Get existing child or will be created new child. */
            DatabaseReference childReference = databaseReference.child("user_records");
            logger.debug("childReference " +childReference.getKey());

            Query query = childReference.orderByChild("id").equalTo(item.getId());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()) {
                        String name =  ds.getKey();
                        logger.debug("key: "+name);
                        logger.debug("ref: " + ds.getRef());
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    // TODO Auto-generated method stub
                }
            });
            DatabaseReference userInfo = childReference.child(item.getId());
            logger.debug(userInfo.getKey());
            /**
             * The Firebase Java client uses daemon threads, meaning it will not prevent a process from exiting.
             * So we'll wait(countDownLatch.await()) until firebase saves record. Then decrement `countDownLatch` value
             * using `countDownLatch.countDown()` and application will continues its execution.
             */
            CountDownLatch countDownLatch = new CountDownLatch(1);
            userInfo.setValue(item, new DatabaseReference.CompletionListener() {

                @Override
                public void onComplete(DatabaseError de, DatabaseReference dr) {
                    logger.debug("Record saved!");
                    // decrement countDownLatch value and application will be continues its execution.
                    countDownLatch.countDown();
                }
            });
            try {
                //wait for firebase to saves record.
                countDownLatch.await();
            } catch (InterruptedException ex) {
                logger.error("Error1: ",ex);
            }
        }
    }

    @Activate
    public void activate() {
        logger.debug("***** initializing Firebase *****");
        initFirebase();
    }

    private void initFirebase() {
        try {
            FirebaseApp app = InitFirebaseSingleton.getConnection();
            firebaseDatabase = FirebaseDatabase.getInstance(app);
            firebaseAuth = FirebaseAuth.getInstance(app);
            logger.debug("intialize done");

        } catch (Exception ex) {
            logger.error("Error3: ",ex);
        }
    }

    private  FirebaseAuth getFirebaseAuth() {
        return firebaseAuth;
    }
}
