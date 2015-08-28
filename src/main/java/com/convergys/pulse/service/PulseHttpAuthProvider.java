package com.convergys.pulse.service;

import com.percero.agents.auth.services.IAuthProvider;
import com.percero.agents.auth.vo.BasicAuthCredential;
import com.convergys.pulse.vo.PulseUserInfo;
import com.percero.agents.auth.vo.ServiceIdentifier;
import com.percero.agents.auth.vo.ServiceUser;
import com.percero.serial.map.SafeObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthProvider implementation for Pulse to their http rest endpoint
 * Created by Jonathan Samples<jonnysamps@gmail.com> on 8/27/15.
 */
public class PulseHttpAuthProvider implements IAuthProvider {

    private static Logger logger = Logger.getLogger(PulseHttpAuthProvider.class);
    private static final String ID = "pulse_http";

    public String getID() {
        return ID;
    }

    /**
     * @param credential - String in <USERNAME>:<PASSWORD> format
     * @return
     */
    public ServiceUser authenticate(String credential) {
        ServiceUser result = null;
        BasicAuthCredential cred = BasicAuthCredential.fromString(credential);

        String endpoint = hostPortAndContext +"/Authenticate";
        Map<String, String> params = new HashMap<String, String>();
        params.put("userDomainAndLogin", cred.getUsername());
        params.put("userPassword", cred.getPassword());

        String body = makeRequest(endpoint, params);

        /**
         * Result will be something like
         * <boolean xmlns="http://schemas.microsoft.com/2003/10/Serialization/">true</boolean>
         */
        if(body.contains("true")){
            endpoint = hostPortAndContext+"/retrieve_user";
            params = new HashMap<String, String>();
            params.put("userName", cred.getUsername());

            body = makeRequest(endpoint, params);
            logger.info(body);

            try {
                PulseUserInfo pulseUserInfo = objectMapper.readValue(body, PulseUserInfo.class);
                result = new ServiceUser();
                result.setId(pulseUserInfo.getEmployeeId());
                result.setFirstName(pulseUserInfo.getUserLogin());
                result.setAreRoleNamesAccurate(true);
                result.getIdentifiers().add(new ServiceIdentifier("pulseUserLogin", pulseUserInfo.getUserLogin()));
                // Uncomment this line when we start getting non-static employeeIds... or never, it shouldn't matter
                // result.getIdentifiers().add(new ServiceIdentifier("pulseEmployeeId", pulseUserInfo.getEmployeeId()));
            }
            catch(JsonMappingException jme){ logger.warn(jme.getMessage(), jme); }
            catch(JsonParseException jpe){ logger.warn(jpe.getMessage(), jpe); }
            catch(IOException ioe){ logger.warn(ioe.getMessage(), ioe); }
        }

        return result;
    }

    /**
     * @param url
     * @param params
     * @return
     */
    private String makeRequest(String url, Map<String, String> params){
        String body = "";
        try {
            HttpClient client = getHttpClient();
            String query = "?";
            for(String key : params.keySet()){
                query += key+"="+params.get(key)+"&";
            }
            url += query;

            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            logger.debug("Got response from auth hostPortAndContext: (" + response.getStatusLine().getStatusCode() + ")" + response.getStatusLine().getReasonPhrase());
            body = IOUtils.toString(response.getEntity().getContent(), "UTF8");
        } catch(ClientProtocolException e){
            logger.warn(e.getMessage(), e);
        } catch(IOException ioe){
            logger.warn(ioe.getMessage(), ioe);
        }

        return body;
    }

    private HttpClient getHttpClient(){
        HttpClient httpClient = null;
        if(trustAllCerts) {
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");

                // set up a TrustManager that trusts everything
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }
                }}, new SecureRandom());

                SSLSocketFactory sf = new SSLSocketFactory(sslContext);
                Scheme httpsScheme = new Scheme("https", sf, 443);
                SchemeRegistry schemeRegistry = new SchemeRegistry();
                schemeRegistry.register(httpsScheme);

                ClientConnectionManager cm = new SingleClientConnManager(null, schemeRegistry);
                httpClient = new DefaultHttpClient(cm, null);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        // If don't trustAllCerts or an exception thrown then use the default one.
        if(httpClient == null){
            httpClient = new DefaultHttpClient();
        }

        return httpClient;
    }

    private String hostPortAndContext;
    private ObjectMapper objectMapper;

    /**
     * Only make this true for development when dealing with a self-signed certificate
     */
    private boolean trustAllCerts = false;

    /**
     * @param hostPortAndContext - e.g. https://some_host:5400/auth
     * @param objectMapper
     */
    public PulseHttpAuthProvider(String hostPortAndContext, ObjectMapper objectMapper, boolean trustAllCerts){
        this.hostPortAndContext = hostPortAndContext;
        this.objectMapper = objectMapper;
        this.trustAllCerts = trustAllCerts;
    }

    /**
     * For Testing
     * @param args
     */
    public static void main(String[] args){
        PulseHttpAuthProvider provider = new PulseHttpAuthProvider("https://localhost:8900/auth", new SafeObjectMapper(), true);
        ServiceUser su = provider.authenticate("hoo:ha");
        System.out.println(su.toString());
    }
}
