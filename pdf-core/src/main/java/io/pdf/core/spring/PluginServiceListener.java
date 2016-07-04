package io.pdf.core.spring;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.InfrastructureProxy;
import org.springframework.util.ReflectionUtils;

import io.pdf.core.PluginContext;
import io.pdf.core.aware.PluginServiceAware;
import io.pdf.core.common.Constants;
import io.pdf.core.internal.PluginContextSupport;
import io.pdf.core.internal.PluginRepository;

@SuppressWarnings("rawtypes")
public class PluginServiceListener implements ServiceListener {

	private static Log logger = LogFactory.getLog(PluginServiceListener.class);

	private PluginContext context;

	public PluginServiceListener(PluginContext context) {
		this.context = context;
	}

	@Override
	public void serviceChanged(ServiceEvent event) {
		ServiceReference serviceReference = event.getServiceReference();
		if (!Constants.PLUGIN_SERVICE.equals(serviceReference.getProperty(Constants.SERVICE_TYPE))) {
			return;
		}
		switch (event.getType()) {
			case ServiceEvent.REGISTERED:
			case ServiceEvent.UNREGISTERING:
				resolveValue(serviceReference);
				break;
		}
	}

	private List<String> getServiceClassNames(ServiceReference serviceReference) {
		String pluginName = (String) serviceReference.getProperty(Constants.PLUGIN_NAME);
		String beanName = (String) serviceReference.getProperty(Constants.SERVICE_NAME);
		PluginContext pluginContext = PluginRepository.getInstance().getPluginContext(serviceReference.getBundle());
		if(null== pluginName|| null== beanName|| pluginContext== null) {
			return null;
		}
		PluginXmlWebApplicationContext applicationContext = (PluginXmlWebApplicationContext)pluginContext.getApplicationContext();
		BeanDefinition definition = null;
		try {
			definition = applicationContext.getBeanFactory().getBeanDefinition(	beanName);
		} catch (NoSuchBeanDefinitionException e) {
			logger.error("Plugin [" + pluginName + "] can not find bean: "+ beanName, e);
			return null;
		}
		try {
			Class<?> clazz = applicationContext.getClassLoader().loadClass(	definition.getBeanClassName());
			Class<PluginServiceAware> serviceAware = PluginServiceAware.class;
			// if it's type of ComponentServiceAware, return
			if (!serviceAware.isAssignableFrom(clazz)) {
				return null;
			}
			List<String> classNames = new ArrayList<String>();

			Map<String, List<Map<String, Object>>> serviceReferences = 
					((PluginContextSupport)context).getSpringManager().serviceReferences;
			
			Class<?> tClass= clazz;
			while(Object.class!= tClass){
				for (Class<?> clz : tClass.getInterfaces()) {
					if (serviceAware.isAssignableFrom(clz) && serviceAware != clz) {
						String awareVersion= Constants.FRAMEWORK_VERSION;
						if(clz.getClassLoader() instanceof ModuleClassLoader){
							awareVersion= ((ModuleClassLoader)clz.getClassLoader()).getBundle().getVersion().toString();
						}
						String nv= clz.getName()+Constants.SEMICOLIN_VERSION+awareVersion;
						if (serviceReferences.containsKey(nv)) {
							classNames.add(nv);
						}
					}
				}
				tClass= tClass.getSuperclass();
			}
			
			String awareVersion= Constants.FRAMEWORK_VERSION;
			if(clazz.getClassLoader() instanceof ModuleClassLoader){
				awareVersion= ((ModuleClassLoader)clazz.getClassLoader()).getBundle().getVersion().toString();
			}
			String nv= clazz.getName()+Constants.SEMICOLIN_VERSION+awareVersion;
			if (serviceReferences.containsKey(nv)) {
				classNames.add(nv);
			}
			
			if (!classNames.isEmpty()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Component service is found: {componentName: "
							+ context.getName() + ",beanName: " + beanName
							+ ",classNames: " + classNames 
							+ "}");
				}
				return classNames;
			}
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	private void resolveValue(ServiceReference serviceReference) {
		List<String> classNames = getServiceClassNames(serviceReference);
		if (null == classNames) {
			return;
		}		
		
		Map<String, List<Map<String, Object>>> serviceReferences = ((PluginContextSupport)context).getSpringManager().serviceReferences;
		for (String className : classNames) {
			List<Map<String, Object>> references=  serviceReferences.get(className);
			for (Map<String, Object> reference : references) {
				
				String beanName= (String) reference.get("beanName");
				DependencyDescriptor descriptor = (DependencyDescriptor) reference.get("descriptor");
				PluginListableBeanFactory factory= (PluginListableBeanFactory)((PluginXmlWebApplicationContext)context.getApplicationContext()).getBeanFactory();
				Object targetBean = getTargetBean(context.getApplicationContext().getBean(beanName));
				Object newValue= factory.resolveDependency(descriptor, beanName);
				
				try {
					Field f = descriptor.getField();
					if (f != null) {
						ReflectionUtils.setField(f, targetBean, newValue);
					} else {
						Method m = descriptor.getMethodParameter().getMethod();
						ReflectionUtils.invokeMethod(m, targetBean, new Object[] { newValue });
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * dispose proxy to get origin object
	 * 
	 * @param bean
	 * @return
	 */
	private Object getTargetBean(Object bean) {
		if (bean instanceof Advised) {
			try {
				return ((Advised) bean).getTargetSource().getTarget();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return bean;
			}
		}
		if (bean instanceof InfrastructureProxy) {
			return ((InfrastructureProxy) bean).getWrappedObject();
		}
		if (bean instanceof ScopedObject) {
			return ((ScopedObject) bean).getTargetObject();
		}
		return bean;
	}
}
