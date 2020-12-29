package sumanta.aem.firebase.core.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;

public class InitFirebaseSingleton {

    private static FirebaseApp firebaseApp = null;
    private static final String DATABASE_URL = "https://aem-voice.firebaseio.com/";
    private static final Logger logger = LoggerFactory.getLogger(InitFirebaseSingleton.class);

    static
    {
        try {
            InputStream gServiceAccountServiceInputStream = InitFirebaseSingleton.class.getResourceAsStream("/firebaseauth/aem-voice.json");
            FirebaseOptions options;
            options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(gServiceAccountServiceInputStream))
                    .setDatabaseUrl(DATABASE_URL)
                    .build();
            firebaseApp = FirebaseApp.initializeApp(options,"aem-voice");

        }catch(Exception e) {
            logger.error("Error: ",e);
        }
    }
    public static FirebaseApp getConnection()
    {
        return firebaseApp;
    }

}
