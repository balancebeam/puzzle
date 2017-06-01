package io.anyway.puzzle.bridge.hibernate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.IdClass;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.ClassLoaderHelper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.orm.hibernate4.LocalSessionFactoryBuilder;

public class FrameworkSessionFactoryBean extends LocalSessionFactoryBean implements ResourceLoaderAware,DisposableBean{
	
	protected final Log logger = LogFactory.getLog(FrameworkSessionFactoryBean.class);
	
	private final static HibernateEntityClassLoader entityClassLoader = new HibernateEntityClassLoader();
	
	private SessionFactoryWrapper sessionFactoryDelegate= new SessionFactoryWrapper();
	
	private List<String> addedClasses;
	
	private ClassLoader classloader;
	
	private List<Class<?>> annotatedClasseCollection= new ArrayList<Class<?>>();
	
	private List<Resource> mappingLocationCollection= new ArrayList<Resource>();
	
	static{
		ClassLoaderHelper.overridenClassLoader= entityClassLoader;
	}
	
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		super.setResourceLoader(resourceLoader);
		if(LocalSessionFactoryBean.class.getClassLoader()!=resourceLoader.getClassLoader()){
			classloader= resourceLoader.getClassLoader();
			addedClasses= new ArrayList<String>();
		}
	}
	
	@Override
	public void destroy(){
		super.destroy();
		if(null!= classloader){
			for(String className: addedClasses){
				entityClassLoader.removeClassName(className);
			}
		}
	}
	
	@Override
	public SessionFactory getObject() {
		return sessionFactoryDelegate;
	}

	@Override
	protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
		if(null!= classloader){
			try{
				Field f= Configuration.class.getDeclaredField("metadataSourceQueue");
				f.setAccessible(true);
				Object o = f.get(sfb);
				f= f.getType().getDeclaredField("annotatedClasses");
				f.setAccessible(true);
				@SuppressWarnings("unchecked")
				List<XClass> l= (List<XClass>)f.get(o);
				for(XClass x: l){
					String className= x.getName();
					addedClasses.add(className);
					entityClassLoader.addClassloader4ClassName(className, classloader);
					IdClass id= classloader.loadClass(className).getAnnotation(IdClass.class);
					if(null!= id){
						className= id.value().getName();
						addedClasses.add(className);
						entityClassLoader.addClassloader4ClassName(className, classloader);
					}
				}
			}catch(Exception e){
				logger.error(e.getMessage(),e);
			}
		}
		SessionFactory sessionFactory= super.buildSessionFactory(sfb);
		sessionFactoryDelegate.setSessionFactory(sessionFactory);
		return sessionFactoryDelegate;
	}
	
	@Override
	public void setMappingLocations(Resource... mappingLocations) {
		if(null== mappingLocations){
			return;
		}
		mappingLocationCollection.addAll(Arrays.asList(mappingLocations));
		super.setMappingLocations(mappingLocationCollection.toArray(new Resource[mappingLocationCollection.size()]));
	}
	
	public void removeMappingLocations(Resource... mappingLocations){
		if(null== mappingLocations){
			return;
		}
		mappingLocationCollection.removeAll(Arrays.asList(mappingLocations));
		super.setMappingLocations(mappingLocationCollection.toArray(new Resource[mappingLocationCollection.size()]));
	}
	
	@Override
	public void setAnnotatedClasses(Class<?>... annotatedClasses) {
		if(null== annotatedClasses){
			return;
		}
		annotatedClasseCollection.addAll(Arrays.asList(annotatedClasses));
		super.setAnnotatedClasses(annotatedClasseCollection.toArray(new Class<?>[annotatedClasseCollection.size()]));
	}
	
	public void removeAnnotatedClasses(Class<?>... annotatedClasses) {
		if(null== annotatedClasses){
			return;
		}
		annotatedClasseCollection.removeAll(Arrays.asList(annotatedClasses));
		super.setAnnotatedClasses(annotatedClasseCollection.toArray(new Class<?>[annotatedClasseCollection.size()]));
	}
	
	public HibernateEntityClassLoader getEntityClassLoader(){
		return entityClassLoader;
	}
	
	public void refresh() throws Exception{
		//prevent memory leak
		this.destroy();
		//new SessionFactory
		this.afterPropertiesSet();
	}
	
}
