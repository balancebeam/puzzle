package io.anyway.puzzle.core.servlet;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.aware.PluginContextAware;
import io.anyway.puzzle.core.aware.ServletMetadataAware;
import io.anyway.puzzle.core.exception.PuzzleException;
import io.anyway.puzzle.core.servlet.metadata.FilterMetadata;
import io.anyway.puzzle.core.servlet.metadata.ServletMetadata;
import io.anyway.puzzle.core.servlet.wrapper.FilterWrapper;
import io.anyway.puzzle.core.servlet.wrapper.HttpServletWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.Aware;
import org.springframework.context.ApplicationContextAware;


import io.anyway.puzzle.core.aware.FilterMetadataAware;

/**
 * web.xml parsing manager，to parse every servlet element and instance them
 * @author yangzz
 * @version 1.0.0
 *
 */
@SuppressWarnings("unchecked")
public class PluginServletManager {
	
	private static Log logger = LogFactory.getLog(PluginServletManager.class);
	
	private PluginContext pluginContext;
	
	private Hashtable<String,String> contextparams= new Hashtable<String,String>();	
	
	private List<String> listenerMetadatas= new LinkedList<String>();
	
	private List<FilterMetadata> filterMetadatas= new LinkedList<FilterMetadata>();
	
	private List<ServletMetadata> servletMetadatas= new LinkedList<ServletMetadata>();
	
	private List<EventListener> listenerIntances= new LinkedList<EventListener>();
	
	private List<FilterWrapper> filterIntances= new LinkedList<FilterWrapper>();
	
	private List<HttpServletWrapper> httpServletIntances= new LinkedList<HttpServletWrapper>();
	
	private ServletContext pluginServletContext;
	
	private ServletConfig pluginServletConfig;
	
	public PluginServletManager(PluginContext pluginContext){
		this.pluginContext= pluginContext;
		pluginServletContext= new PluginServletContext(pluginContext,contextparams);
		pluginServletConfig= new PluginServletConfig("PluginServletConfig",pluginServletContext,contextparams);
	}
	
	/**
	 * parse component web.xml
	 * @param in InputStream
	 * @version 1.0.0
	 */
	public void init(URL url){
		SAXReader reader = new SAXReader();
		try {
			Document document = reader.read(url);
			Element root = document.getRootElement();
			parseContextParam(root);
			parseListener(root);
			parseFilter(root);
			parseServlet(root);
		} catch (Exception e) {
			throw new PuzzleException(e.getMessage(),e);
		} 
	}
	/**
	 * instance servlet element
	 */
	public void newInstance(){
		newListenerInstance();
		newFilterInstance();
		newServletInstance();
	}
	
	private void parseContextParam(Element root){
		
		List<Element> context_params = root.elements("context-param");
		if(null!= context_params&& !context_params.isEmpty()){
			for(Element param: context_params){
				String key= ((Element)param.elements("param-name").get(0)).getTextTrim();
				String value= ((Element)param.elements("param-value").get(0)).getTextTrim();
				contextparams.put(key, value);
			}
			logger.info("Context-Param definition: "+contextparams);
		}
		
	}
	
	private void parseListener(Element root){
		
		List<Element> listeners = root.elements("listener");
		if(null!= listeners&& !listeners.isEmpty()){
			for(Element listener: listeners){
				String clazz= ((Element)listener.elements("listener-class").get(0)).getTextTrim();
				listenerMetadatas.add(clazz);
			}
			logger.info("Listener definition: "+listenerMetadatas);
		}
	}
	
