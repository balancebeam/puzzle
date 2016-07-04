package io.pdf.core.service;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.osgi.internal.serviceregistry.ServiceRegistrationImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import io.pdf.core.PluginContext;
import io.pdf.core.aware.PluginContextAware;
import io.pdf.core.common.Constants;
import io.pdf.core.internal.PluginRepository;
/**
 * component extension service ,can provide service for other component
 * @author yangzz
 *
 */
@SuppressWarnings("rawtypes")
public abstract class PluginExtensionPointService implements InitializingBean,DisposableBean,PluginContextAware{
	
	private String name;
	
	private ServiceRegistration serviceRegistration;
	
	protected PluginContext pluginContext;
	
	public void setName(String name){
		this.name= name;
	}
	
	/**
	 * get extension service name
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * register plugin extension service point
	 * @param ctx
	 */
	public abstract void register(PluginContext ctx);
	
	/**
	 * unregister plugin extension service point
	 * @param ctx
	 */
	public abstract void unregister(PluginContext ctx);
	
	@Override
	final public void afterPropertiesSet() throws Exception{
		Dictionary<String, String> dictionary = new Hashtable<String, String>();
		dictionary.put(Constants.SERVICE_TYPE, Constants.EXTENSION_POINT_SERVICE);
		dictionary.put(Constants.PLUGIN_NAME, pluginContext.getName());
		dictionary.put(Constants.VERSION, pluginContext.getVersion());
		BundleContext bundleContext= pluginContext.getBundle().getBundleContext();
		PluginExtensionPointService object= pluginContext.getApplicationContext().getBean(getClass());
		serviceRegistration= bundleContext.registerService(PluginExtensionPointService.class.getName(), object, dictionary);
		for(PluginContext each: PluginRepository.getInstance().getAllPluginContext()){
			if(each!= pluginContext){
				register(each);
			}
		}
	}
	
	@Override
	final public void destroy() throws Exception{
		for(PluginContext each: PluginRepository.getInstance().getAllPluginContext()){
			unregister(each);
		}
		((ServiceRegistrationImpl)serviceRegistration).unregister();
	}
	
	@Override
	final public void setPluginContext(PluginContext pluginContext){
		this.pluginContext= pluginContext;
	}
	
	public int getPriority() {
		return 100;
	}
}
