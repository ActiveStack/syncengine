package com.percero.agents.sync.connectors;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import edu.emory.mathcs.backport.java.util.Collections;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@Component
public class HttpLogicConnector implements ILogicConnector {

	private final Logger logger = Logger.getLogger(getClass());
	
	@SuppressWarnings("unchecked")
	private final static Map<String, HttpConnectorOperation> httpLogicConnectors = Collections.synchronizedMap(new HashMap<String, HttpConnectorOperation>());
	private final static Map<String, MethodResponsePattern> methodResponsePatterns = new HashMap<String, HttpLogicConnector.MethodResponsePattern>();
	
	@Autowired @Value("$pf{connector.http.templateDirectory}")
	String httpTemplateDirectory = "";

	@Autowired @Value("$pf{connector.http.templateFilename:request.ftl}")
	String templateFileName = "";
	
	@Autowired @Value("$pf{connector.http.propertiesFilename:connector.properties}")
	String propertiesFileName = "";
	
	public HttpLogicConnector() {
	}
	
	public static final String CONNECTOR_PREFIX = "HTTP";
	
	@Override
	public String getConnectorPrefix() {
		return CONNECTOR_PREFIX;
	}
	
	/* (non-Javadoc)
	 * @see com.percero.agents.sync.connectors.IConnectorFactory#runOperation(java.lang.String, java.lang.String, java.util.Map)
	 */
	@Override
	public String runOperation(String operationName, String clientId, Object parameters) throws ConnectorException {
		String result = "";
		
		HttpConnectorOperation httpLogicConnector = httpLogicConnectors.get(operationName);
		result = runHttpLogicConnector(httpLogicConnector, parameters);
		
		return result;
	}
	
	private Configuration cfg = null;
	private Configuration getFreemarkerConfig() throws IOException {
		if (cfg == null) {
			cfg = new Configuration(Configuration.VERSION_2_3_23);
			cfg.setDirectoryForTemplateLoading(new File(httpTemplateDirectory));
			cfg.setDefaultEncoding("UTF-8");
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		}

		return cfg;
	}
	
	@PostConstruct
	public void initialize() {
		
		loadOperations();
		
		// Create the root hash
		Map<String, Object> root = new HashMap<String, Object>();
		root.put("clientName", "CONVERGYS");
		root.put("userName", "mobileapp");
		root.put("password", "mobile");
		root.put("value", "10851886002");
		
//		try {
//			String result = runOperation("EmployeeAccruals", null, root);
//			System.out.println(result);
////			runFreemarkerTest("EmployeeAccruals", root);
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
	}
	
	private void loadOperations() {
		
		if (!StringUtils.hasText(httpTemplateDirectory)) {
			logger.warn("No HTTP Connector directory specified, skipping HTTP Connectors");
			return;
		}
		
		File httpConnectorsDirectory = new File(httpTemplateDirectory);
		if (!httpConnectorsDirectory.exists() || !httpConnectorsDirectory.isDirectory()) {
			logger.warn("Unable to locate HTTP Connector directory specified, skipping HTTP Connectors");
			return;
		}
		else {

			// Get a list of all sub-directories here.
			File[] directoryFiles = httpConnectorsDirectory.listFiles();
			if (directoryFiles != null) {
				for(File nextHttpDirectoryFile : directoryFiles) {
					if (nextHttpDirectoryFile.exists() && nextHttpDirectoryFile.isDirectory()) {
						// We have found an HTTP Connector Operation
						try {
							HttpConnectorOperation httpLogicConnector = loadHttpConnectorOperation(nextHttpDirectoryFile);
							httpLogicConnectors.put(httpLogicConnector.getOperationName(), httpLogicConnector);
						} catch(Exception e) {
							logger.error("Unable to load HTTP Connector " + nextHttpDirectoryFile.getAbsolutePath(), e);
						}
					}
				}
			}
		}
	}
	
