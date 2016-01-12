package com.percero.example;

import com.percero.agents.auth.helpers.AccountHelper;
import com.percero.framework.bl.ManifestHelper;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class ExampleAccountHelper extends AccountHelper {

	private static final Logger log = Logger.getLogger(ExampleAccountHelper.class);
	
	public static final String ROLE_ADMIN = "ADMIN";
	public static final String ROLE_BASIC = "BASIC";
	
	public ExampleAccountHelper() {
		super();
		manifest = new ExampleManifest();
		ManifestHelper.setManifest(manifest);
	}
}
