package io.pdf.core.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Primary;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;

import io.pdf.core.PluginContext;
import io.pdf.core.aware.PluginContextAware;
import io.pdf.core.aware.PluginServiceAware;
import io.pdf.core.common.Constants;
import io.pdf.core.event.PluginFrameworkListener;
import io.pdf.core.internal.PluginContextSupport;
import io.pdf.core.internal.PluginRepository;

@SuppressWarnings({"rawtypes","unchecked"})
public class PluginListableBeanFactory extends DefaultListableBeanFactory{
	
	private PluginContext pluginContext;
	
	final private Map<String, BeanWrapper> factoryBeanInstanceCache;
	
	public PluginListableBeanFactory(BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
		Field f= ReflectionUtils.findField(AbstractAutowireCapableBeanFactory.class, "factoryBeanInstanceCache");
		ReflectionUtils.makeAccessible(f);
		factoryBeanInstanceCache= (Map<String, BeanWrapper>) ReflectionUtils.getField(f, this);
	}
	
	public void setPluginContext(PluginContext pluginContext) {
		this.pluginContext= pluginContext;
	}
	
	@Override
	public Object getBean(String name) throws BeansException {
		if (name.indexOf(Constants.FACTORY_SEGMENT) > 0) {
			String[] parts = name.split(Constants.FACTORY_SEGMENT);
			return PluginRepository.getInstance().getPluginContext(parts[0]).getApplicationContext().getBean(parts[1]);
		}
		return super.getBean(name);
	}
	
	@Override
	public boolean isPrimary(String beanName, Object beanInstance) {
		return null!=beanInstance.getClass().getAnnotation(Primary.class);
	}
	
	@Override
	protected String determineAutowireCandidate(Map<String, Object> candidateBeans, DependencyDescriptor descriptor) {
		Annotation[] annotations= descriptor.getAnnotations();
		if(null!=  annotations && annotations.length> 0){
			for(Annotation ant: annotations){
				if(ant.annotationType()== Qualifier.class && candidateBeans.containsKey(((Qualifier)ant).value())){
					return ((Qualifier)ant).value();
				}
			}
		}
		return super.determineAutowireCandidate(candidateBeans, descriptor);
	}
	
	@Override
	public String[] getBeanNamesForType(
			Class type,
			boolean includeNonSingletons, boolean allowEagerInit) {
		Class<PluginServiceAware> serviceAware = PluginServiceAware.class;
		Map<String,Object> holder = null;
		if (!serviceAware.isAssignableFrom(type) || 
				serviceAware == type || 
				null==(holder = DependencyDescriptorHolder.getDependencyDescriptor())) {
			return super.getBeanNamesForType(type, includeNonSingletons,allowEagerInit);
		}
		
		Class<?> declaringClass = null;
		DependencyDescriptor descriptor = (DependencyDescriptor)holder.get("descriptor");
		Field field =  descriptor.getField();
		if(null== field){
			MethodParameter methodParameter = descriptor.getMethodParameter();
			if(null!= methodParameter){
				declaringClass= methodParameter.getDeclaringClass();
			}
		}
		else{
			declaringClass= descriptor.getField().getDeclaringClass();
		}
		
		Field f= ReflectionUtils.findField(DependencyDescriptor.class, "required");
		ReflectionUtils.makeAccessible(f);
		ReflectionUtils.setField(f, descriptor, false);
		
		BundleContext bundleContext = pluginContext.getBundle().getBundleContext();
		String serviceAwareVersion= Constants.FRAMEWORK_VERSION;
		if(type.getClassLoader() instanceof ModuleClassLoader){
			serviceAwareVersion= ((ModuleClassLoader)type.getClassLoader()).getBundle().getVersion().toString();
		}
		
		String[] args = super.getBeanNamesForType(declaringClass,true,true);
		if(args.length!=0 && !this.getBeanDefinition(args[0]).isPrototype()){
			PluginContextSupport ctx= (PluginContextSupport)pluginContext;
			//if component is starting, need to save holder
			if(ctx.getState()==Bundle.STARTING){
				//class name & service version
				String nv= type.getName()+Constants.SEMICOLIN_VERSION+serviceAwareVersion;
				ctx.getSpringManager().registerServiceReference(nv,holder);
			}
		}
		
		List<String> beanNames= new ArrayList<String>();
		//first get local bean name
		try{
			String[] nms= super.getBeanNamesForType(type, includeNonSingletons,allowEagerInit);
			if(null!= nms){
				beanNames.addAll(Arrays.asList(nms));
			}
		}catch(BeansException e){
			if(logger.isDebugEnabled()){
				logger.debug(type.getName()+ "not found in the Plugin ["+pluginContext.getName()+"] applicationContext");
			}
		}
		
		try {
			String conditions=  "("+Constants.SERVICE_AWARE_VERSION+"="+serviceAwareVersion+")";
			ServiceReference[] serviceReferences= bundleContext.getAllServiceReferences(type.getName(),conditions);
			if(serviceReferences!=null){
				for(ServiceReference reference: serviceReferences){
					String pluginName= (String)reference.getProperty(Constants.PLUGIN_NAME);
					String beanName= (String)reference.getProperty(Constants.SERVICE_NAME);
					String version= (String)reference.getProperty(Constants.VERSION);
					beanNames.add(pluginName+Constants.SEMICOLIN_VERSION+version+Constants.FACTORY_SEGMENT+beanName);
				}
			}
		} catch (Throwable e) {
			logger.error("Plugin name error",e);
		}
		return beanNames.toArray(new String[beanNames.size()]);
	}
	
