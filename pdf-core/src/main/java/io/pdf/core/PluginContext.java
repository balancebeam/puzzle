package io.pdf.core;

import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.springframework.context.ApplicationContext;

public interface PluginContext {
	/**
	 * 获取插件名字
	 * 对应MANIFEST文件中的Bundle-SymbolicName节点
	 * @return
	 */
	String getName();
	/**
	 * 获取插件版本号
	 * 对应MANIFEST文件中的Bundle-Version节点
	 * @return
	 */
	String getVersion();
	/**
	 * 获取插件的唯一键，插件名加版本号
	 * Bundle-SymbolicName+";VER="+Bundle-Version
	 * @return 
	 */
	String getUnique();
	/**
	 * 获取插件的别名，用于访问插件内的静态资源
	 * 对应MANIFEST文件中的Bundle-Name节点
	 * @return
	 */
	String getAlias();
	/**
	 * 获取插件MANIFEST定义的头信息
	 * @return
	 */
	Dictionary<String, String> getManifest();
	/**
	 * 获取组件的启动状态
	 * @return
	 */
	int getState();
	/**
	 * 获取插件的类加载器
	 * @return
	 */
	ClassLoader getClassLoader();
	/**
	 * 获取插件本地的类
	 * @param name
	 * @return
	 * @throws ClassNotFoundException
	 */
	Class<?> findLocalClass(String name)throws ClassNotFoundException;
	/**
	 * 获取插件本地的资源文件
	 * @param resource
	 * @return
	 */
	URL findLocalResource(String resource);
	/**
	 * 获取插件指定目录下的所有子文件
	 * @param path
	 * @return
	 */
	Collection<URL> getResourcePaths(String path);
	/**
	 * 获取插件的ServletConfig配置
	 * @return
	 */
	ServletConfig getServletConfig();
	/**
	 * 获取插件的ServletContext上下文
	 * @return
	 */
	ServletContext getServletContext();
	/**
	 * 获取插件的应用上下文
	 * @return
	 */
	ApplicationContext getApplicationContext();
	/**
	 * 获取插件自定义属性值
	 * @param key
	 * @return
	 */
	Object getAttribute(String key);
	/**
	 * 设置插件自定义属性值
	 * @param key
	 * @param value
	 */
	void setAttribute(String key,Object value);
	/**
	 * 删除插件自定义属性
	 * @param key
	 */
	void removeAttribute(String key);
	/**
	 * 获取插件自定义属性键值列表
	 * @return
	 */
	Collection<String> getAttributeKeys();
	/**
	 * 判断是否包含自定义属性
	 * @param key
	 * @return
	 */
	boolean containAttributeKey(String key);
	/**
	 * 获取Bundle对象
	 * @return
	 */
	Bundle getBundle();
	
}
