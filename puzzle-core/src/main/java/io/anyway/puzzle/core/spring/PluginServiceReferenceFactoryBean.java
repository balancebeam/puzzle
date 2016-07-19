package io.anyway.puzzle.core.spring;

import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.aware.PluginContextAware;
import io.anyway.puzzle.core.common.Constants;
import io.anyway.puzzle.core.internal.PluginRepository;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import io.anyway.puzzle.core.aware.PluginServiceAware;


public class PluginServiceReferenceFactoryBean implements FactoryBean<PluginServiceAware>,PluginContextAware,InitializingBean {

	private PluginContext pluginContext;
	
	private String id;
	
	private String name;
	
	private String interfaceName;
	
	private String uniquePluginName;
	
	private String beanName;
	
	private Class<PluginServiceAware> clazz;
	
	public void setId(String id){
		this.id= id;
	}
	
	public String getId(){
		return id;
	}
	
	public void setName(String name){
		this.name= name;
	}
	
	public void setInterface(String interfaceName){
		this.interfaceName= interfaceName;
	}
	
	@Override
	public PluginServiceAware getObject() throws Exception {
		if(StringUtils.isEmpty(uniquePluginName)){
			return null;
		}
		PluginContext ctx=  PluginRepository.getInstance().getPluginContext(uniquePluginName);
		return (PluginServiceAware)ctx.getApplicationContext().getBean(beanName);
	}

	@Override
	public Class<?> getObjectType() {
		return clazz;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}
	
	@Override
	@SuppressWarnings({ "unchecked"})
	public void afterPropertiesSet() throws Exception {
		clazz= (Class<PluginServiceAware>)pluginContext.getClassLoader().loadClass(interfaceName);
		ApplicationContext applicationContext= pluginContext.getApplicationContext();
		String[] beanNames=applicationContext.getBeanNamesForType(clazz);
		if(beanNames!=null && beanNames.length> 0){
			String t_uniqueComponentName= pluginContext.getUnique(),t_beanName;
			if(!StringUtils.isEmpty(name)){
				for(String it: beanNames){
					t_beanName= it;
					String[] nvb= it.split(Constants.FACTORY_SEGMENT);
					if(nvb.length==2){
						t_uniqueComponentName= nvb[0];
						t_beanName= nvb[1];
					}
					if(t_beanName.equals(name) && !t_beanName.equals(id)){
						uniquePluginName= t_uniqueComponentName;
						beanName= t_beanName;
						return;
					}
				}
			}
			else{
				for(String it: beanNames){
					t_beanName= it;
					String[] nvb= it.split(Constants.FACTORY_SEGMENT);
					if(nvb.length==2){
						t_uniqueComponentName= nvb[0];
						t_beanName= nvb[1];
					}
					if(!t_beanName.equals(id)){
						uniquePluginName= t_uniqueComponentName;
						beanName= t_beanName;
						return;
					}
				}
			}
		}
	}
	
	@Override
	public void setPluginContext(PluginContext pluginContext) {
		this.pluginContext= pluginContext;
	}

}