	private HttpConnectorOperation loadHttpConnectorOperation(File operationDirectory) throws IOException, TemplateException, ConnectorException, URISyntaxException {
		
		HttpConnectorOperation result = new HttpConnectorOperation();
		
		Properties properties = new Properties();
		File propertiesFile = new File(operationDirectory.getAbsolutePath() + File.separator + propertiesFileName);
		FileInputStream is = new FileInputStream(propertiesFile);
		properties.load(is);
		is.close();

		result.setOperationName( properties.getProperty("name") );
		result.setProtocol( properties.getProperty("protocol") );
		result.setHost( properties.getProperty("host") );
		result.setPort( Integer.valueOf(properties.getProperty("port")) );
		result.setFile( properties.getProperty("file") );
		String command = properties.getProperty("command");
		if (!StringUtils.hasText(command)) {
			// Default to POST command
			command = "POST";
		}
		result.setCommand(command);
		
		if (!StringUtils.hasText(result.getOperationName())) {
			// If no operation name, then infer from directory name.
			result.setOperationName( operationDirectory.getName() );
		}
		
		result.setTemplatePath(operationDirectory.getName() + File.separator + templateFileName);
		return result;
	}
	
	public String runHttpLogicConnector(HttpConnectorOperation connector, Object root) throws ConnectorException {
		
		try {
			if (connector.getTemplate() == null) {
				connector.setTemplate(getFreemarkerConfig().getTemplate(connector.getTemplatePath()));
			}
			
			StringWriter stringWriter = new StringWriter();
			connector.getTemplate().process(root, stringWriter);
			
			stringWriter.close();
			stringWriter.flush();
			
			String request = stringWriter.toString();
			logger.debug("HTTP Connector Request (" + connector.getOperationName() + "): " + request);
			
			String result = null;
			if ( "GET".equalsIgnoreCase(connector.getCommand()) ) {
				result = sendGet(connector.getProtocol(), connector.getHost(), connector.getPort(), connector.getFile(), request);
			}
			else if ( "POST".equalsIgnoreCase(connector.getCommand()) ) {
				result = sendPost(connector.getProtocol(), connector.getHost(), connector.getPort(), connector.getFile(), request);
			}
			else {
				throw new ConnectorException(ConnectorException.UNSUPPORTED_COMMAND_METHOD, ConnectorException.UNSUPPORTED_COMMAND_CODE);
			}
			logger.debug("HTTP Connector Result (" + connector.getOperationName() + "): " + result);
			return result;
		} catch(IOException ioe) {
			logger.error("Error processing HTTP Logic Connector " + connector.getOperationName(), ioe);
			throw new ConnectorException(ioe);
		} catch (TemplateException e) {
			logger.error("Error processing HTTP Logic Connector template " + connector.getOperationName(), e);
			throw new ConnectorException(e);
		}
	}
	
//	public void runFreemarkerTest(String operationName, Map<String, Object> root) throws IOException, TemplateException, ConnectorException, URISyntaxException {
//		if (cfg == null) {
//	    	cfg = new Configuration(Configuration.VERSION_2_3_23);
//	    	cfg.setDirectoryForTemplateLoading(new File(httpTemplateDirectory));
//	    	cfg.setDefaultEncoding("UTF-8");
//	    	cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
//		}
//		
//		Properties properties = new Properties();
//		File propertiesFile = new File(httpTemplateDirectory + "/" + operationName + "/connector.properties");
//		FileInputStream is = new FileInputStream(propertiesFile);
//		properties.load(is);
//		is.close();
//		
//		String protocol = properties.getProperty("protocol");
//		String host = properties.getProperty("host");
//		int port = Integer.valueOf(properties.getProperty("port"));
//		String file = properties.getProperty("file");
//		
//		Template template = cfg.getTemplate(operationName + "/request.xml.ftl");
//    	
//		StringWriter stringWriter = new StringWriter();
//		template.process(root, stringWriter);
//		
//		stringWriter.close();
//		stringWriter.flush();
//		
//		String request = stringWriter.toString();
//		System.out.println(request);
//		
//		String result = sendPost(protocol, host, port, file, request);
//		System.out.println(result);
//	}
	
//	public Object run_employeeAccrual() throws ConnectorException {
//		Object result = null;
//		
//		MethodResponsePattern methodResponsePattern = methodResponsePatterns.get("EmployeeAccrual");
//		if (methodResponsePattern == null) {
//			methodResponsePattern = new MethodResponsePattern();
//			methodResponsePattern.methodId = "EmployeeAccrual";
////			methodResponsePattern.successResponsePattern = Pattern.compile("/(?s)ValueObjects=&quot;(.*?)&quot;/");// Pattern.DOTALL);
//			methodResponsePattern.successResponsePattern = Pattern.compile("/(?s)ValueObjects(.*?)ValueObjects>/", Pattern.DOTALL);
//			methodResponsePattern.failureResponsePattern = Pattern.compile("^.");
//			methodResponsePattern.errorResponsePattern = Pattern.compile("^.");
//			methodResponsePattern.resultResponsePattern = Pattern.compile(".*");
//			
//			methodResponsePatterns.put("EmployeeAccrual", methodResponsePattern);
//		}
//		
//		String protocol = "http";
//		String host = "localhost";
//		int port = 8900;
//		String file = "/estart/employee_accruals";
//		String requestBody = "   <soapenv:Header>" +
//"      <mcs:UsernameToken>" +
//"         <mcs:clientName>CONVERGYS</mcs:clientName>" +
//"         <mcs:user>csadmin</mcs:user>" +
//"         <mcs:password>pass123</mcs:password>" +
//"      </mcs:UsernameToken>" +
//"   </soapenv:Header>" +
//"   <soapenv:Body>" +
//"      <mcs:RetrieveData>" +
//"         <mcs:businessObjectName>ACCRUALS_EMPLOYEES</mcs:businessObjectName>" +
//"         <mcs:queryValueObject  pageNumber=\"1\" pageSize=\"2\" >" +
//"            <!--You have a CHOICE of the next 11 items at this level-->" +
//"            <ns:StringAttribute name=\"ACCRUALS_EMPLOYEES_PAYROLL\">" +
//"               <ns:string>10851886002</ns:string>" +
//"            </ns:StringAttribute>" +
//"            </mcs:queryValueObject>" +
//"      </mcs:RetrieveData>" +
//"   </soapenv:Body>";
//		
//		String httpResult = sendPost(protocol, host, port, file, requestBody);
////		String soapResult = null;
////		try {
////			soapResult = runSoap(protocol, host, port, file, requestBody);
////		} catch(Exception e) {
////			e.printStackTrace();
////		}
//		System.out.println(httpResult != null ? httpResult.toString() : "NULL response");
//		
//		// Now gauge the response
//		try {
//			Matcher successMatcher = methodResponsePattern.successResponsePattern.matcher(httpResult);
//			boolean found = successMatcher.find();
//			boolean matches = successMatcher.matches();
//			if (found || matches) {
//				// Success!
//				if (found) {
//					int numResults = successMatcher.groupCount();
//					System.out.println(successMatcher.group(1));
//					String[] arrayResults = new String[numResults];
//					
//					for(int i=0; i<numResults; i++) {
//						arrayResults[i] = successMatcher.group(i);
//					}
//					
//					result = arrayResults;
//				}
//				else {
//					result = true;
//				}
//				return result;
//			}
//			else if (methodResponsePattern.failureResponsePattern.matcher(httpResult).matches()) {
//				// Failure!
//				result = methodResponsePattern.resultResponsePattern.split(httpResult);
//			}
//			else if (methodResponsePattern.errorResponsePattern.matcher(httpResult).matches()) {
//				// Error!
//				result = methodResponsePattern.resultResponsePattern.split(httpResult);
//			}
//			else {
//				// Unexpected result
//				throw new ConnectorException(ConnectorException.RESPONSE_PROCESS_FAILED, ConnectorException.RESPONSE_PROCESS_FAILED_CODE);
//			}
//		} catch(Exception e) {
//			throw new ConnectorException(ConnectorException.RESPONSE_PROCESS_FAILED, ConnectorException.RESPONSE_PROCESS_FAILED_CODE);
//		}
//		
//		return result;
//	}
	
//	protected String runSoap(String protocol, String host, int port, String file, String requestBody) throws Exception {
//		// Create SOAP Connection
//        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
//        SOAPConnection soapConnection = soapConnectionFactory.createConnection();
//
//        // Send SOAP Message to SOAP Server
//        String url = protocol + "://" + host;
//        if (port != 80){
//        	url += ":" + port;
//        }
//        url += file;
//        SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(protocol, host, port, requestBody), url);
//
//        // print SOAP Response
//        System.out.print("Response SOAP Message:");
//        soapResponse.writeTo(System.out);
//
//        soapConnection.close();
//        
//        return soapResponse.getSOAPBody().getTextContent();
//	}
//	
//	private static SOAPMessage createSOAPRequest(String protocol, String host, int port, String requestBody) throws Exception {
//        MessageFactory messageFactory = MessageFactory.newInstance();
//        SOAPMessage soapMessage = messageFactory.createMessage();
//        SOAPPart soapPart = soapMessage.getSOAPPart();
//
//        String serverURI = protocol + "://" + host;
//        if (port != 80){
//        	serverURI += ":" + port;
//        }
//
//        // SOAP Envelope
//        SOAPEnvelope envelope = soapPart.getEnvelope();
//        envelope.addNamespaceDeclaration("example", serverURI);
//
//        /*
//        Constructed SOAP Request Message:
//        <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:example="http://ws.cdyne.com/">
//            <SOAP-ENV:Header/>
//            <SOAP-ENV:Body>
//                <example:VerifyEmail>
//                    <example:email>mutantninja@gmail.com</example:email>
//                    <example:LicenseKey>123</example:LicenseKey>
//                </example:VerifyEmail>
//            </SOAP-ENV:Body>
//        </SOAP-ENV:Envelope>
//         */
//
//        // SOAP Body
//        envelope.setTextContent(requestBody);
////        SOAPBody soapBody = envelope.getBody();
////        SOAPElement soapBodyElem = soapBody.addChildElement("VerifyEmail", "example");
////        SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("email", "example");
////        soapBodyElem1.addTextNode("mutantninja@gmail.com");
////        SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("LicenseKey", "example");
////        soapBodyElem2.addTextNode("123");
////
////        MimeHeaders headers = soapMessage.getMimeHeaders();
////        headers.addHeader("SOAPAction", serverURI  + "VerifyEmail");
//
//        soapMessage.saveChanges();
//
//        /* Print the request message */
//        System.out.print("Request SOAP Message:");
//        soapMessage.writeTo(System.out);
//        System.out.println();
//
//        return soapMessage;
//    }
	