	private void parseFilter(Element root){
		
		List<Element> filters = root.elements("filter");
		if(null!= filters&& !filters.isEmpty()){
			Map<String,FilterMetadata> hash= new LinkedHashMap<String,FilterMetadata>();
			for(Element filter: filters){
				String name= ((Element)filter.elements("filter-name").get(0)).getTextTrim();
				String clazz= ((Element)filter.elements("filter-class").get(0)).getTextTrim();
				
				FilterMetadata metadata= new FilterMetadata(name,clazz,pluginContext);
				
				List<Element> initParams= filter.elements("init-param");
				if(null!= initParams&& !initParams.isEmpty()){
					for(Element param: initParams){
						String key= ((Element)param.elements("param-name").get(0)).getTextTrim();
						String value= ((Element)param.elements("param-value").get(0)).getTextTrim();
						metadata.addInitParam(key,value); 
					}
				}
				hash.put(name, metadata);
				filterMetadatas.add(metadata);
			}
			
			List<Element> mappings= root.elements("filter-mapping");
			if(null!= mappings && !mappings.isEmpty()){
				for(Element mapping: mappings){
					String name= ((Element)mapping.elements("filter-name").get(0)).getTextTrim();
					String pattern= ((Element)mapping.elements("url-pattern").get(0)).getTextTrim();
					if(!hash.containsKey(name)){
						throw new PuzzleException("No filter definition: "+ name);
					}
					hash.get(name).addUrlPattern(pattern);
				}
			}
			logger.info("Filter definition: "+filterMetadatas);
		}
		
	}

	private void parseServlet(Element root){
		
		List<Element> servets = root.elements("servlet");
		if(null!= servets&& !servets.isEmpty()){
			Map<String,ServletMetadata> hash= new LinkedHashMap<String,ServletMetadata>();
			for(Element servlet: servets){
				String name= ((Element)servlet.elements("servlet-name").get(0)).getTextTrim();
				String clazz= ((Element)servlet.elements("servlet-class").get(0)).getTextTrim();
				
				ServletMetadata metadata= new ServletMetadata(name,clazz,pluginContext);
				
				List<Element> initParams= servlet.elements("init-param");
				if(null!= initParams&& !initParams.isEmpty()){
					for(Element param: initParams){
						String key= ((Element)param.elements("param-name").get(0)).getTextTrim();
						String value= ((Element)param.elements("param-value").get(0)).getTextTrim();
						metadata.addInitParam(key,value); 
					}
				}
				
				List<Element> startup= servlet.elements("load-on-startup");
				if(null!= startup && !startup.isEmpty()){
					if(startup.size()> 1){
						throw new PuzzleException("Reduplicative servlet attribute load-on-startup : "+ name);
					}
					String idx= ((Element)startup.get(0)).getTextTrim();
					metadata.setStartup(Integer.parseInt(idx));
				}
				hash.put(name, metadata);
				servletMetadatas.add(metadata);
			}
			
			List<Element> mappings= root.elements("servlet-mapping");
			if(null!= mappings && !mappings.isEmpty()){
				for(Element mapping: mappings){
					String name= ((Element)mapping.elements("servlet-name").get(0)).getTextTrim();
					String pattern= ((Element)mapping.elements("url-pattern").get(0)).getTextTrim();
					if(!hash.containsKey(name)){
						throw new PuzzleException("No servlet definition: "+ name);
					}
					hash.get(name).addUrlPattern(pattern);
				}
			}
			
			Collections.sort(servletMetadatas, new Comparator<ServletMetadata>() {
				public int compare(ServletMetadata p,ServletMetadata n) {
					return p.getStartup() - n.getStartup();
				}
			});
			logger.info("Servlet definition: "+servletMetadatas);
		}
	}
	
	private void newListenerInstance(){
		if(listenerMetadatas.isEmpty()){
			return;
		}
		ClassLoader classloader= pluginContext.getClassLoader();
		try {
			for(String lisenterClass: listenerMetadatas){
				Class<?> clazz = classloader.loadClass(lisenterClass);
				EventListener listener = (EventListener)clazz.newInstance();
				injectComponentAware(listener);
				if(listener instanceof ServletContextListener){
					((ServletContextListener)listener).contextInitialized(new ServletContextEvent(getServletContext()));
				}
				listenerIntances.add(listener);
			}
			PluginWebInstanceRepository.registerListeners(listenerIntances);
			logger.info("Complete to new and register listener instance");
			
		} catch (Exception e) {
			throw new PuzzleException("New Plugin ["+pluginContext.getName()+"] Listener instance failure",e);
		} 
	}
	
