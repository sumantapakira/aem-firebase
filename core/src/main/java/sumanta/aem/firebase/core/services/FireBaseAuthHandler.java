package sumanta.aem.firebase.core.services;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.FileInputStream;
import java.io.IOException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.day.crx.security.token.TokenUtil;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.FirebaseDatabase;

@Component(service = AuthenticationHandler.class, immediate = true, property = { "path=/" })

public class FireBaseAuthHandler extends DefaultAuthenticationFeedbackHandler
implements AuthenticationHandler, AuthenticationFeedbackHandler{
	
	private static final Logger logger = LoggerFactory.getLogger(FireBaseAuthHandler.class);
	//private FirebaseDatabase firebaseDatabase;
	private  FirebaseAuth firebaseAuth;
	private static final String DATABASE_URL = "https://aem-voice.firebaseio.com/";
	

	@Reference
	private SlingRepository repository;

	@Override
	public void dropCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response) {
		
		FirebaseToken decodedToken;
		try {
			if(StringUtils.isNotBlank(getJWTHeader(request))) {
			decodedToken = getFirebaseAuth().verifyIdToken(getJWTHeader(request));
			String uid = decodedToken.getUid();
			logger.debug("uid : "+uid);
			logger.debug("email : "+decodedToken.getEmail());
			logger.debug("name : "+decodedToken.getName());
			if(StringUtils.isNoneBlank(uid)) {
				process(request, response,decodedToken.getEmail(),decodedToken.getUid());
			}else {
				logger.debug("****** uid not found*******");
				return null;
			}
		}
			
		} catch (FirebaseAuthException e) {
			logger.error("Error: ", e);
		}
    	
    	
    	return null;
	}

	@Override
	public boolean requestCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			response.sendRedirect(request.getRequestURI());
		} catch (IOException e) {
			logger.error("Could not redirect : ", e);
		}
		return false;
	}
	
	public AuthenticationInfo process(HttpServletRequest request, HttpServletResponse response, String username,
			String token) {
		AuthenticationInfo authInfo = new AuthenticationInfo("FIREBASE");
		Session serviceSession = null;
		Authorizable authorizable = null;
		try {
			serviceSession = this.repository.loginService("firebaseauth", null);
			authorizable = createOrUpdateCRXUser(serviceSession, username, token);
			if (authorizable != null) {
				synchronizeAttributes(serviceSession, authorizable);
			}
			serviceSession.save();
			/*Cookie cookie = new Cookie("name", username);
			cookie.setPath("/");
			response.addCookie(cookie);*/
			authInfo = TokenUtil.createCredentials(request, response, this.repository, username, true);
		} catch (RepositoryException e) {
			logger.error("Error : ", e);
			authInfo = AuthenticationInfo.FAIL_AUTH;
		} finally {
			if (null != serviceSession) {
				serviceSession.logout();
			}
		}
		if (authorizable == null) {
			return AuthenticationInfo.FAIL_AUTH;
		}
		authInfo.put("$$auth.info.login$$", new Object());
		return authInfo;
	}

	private static Authorizable createOrUpdateCRXUser(Session serviceSession, String username, String idpToken) {
		try {
			UserManager um = ((JackrabbitSession) serviceSession).getUserManager();
			Authorizable authorizable = um.getAuthorizable(username);
			if (authorizable == null) {
				authorizable = um.createUser(username, null);
			}
			if ((authorizable != null)) {
				ValueFactory vf = serviceSession.getValueFactory();
				authorizable.setProperty("idpToken", vf.createValue(idpToken));
			}
			return authorizable;
		} catch (AccessDeniedException e) {
			logger.error("User synchronization failed: Could not get user manager.", e);
		} catch (RepositoryException e) {
			logger.error("User synchronization failed: Could not access repository.", e);
		}
		return null;
	}

	private static void synchronizeAttributes(Session serviceSession, Authorizable authorizable) {
		try {
			ValueFactory vf = serviceSession.getValueFactory();
			authorizable.setProperty("givenName", vf.createValue(""));
			authorizable.setProperty("familyName", vf.createValue(""));
			
		} catch (RepositoryException e) {
			logger.error("Attribute synchronization failed.", e);
		}
	}
	
	private String getJWTHeader(HttpServletRequest request) {
		String firebaseToken = request.getParameter("token");
		logger.debug("firebaseToken : "+firebaseToken);
		if (StringUtils.isBlank(firebaseToken)) {
			firebaseToken = request.getHeader("Authorization");
			if (StringUtils.isNotBlank(firebaseToken)) {
				firebaseToken =  StringUtils.substringAfter(request.getHeader("Authorization"), " ");
			}
		}
		return firebaseToken;
    }
	
	 @Activate
	    public void activate() {
	    	logger.debug("*****initializing*****");
	    	initFirebase();
	    }
	 
	 private void initFirebase() {
	        try {
	        	FirebaseApp app = InitFirebaseSingleton.getConnection();
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