	protected String sendGet(String protocol, String host, int port, String file, String requestParams) throws ConnectorException {
		String result = null;

		URL url;
		try {
			if (StringUtils.hasText(requestParams)) {
				url = new URL(protocol, host, port, file + requestParams);
			}
			else {
				url = new URL(protocol, host, port, file);
			}
		} catch (MalformedURLException e) {
			throw new ConnectorException(ConnectorException.INVALID_URI, ConnectorException.INVALID_URI_CODE, e.getMessage());
		}
		URLConnection connection;
		try {
			connection = url.openConnection();
			setConnectionRequestMethod(connection, "GET");

			result = handleHttpResponse(connection);
		} catch (IOException e) {
			throw new ConnectorException(ConnectorException.UNABLE_TO_REACH_HOST, ConnectorException.UNABLE_TO_REACH_HOST_CODE, e.getMessage());
		}
			
		return result;
	}

	protected String sendPost(String protocol, String host, int port, String file, String requestBody) throws ConnectorException {
		String result = null;

		URL url;
		try {
			url = new URL(protocol, host, port, file);
		} catch (MalformedURLException e) {
			throw new ConnectorException(ConnectorException.INVALID_URI, ConnectorException.INVALID_URI_CODE, e.getMessage());
		}
		URLConnection connection;
		try {
			connection = url.openConnection();
			
			setConnectionRequestMethod(connection, "POST");
			
			connection.setDoOutput(true);
			
			DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
			outputStream.writeBytes(requestBody);
			outputStream.flush();
			outputStream.close();
			
			result = handleHttpResponse(connection);
		} catch (IOException e) {
			throw new ConnectorException(ConnectorException.UNABLE_TO_REACH_HOST, ConnectorException.UNABLE_TO_REACH_HOST_CODE, e.getMessage());
		}
			
		return result;
	}
	
