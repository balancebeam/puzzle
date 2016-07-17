package io.pdf.core.spring;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.web.context.support.XmlWebApplicationContext;

import io.pdf.core.PluginContext;

public class PluginXmlWebApplicationContext extends XmlWebApplicationContext {

	private PluginContext pluginContext;
	
	public PluginXmlWebApplicationContext(PluginContext pluginContext) {
		this.pluginContext= pluginContext;
	}

	@Override
	public ResourcePatternResolver getResourcePatternResolver() {
		return new PluginResourcePatternResolver(this);
	}
	
	@Override
	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");
		if (location.startsWith("/")) {
			return getResourceByPath(location);
		}
		else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new PluginClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		}
		else {
			try {
				// Try to parse the location as a URL...
				URL url = new URL(location);
				return new UrlResource(url);
			}
			catch (MalformedURLException ex) {
				// No URL -> resolve as resource path.
				return getResourceByPath(location);
			}
		}
	}
	
	@Override
	protected DefaultListableBeanFactory createBeanFactory() {
		PluginListableBeanFactory beanFactory = new PluginListableBeanFactory(getInternalParentBeanFactory());  
		beanFactory.setBeanClassLoader(getClassLoader());
		beanFactory.setPluginContext(pluginContext);
		return beanFactory;
	}
	
}
