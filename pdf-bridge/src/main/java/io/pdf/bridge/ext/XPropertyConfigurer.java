package io.pdf.bridge.ext;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class XPropertyConfigurer extends PropertyPlaceholderConfigurer{

	private Properties properties;
	
	@Override
	protected Properties mergeProperties() throws IOException {
		if(properties==null){
			properties= super.mergeProperties();
		}
		return properties;
	}
	
	public Properties getProperties(){
		return properties;
	}
}