	private String handleHttpResponse(URLConnection connection) throws IOException {
		int responseCode = getConnectionResponseCode(connection);
		logger.debug("HTTP Response Code for " + connection.getURL().toString() + " -> " + responseCode);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String nextLine = null;
		StringBuffer response = new StringBuffer();
		
		while ((nextLine = in.readLine()) != null) {
			response.append(nextLine);
		}
		
		in.close();
		
		return response.toString();
	}
	
	private void setConnectionRequestMethod(URLConnection connection, String requestMethod) throws ProtocolException {
		if (connection instanceof HttpURLConnection) {
			((HttpURLConnection)connection).setRequestMethod( requestMethod );
		}
		else if (connection instanceof HttpsURLConnection) {
			((HttpsURLConnection)connection).setRequestMethod( requestMethod );
		}
	}
	
	private int getConnectionResponseCode(URLConnection connection) throws IOException {
		if (connection instanceof HttpURLConnection) {
			return ((HttpURLConnection)connection).getResponseCode();
		}
		else if (connection instanceof HttpsURLConnection) {
			return ((HttpsURLConnection)connection).getResponseCode();
		}
		else {
			return 0;
		}
	}
	
	private class MethodResponsePattern {
		String methodId = "";
		
		Pattern successResponsePattern;
		Pattern failureResponsePattern;
		Pattern errorResponsePattern;

