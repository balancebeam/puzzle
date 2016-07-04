package io.pdf.core.property;

import java.util.Enumeration;
/**
 * get component property repository
 * @author yangzz
 *
 */
public interface PluginPropertyRepository {
	
	/**
	 * get component property value by key
	 * @param key
	 * @return
	 */
	String getProperty(String key);
	
	/**
	 * get all available property key, include framework 
	 * @return
	 */
	Enumeration<String> getPropertyNames();
	
}
