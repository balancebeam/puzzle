package io.anyway.puzzle.core.event;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistrationImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.aware.PluginContextAware;
import io.anyway.puzzle.core.common.Constants;
/**
 * Event Subscriber, can listen multiple event topic
 * @author yangzz
 *
 */
@SuppressWarnings("rawtypes")
abstract public class EventSubscriber implements InitializingBean,
		DisposableBean, PluginContextAware { 

	protected Log logger = LogFactory.getLog(getClass());
	
	private BundleContext bundleContext;

	/**
	 * event topic 
	 * 
	 * @return
	 */
	abstract public String[] getTopics();

	private List<ServiceRegistration> serviceRegistrations= new ArrayList<ServiceRegistration>();

	@Override
	final public void afterPropertiesSet() throws Exception {
		String[] topics = getTopics();
		if (null == topics || topics.length==0) {
			logger.error("Not avaiable event topic");
			return;
		}
		for(String topic: topics){
			if(topic==null || "".equals(topic=topic.trim())){
				logger.error("Topic is not empty");
				continue;
			}
			Dictionary<String, String> dictionary = new Hashtable<String, String>();
			dictionary.put(Constants.SERVICE_TYPE, Constants.EVENT_SERVICE);
			dictionary.put(Constants.EVENT_TOPIC, topic);
			ServiceRegistration serviceRegistration = bundleContext.registerService(EventSubscriber.class.getName(),this, dictionary);
			serviceRegistrations.add(serviceRegistration);
		}
		afterPropertiesPerformed();
	}

	protected void afterPropertiesPerformed() {}

	@Override
	final public void destroy() throws Exception {
		for(ServiceRegistration sr: serviceRegistrations){
			((ServiceRegistrationImpl) sr).unregister();
		}
		destroyPerformed();
	}

	
	protected void destroyPerformed() {}

	@Override
	final public void setPluginContext(PluginContext pluginContext) {
		this.bundleContext = pluginContext.getBundle().getBundleContext();
	}

	public int getPriority() {
		return 100;
	}
	
	public boolean isAsync(){
		return false;
	}
	
	/**
	 * handle event
	 * @param event
	 */
	abstract public void handleEvent(Event event);

}