		Pattern resultResponsePattern;
	}
	
//	public class SoapConnectorDefinition {
//		
//		private String name;
//		private String namespaceURI;
//		private String wsdlUri;
//		private String prefix;
//		private Client client;
//		
//		private Map<String, ActiveStackSoapOperationDefinition> operations;
//
//		public String getName() {
//			return name;
//		}
//
//		public void setName(String name) {
//			this.name = name;
//		}
//
//		public String getWsdlUri() {
//			return wsdlUri;
//		}
//		
//		public void setWsdlUri(String wsdlUri) {
//			this.wsdlUri = wsdlUri;
//		}
//		
//		public String getNamespaceURI() {
//			return namespaceURI;
//		}
//
//		public void setNamespaceURI(String namespaceURI) {
//			this.namespaceURI = namespaceURI;
//		}
//
//		public String getPrefix() {
//			return prefix;
//		}
//		
//		public void setPrefix(String prefix) {
//			this.prefix = prefix;
//		}
//		
//		public Client getClient() {
//			return client;
//		}
//
//		public void setClient(Client client) {
//			this.client = client;
//		}
//
//		public Map<String, ActiveStackSoapOperationDefinition> getOperations() {
//			return operations;
//		}
//
//		public void setOperations(Map<String, ActiveStackSoapOperationDefinition> operations) {
//			this.operations = operations;
//		}
//	}
//	
//	public class ActiveStackSoapOperationDefinition {
//		
//		private String name;
//		private String description;
//		private String namespaceURI;
//		private String prefix;
//		private Map<String, ActiveStackSoapPartDefinition> partsMap;
//		public String getName() {
//			return name;
//		}
//		public void setName(String name) {
//			this.name = name;
//		}
//		public String getDescription() {
//			return description;
//		}
//		public void setDescription(String description) {
//			this.description = description;
//		}
//		public String getNamespaceURI() {
//			return namespaceURI;
//		}
//		public void setNamespaceURI(String namespaceURI) {
//			this.namespaceURI = namespaceURI;
//		}
//		public String getPrefix() {
//			return prefix;
//		}
//		public void setPrefix(String prefix) {
//			this.prefix = prefix;
//		}
//		public Map<String, ActiveStackSoapPartDefinition> getPartsMap() {
//			return partsMap;
//		}
//		public void setPartsMap(Map<String, ActiveStackSoapPartDefinition> partsMap) {
//			this.partsMap = partsMap;
//		}
//	}
//	
//	public class ActiveStackSoapPartDefinition {
//		
//		private QName concreteName;
//		private QName elementName;
//		private Boolean element;
//		private QName pname;
//		private Class typeClass;
//		private QName typeName;
//		private int index;
//		public QName getConcreteName() {
//			return concreteName;
//		}
//		public void setConcreteName(QName concreteName) {
//			this.concreteName = concreteName;
//		}
//		public QName getElementName() {
//			return elementName;
//		}
//		public void setElementName(QName elementName) {
//			this.elementName = elementName;
//		}
//		public Boolean getElement() {
//			return element;
//		}
//		public void setElement(Boolean element) {
//			this.element = element;
//		}
//		public QName getPname() {
//			return pname;
//		}
//		public void setPname(QName pname) {
//			this.pname = pname;
//		}
//		public Class getTypeClass() {
//			return typeClass;
//		}
//		public void setTypeClass(Class typeClass) {
//			this.typeClass = typeClass;
//		}
//		public QName getTypeName() {
//			return typeName;
//		}
//		public void setTypeName(QName typeName) {
//			this.typeName = typeName;
//		}
//		public int getIndex() {
//			return index;
//		}
//		public void setIndex(int index) {
//			this.index = index;
//		}
//	}
}