	@Override
	protected Map<String, Object> findAutowireCandidates(String beanName,
			Class requiredType,
			DependencyDescriptor descriptor) {
		Map<String, Object> tmp= super.findAutowireCandidates(beanName, requiredType,descriptor);
		Map<String, Object> result= new HashMap<String,Object>(); 
		for(Iterator<Entry<String,Object>> it=tmp.entrySet().iterator();it.hasNext();){
			Entry<String,Object> entry= it.next();
			String key= entry.getKey();
			Object value= entry.getValue();
			if(key.indexOf(Constants.FACTORY_SEGMENT)>0){
				result.put(key.split(Constants.FACTORY_SEGMENT)[1], value);
				continue;
			}
			result.put(key, value);
		}		
		return result;
	}
	
	@Override
	public Object doResolveDependency(DependencyDescriptor descriptor, String beanName,
			Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException {
		
		Map<String, Object> tmp = new HashMap<String,Object>();
		tmp.put("descriptor", descriptor);
		tmp.put("beanName", beanName);
		DependencyDescriptorHolder.setDependencyDescriptor(tmp);
		try{
			return super.doResolveDependency(descriptor,beanName,autowiredBeanNames,typeConverter);
		}finally{
			DependencyDescriptorHolder.setDependencyDescriptor(null);
		}
	}
	
	private static class DependencyDescriptorHolder extends ThreadLocal<Map<String,Object>> {
		
		static DependencyDescriptorHolder instance = new DependencyDescriptorHolder();
		static Map<String,Object> getDependencyDescriptor() {
			return instance.get();
		}
		static void setDependencyDescriptor(Map<String,Object> holder) {
			instance.set(holder);
		}
	}
	
	//inject custom aware interface, in component ApplicationContext
	@Override
	protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
		if (bean instanceof PluginContextAware) {
			((PluginContextAware) bean).setPluginContext(pluginContext);
		}
		//add listener
		if(bean instanceof PluginFrameworkListener){
			PluginFrameworkListener.Processor.addListener((PluginFrameworkListener)bean);
		}
		return super.initializeBean(beanName,bean,mbd);
	}
	
	@Override
	protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args) {
		if (mbd.isSingleton() && factoryBeanInstanceCache.containsKey(beanName)) {
			initBeanWrapper(this.factoryBeanInstanceCache.get(beanName));
		}
		return super.doCreateBean(beanName, mbd, args);
	}
}
