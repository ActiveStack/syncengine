# Auth Provider

## Basic Auth Provider
Out of the box, ActiveStack provides a basic Auth Provider which uses simple username/password stored in the ActiveStack Auth database.

By default, the Basic Auth Provider is disabled.  In order to enable it, you must add the following line to your [env.properties](src/main/resources/env.properties.sample) file.
```
auth.basic.enabled=true
```

## Custom Auth Provider
ActiveStack allows you to define your own Auth Provider.  Follow these steps to create your own Custom Auth Provider.

- Create a new Custom Auth Provider Factory class.  This class is setup to be autowired by spring.  On PostConstruct, this Factory creates a new instance of a CustomAuthProvider and registers it with the ActiveStack AuthProviderRegistry.
```
@Component
public class CustomAuthProviderFactory {

    private static Logger logger = Logger.getLogger(CustomAuthProviderFactory.class);

	/**
	 * This is the ActiveStack Auth Provider registry. Auth Providers are
	 * identifed by their ID. This ID is passed from the client when attempting
	 * to authorize/register.  It is advised to namespace Auth Provider ID's.
	 */
    @Autowired
    AuthProviderRegistry authProviderRegistry;

    @PostConstruct
    public void init(){
		// Depending on the details of the Auth Provider, you need to inject the appropriate resources.
        CustomAuthProvider provider = new CustomAuthProvider();
        authProviderRegistry.addProvider(provider);
        logger.info("CustomAuth:.............ENABLED");
    }

}
```

- Create a new Custom Auth Provider class. This class does the actual work of authorizing and registering users.  It must implement the [IAuthProvider](src/main/java/com/percero/agents/auth/services/IAuthProvider.java) class, which defines these three functions:
  - `getID()`: Must return the UNIQUE ID of this Auth Provider.  It is recommended to name space any custom Auth Providers just to make sure there is no name collision.
  - `authorize(String credentialString)`: This is the function that is called to authorize a user.
    - The format of `credentialString` is completely customizable -- as long as the client app and server Auth Provider agree on the format.  There is a `BasicAuthCredential` defined as part of ActiveStack that can de-serialize the String `username:password` or `{"username":"<my_user_name","password":"<my_password>"}`.
   - `register(String credentialString)`: This is the function that is called to register a user.  Note that an Auth Provider does NOT need to support this functionality.  If an Auth Provider does NOT support this functionality, the response should simply be an `AuthProviderResponse` with a failed result.
     - The format of `credentialString` is completely customizable -- as long as the client app and server Auth Provider agree on the format.  There is a `BasicAuthCredential` defined as part of ActiveStack that can de-serialize the String `username:password` or `{"username":"<my_user_name","password":"<my_password>"}`.
```
public class CustomAuthProvider implements IAuthProvider {

    private static Logger logger = Logger.getLogger(CustomAuthProvider.class);

	/**
	 * Auth Providers are identifed by their ID. This ID is passed from the
	 * client when attempting to authorize/register. It is advised to namespace
	 * Auth Provider ID's.
	 */
    public static final String ID = "tykoon:custom";
    
	public CustomAuthProvider() {
		// Nothing to do...
    }
	
    public String getID() {
        return ID;
    }

	// This is our credential store for our custom auth.  It is just a map of username/password combinations.
	private Map<String, String> registeredUsers = new ConcurrentHashMap<String, String>();


	/**
	 * The credential string can be in any format - it just needs to be an
	 * agreed upon format between the client and the Auth Provider. Since this
	 * is a custom Auth Provider, the developer is free to choose any format
	 * they desire. In this case, we are using the BasicAuthCredential JSON
	 * format.
	 * 
	 * @param credentialString
	 *            - JSON String in `{"username":<USERNAME>, "password":
	 *            <PASSWORD>}` format
	 * @return
	 */
    public AuthProviderResponse authenticate(String credentialString) {
        AuthProviderResponse response = new AuthProviderResponse();

    	BasicAuthCredential cred = BasicAuthCredential.fromJsonString(credentialString);

        logger.debug("Autheticating user " + cred.getUsername());
        
        // To authorize, we simply check if the user name is in the `registeredUsers` map.
        if (cred.getPassword().equals(registeredUsers.get(cred.getUsername()))) {
        	logger.debug("AUTH SUCCESS: " + cred.getUsername());
        	response.authCode = AuthCode.SUCCESS;
        	
        	// Now put together the ServiceUser.
        	response.serviceUser = getServiceUser(cred.getUsername());
        }
        else {
        	logger.debug("AUTH FAILURE: " + cred.getUsername());
        	response.authCode = AuthCode.UNAUTHORIZED;
        }

    	return response;
    }
    
    /* (non-Javadoc)
     * @see com.percero.agents.auth.services.IAuthProvider#register(java.lang.String)
     */
	/**
	 * Registers a user with this Auth Provider. Note that Auth Providers do NOT
	 * need to support this method. If an Auth Provider does NOT support this
	 * method, then the AuthProviderResponse would simply be set to
	 * failed/rejected.
	 * 
	 * Similar to authorize, the `registrationString` can be any format that the
	 * developer desires, so long as the client and this Auth Provider agree on
	 * the format. In this case, we are going to use the BasicAuthCredential
	 * JSON format.
	 * 
	 * @param registrationString
	 * @return
	 */
    public AuthProviderResponse register(String registrationString) {
        AuthProviderResponse response = new AuthProviderResponse();
    	BasicAuthCredential cred = BasicAuthCredential.fromJsonString(registrationString);

        logger.debug("Registering user " + cred.getUsername());
        
        registeredUsers.put(cred.getUsername().toLowerCase(), cred.getPassword());
        
    	response.authCode = AuthCode.SUCCESS;
    	// Now put together the ServiceUser.
    	response.serviceUser = getServiceUser(cred.getUsername());

    	return response;
    }
    
	/**
	 * Builds up a ServiceUser object that corresponds to the userName. For this
	 * example, we always fill it with the same data.
	 * 
	 * @return
	 */
    protected ServiceUser getServiceUser(String userName) {
    	// Now put together the ServiceUser.
    	ServiceUser serviceUser = new ServiceUser();
    	serviceUser.setAuthProviderID(getID());
    	serviceUser.setFirstName("My");
    	serviceUser.setLastName("Name");
		serviceUser.setId(userName);	// The unique identifier for this Auth Provider for this user.
		serviceUser.setEmails(new ArrayList<String>());
		serviceUser.setIdentifiers(new ArrayList<ServiceIdentifier>());
		
		serviceUser.getEmails().add("myusername@mail.com");
		
		// Add the email address as an identifier. This is typically used to
		// link together a single user across multiple Auth Providers.
		ServiceIdentifier emailServiceIdentifier = new ServiceIdentifier("email", "myusername@mail.com");
		serviceUser.getIdentifiers().add(emailServiceIdentifier);
		
		// Add the user name as another unique identifier.
		ServiceIdentifier userNameServiceIdentifier = new ServiceIdentifier(getID(), userName);
		serviceUser.getIdentifiers().add(userNameServiceIdentifier);
		
		// We are creating an Access Token for this service user.
		serviceUser.setAccessToken(UUID.randomUUID().toString());
		
		// If role names are handled by the Auth Provider, then we set this to `TRUE`, otherwise we set it to `FALSE`.
		serviceUser.setAreRoleNamesAccurate(false);
		
		return serviceUser;
    }
    

}
```
