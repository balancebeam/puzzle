package io.anyway.puzzle.bridge.hibernate;

import java.util.HashMap;
/**
 * Hibernate Entity and Classloader mapping repository
 * @author yangzz
 *
 */
public class HibernateEntityClassLoader extends ClassLoader { 
	
	private HashMap<String,ClassLoader> entityLoaderMapper = new HashMap<String,ClassLoader>();
	
	private ClassLoader webAppClassLoader= HibernateEntityClassLoader.class.getClassLoader();
	
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if(entityLoaderMapper.containsKey(name)){
			return entityLoaderMapper.get(name).loadClass(name);
		}
		return webAppClassLoader.loadClass(name);
	}

	public void addClassloader4ClassName(String name,ClassLoader classloader){
		entityLoaderMapper.put(name, classloader);
	}

	public void removeClassName(String name){
		entityLoaderMapper.remove(name);
	}
}