	private void newFilterInstance(){
		if(filterMetadatas.isEmpty()){
			return;
		}
		ClassLoader classloader= pluginContext.getClassLoader();
		try {
			for(FilterMetadata metadata: filterMetadatas){
				Class<?> clazz= classloader.loadClass(metadata.getFilterClass());
				Filter filter= (Filter)clazz.newInstance();
				injectComponentAware(filter);
				if(filter instanceof FilterMetadataAware){
					((FilterMetadataAware)filter).setFilterMetadata(metadata);
				}
				FilterConfig config= new PluginFilterConfig(metadata.getFilterName(),getServletContext(),metadata.getInitParams());
				filter.init(config);
				filterIntances.add(new FilterWrapper(filter,config,metadata));
			}
			PluginWebInstanceRepository.registerFilters(filterIntances);
			logger.info("Complete to new and register filter instance");
			
		} catch (Exception e) {
			throw new PuzzleException("New Plugin ["+pluginContext.getName()+"] Filter instance failure",e);
		} 
	}

	private void newServletInstance(){
		if(servletMetadatas.isEmpty()){
			return;
		}
		ClassLoader classloader= pluginContext.getClassLoader();
		try {
			for (ServletMetadata metadata: servletMetadatas) {
				Class<?> clazz= classloader.loadClass(metadata.getServletClass());
				HttpServlet servlet= (HttpServlet) clazz.newInstance();
				injectComponentAware(servlet);
				if(servlet instanceof ServletMetadataAware){
					((ServletMetadataAware)servlet).setServletMetadata(metadata);
				}
				ServletConfig config= new PluginServletConfig(metadata.getServletName(),getServletContext(),metadata.getInitParams());
				HttpServletWrapper servletWrapper= new HttpServletWrapper(servlet,config,metadata);
				if(metadata.getStartup()>=0){
					servletWrapper.getHttpServlet(); 
				}
				httpServletIntances.add(servletWrapper);
			}
			PluginWebInstanceRepository.registerHttpServlets(httpServletIntances);
			logger.info("Complete to new and register servlet instance");
			
		} catch (Exception e) {
			throw new PuzzleException("New Plugin ["+pluginContext.getName()+"] Servlet instance failure",e);
		} 
	}
	
	/**
	 * get component ServletContext, every component has own ServletContext
	 * @return ServletContext
	 */
	public ServletContext getServletContext(){
		return pluginServletContext;
	}
	
	/**
	 * get component ServletConfig
	 * @return
	 */
	public ServletConfig getServletConfig(){
		return pluginServletConfig;
	}
	
	private void injectComponentAware(Object instance){
		if(instance instanceof Aware){
			if(instance instanceof PluginContextAware){
				((PluginContextAware)instance).setPluginContext(pluginContext);
			}
			if(instance instanceof ApplicationContextAware){
				((ApplicationContextAware)instance).setApplicationContext(pluginContext.getApplicationContext());
			}
		}
	}
	/**
	 * destroy all web instance
	 */
	public void destroy(){
		if(!listenerIntances.isEmpty()){
			PluginWebInstanceRepository.unregisterListeners(listenerIntances);
		}
		if(!filterIntances.isEmpty()){
			PluginWebInstanceRepository.unregisterFilters(filterIntances);
		}
		if(!httpServletIntances.isEmpty()){
			PluginWebInstanceRepository.unregisterHttpServlets(httpServletIntances);
		}
		for(EventListener listener: listenerIntances){
			if(listener instanceof ServletContextListener){
				try{
					((ServletContextListener)listener).contextDestroyed(new ServletContextEvent(getServletContext()));
					logger.info("Destroy listener: "+listener);
				}catch(Throwable e){
					logger.error("Destroy Plugin ["+pluginContext.getName()+"] listener "+listener+" failure",e);
				}
			}
		}
		for(FilterWrapper wrapper: filterIntances){
			try{
				wrapper.getFilter().destroy();
				logger.info("Destroy filter: "+wrapper);
			}catch(Throwable e){
				logger.error("Destroy Plugin ["+pluginContext.getName()+"] filter "+wrapper+" failure",e);
			}
		}
		for(HttpServletWrapper wrapper: httpServletIntances){
			if(wrapper.started()){ 
				try{	
					wrapper.getHttpServlet().destroy();
					logger.info("Destroy servlet: "+wrapper);
				}catch(Throwable e){
					logger.error("Destroy Plugin ["+pluginContext.getName()+"] servlet "+wrapper+" failure",e);
				}
			}
		}
	}
}
