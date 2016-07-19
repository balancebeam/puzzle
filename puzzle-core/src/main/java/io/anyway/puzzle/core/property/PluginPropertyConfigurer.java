package io.anyway.puzzle.core.property;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.anyway.puzzle.bridge.ext.XPropertyConfigurer;
import io.anyway.puzzle.bridge.plugin.PluginFramework;
import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.aware.PluginContextAware;

public class PluginPropertyConfigurer extends
		PropertyPlaceholderConfigurer implements PluginPropertyRepository,PluginContextAware{
	
	private static volatile Properties frameworkProperties;
	
	private PluginContext ctx;
	
	private Properties properties;
	
	public static Properties getFrameworkProperties(){
		if(null==frameworkProperties){
			synchronized(PluginPropertyConfigurer.class){
				if(null==frameworkProperties){
					ServletContext servletContext= PluginFramework.getFrameworkServletContext();
					ApplicationContext context= WebApplicationContextUtils.getWebApplicationContext(servletContext);
					if(context==null){
						return new Properties();
					}
					XPropertyConfigurer configurer = context.getBean(XPropertyConfigurer.class);
					frameworkProperties = configurer.getProperties();
				}
			}
		}
		return frameworkProperties;
	}

	protected Properties mergeProperties() throws IOException {
		if(null==properties){
			properties= new Properties();
			//add framework properties
			properties.putAll(PluginPropertyConfigurer.getFrameworkProperties());
			//add component properties
			properties.putAll(super.mergeProperties());
			//add implemented properties
			Properties implementedProperties= (Properties)ctx.getAttribute("ImplementedProperties");
			if(implementedProperties!= null){
				properties.putAll(implementedProperties);
				ctx.removeAttribute("ImplementedProperties");
			}
		}
		return properties;
	}
	/**
	 * 获取属性集合
	 * @return
	 */
	public Properties getProperties(){
		return properties;
	}
	
	@Override
	public String getProperty(String key) {
		return properties.getProperty(key);
	}
	
	@Override
	public Enumeration<String> getPropertyNames() {
		final Enumeration<Object> it= properties.keys();
		return new Enumeration<String>(){
			@Override
			public boolean hasMoreElements() {
				return it.hasMoreElements();
			}
			@Override
			public String nextElement() {
				return (String)it.nextElement();
			}
		};
	}
	@Override
	public void setPluginContext(PluginContext ctx) {
		this.ctx= ctx;		
	}
	
}
