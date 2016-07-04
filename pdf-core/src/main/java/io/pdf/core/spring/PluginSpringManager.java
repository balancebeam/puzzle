package io.pdf.core.spring;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistrationImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.pdf.bridge.plugin.PluginFramework;
import io.pdf.core.PluginContext;
import io.pdf.core.aware.PluginServiceAware;
import io.pdf.core.common.Constants;
import io.pdf.core.exception.BaseException;
import io.pdf.core.internal.PluginContextSupport;

@SuppressWarnings("rawtypes")
public class PluginSpringManager {
	
	private static Log logger = LogFactory.getLog(PluginSpringManager.class);
	
	PluginContext pluginContext;
	
	private BundleContext bundleContext;
	
	PluginXmlWebApplicationContext applicationContext;
	
	private ServiceListener serviceListener;
	
	Map<String,List<Map<String,Object>>> serviceReferences = new ConcurrentHashMap<String,List<Map<String,Object>>>();	
	
	List<ServiceRegistration> serviceRegistrations = new CopyOnWriteArrayList<ServiceRegistration>();
	
	public PluginSpringManager(PluginContext pluginContext){
		this.pluginContext= pluginContext;
		this.bundleContext= pluginContext.getBundle().getBundleContext();
	}
	
	public void init(String location){
		applicationContext= new PluginXmlWebApplicationContext(pluginContext);
		applicationContext.setId(pluginContext.getName());
		applicationContext.setParent(WebApplicationContextUtils.getWebApplicationContext(PluginFramework.getFrameworkServletContext()));
		applicationContext.setServletContext(pluginContext.getServletContext());
		applicationContext.setServletConfig(pluginContext.getServletConfig());
		applicationContext.setClassLoader(pluginContext.getClassLoader());
		pluginContext.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
		applicationContext.setConfigLocation(location);
		ConfigurableEnvironment env = applicationContext.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(pluginContext.getServletContext(), null);
		}
		((PluginContextSupport)pluginContext).setApplicationContext(applicationContext);
		try{
			applicationContext.refresh();  
		}catch(Throwable e){
			logger.error("Plugin ["+pluginContext.getName()+"] "+location+" parse failure",e);
			throw new BaseException(e);
		}
		registerPluginService2OSGiServiceBus();
		logger.info(location+" parse success");
	}
	
	//deploy component service
	public void registerPluginService2OSGiServiceBus(){
		PluginXmlWebApplicationContext applicationContext=(PluginXmlWebApplicationContext)pluginContext.getApplicationContext();
		String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
		Class<PluginServiceAware> serviceAware= PluginServiceAware.class;
		try {
			for(String beanName : beanDefinitionNames){
				BeanDefinition definition = applicationContext.getBeanFactory().getBeanDefinition(beanName);
				Class<?> clazz = applicationContext.getClassLoader().loadClass(definition.getBeanClassName());
				if(serviceAware.isAssignableFrom(clazz)){
					Class<?> originCls= applicationContext.getBean(beanName).getClass();
					List<String> result= new ArrayList<String>();
					Class<?> tClass= clazz;
					while(Object.class!= tClass){
						if(!serviceAware.isAssignableFrom(tClass)){
							break;
						}
						if(tClass.isAssignableFrom(originCls)){
							result.add(tClass.getName());
							publishService(tClass,beanName);
						}
						for(Class<?> clz: tClass.getInterfaces()){
							if(serviceAware.isAssignableFrom(clz) && serviceAware!= clz){
								if(!result.contains(clz.getName())){
									result.add(clz.getName());
									publishService(clz,beanName);
								}
							}
						}
						tClass= tClass.getSuperclass();
					}
					
					if(result.isEmpty()){
						result.add(clazz.getName());
						publishService(clazz,beanName);
					}
					
					logger.info("Plugin service: {relevancy="+result+",instance="+applicationContext.getBean(beanName)+",version="+pluginContext.getVersion()+",prototype="+definition.isPrototype()+"}");
				}
			}
		} catch (ClassNotFoundException e) {
			logger.error("Plugin ["+pluginContext.getName()+"] can not found service class: "+e.getMessage(),e);
			throw new BaseException(e);
		}
	}
	
	private void publishService(Class<?> clazz,String beanName){
		Dictionary<String, String> dictionary = new Hashtable<String, String>();
		//set service type
		dictionary.put(Constants.SERVICE_TYPE, Constants.SERVICE_TYPE);
		//set component name
		dictionary.put(Constants.PLUGIN_NAME, pluginContext.getName());
		//set service name
		dictionary.put(Constants.SERVICE_NAME, beanName);
		//set service version
		dictionary.put(Constants.VERSION, pluginContext.getVersion());
		//set interface version
		String serviceAwareVersion= Constants.FRAMEWORK_VERSION;
		if(clazz.getClassLoader() instanceof ModuleClassLoader){
			serviceAwareVersion= ((ModuleClassLoader)clazz.getClassLoader()).getBundle().getVersion().toString();
		}
		dictionary.put(Constants.SERVICE_AWARE_VERSION, serviceAwareVersion);
		Object obj= applicationContext.getBean(beanName);
		serviceRegistrations.add(bundleContext.registerService(clazz.getName(),obj,dictionary));
	}
	
	/**
	 * storage service reference
	 * @param referenceName  className;version=xxx
	 * @param holder
	 */
	public void registerServiceReference(String referenceName,Map<String,Object> holder){
		if(!serviceReferences.containsKey(referenceName)){
			serviceReferences.put(referenceName, new ArrayList<Map<String,Object>>());
		}
	   serviceReferences.get(referenceName).add(holder);
	}
	
	/**
	 * add OSGi service listener
	 */
	public void addServiceListener(){
		if(!serviceReferences.isEmpty()){
			serviceListener= new PluginServiceListener(pluginContext);
			bundleContext.addServiceListener(serviceListener);
			logger.info("add OSGi service listener");
		}
	}
	
	/**
	 *  remove OSGi service listener
	 */
	public void removeServiceListener(){
		if (null != serviceListener) {
			bundleContext.removeServiceListener(serviceListener);
			logger.info("remove OSGi service listener");
		}
	
		for (Iterator<ServiceRegistration> it = serviceRegistrations.iterator(); it.hasNext();) {
			((ServiceRegistrationImpl)it.next()).unregister();
		}
		serviceRegistrations.clear();
		serviceReferences.clear();
	}
	
	/**
	 * destroy component application context
	 */
	public void destroy(){
		if(applicationContext!=null){
			try{
				applicationContext.destroy();
			}catch(Throwable e){
				logger.error("Plugin ["+pluginContext.getName()+"] ApplicationContext destory failure",e);
			}
		}
	}
	
}