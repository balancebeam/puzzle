package io.pdf.core.internal;

import static io.pdf.core.common.Constants.BUNDLE_ALIAS;
import static io.pdf.core.common.Constants.SEMICOLIN_VERSION;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.springframework.util.StringUtils;

import io.pdf.core.PluginContext;
/**
 * 获取所有插件的工厂
 * @author yangzz
 *
 */
public final class PluginRepository {
	
	//联合键name+version
	private final Map<String,PluginContext> uniqueMapping;
	//别名映射容器
	private final Map<String,PluginContext> aliasMapping;

	private static final PluginRepository instance = new PluginRepository();
	
	private PluginRepository(){
		uniqueMapping= new HashMap<String,PluginContext>();
		aliasMapping= new HashMap<String,PluginContext>();
	}
	
	public static PluginRepository getInstance() {
		return instance;
	}
	/*
	 * 添加插件上下文
	 */
	void addPluginContext(PluginContext pluginContext){
		uniqueMapping.put(pluginContext.getUnique(), pluginContext);
		String alias= pluginContext.getAlias();
		if(!StringUtils.isEmpty(alias)){
			if(aliasMapping.containsKey(alias)){
				throw new IllegalArgumentException("Duplicated alias name ["+alias+"], Please change another alias name");
			}
			aliasMapping.put(alias, pluginContext);
		}
	}
	/**
	 * 根据插件的名称和版本号获取插件的上下文
	 * @param name
	 * @param version
	 * @return
	 */
	public PluginContext getPluginContext(String name,String version){
		return uniqueMapping.get(name+ SEMICOLIN_VERSION+ version);
	}
	
	/**
	 * 根据Bundle获取对应的插件上下文
	 * @param bundle
	 * @return
	 */
	public PluginContext getPluginContext(Bundle bundle){
		String name= bundle.getSymbolicName();
		String version= bundle.getVersion().toString();
		return getPluginContext(name,version);
	}
	/**
	 * 根据唯一键或名称获取插件上下文
	 * @param unique
	 * @return
	 */
	public PluginContext getPluginContext(String unique){
		if(uniqueMapping.containsKey(unique)){
			return uniqueMapping.get(unique);
		}
		for(String each: uniqueMapping.keySet()){
			if(unique.startsWith(each)){
				return uniqueMapping.get(each);
			}
		}
		return null;
	}
	/**
	 * 根据别名获取插件上下文
	 * @param alias
	 * @return
	 */
	public PluginContext getPluginContextByAlias(String alias){
		return aliasMapping.get(alias);
	}
	/*
	 * 根据Bundle删除工厂中的插件上下文
	 */
	void removePluginContext(Bundle bundle){
		Dictionary<String, String> headers= bundle.getHeaders();
		String alias= headers.get(BUNDLE_ALIAS);
		if(!StringUtils.isEmpty(alias)){
			aliasMapping.remove(alias);
		}
		String name= bundle.getSymbolicName();
		String version= bundle.getVersion().toString();
		uniqueMapping.remove(name+SEMICOLIN_VERSION+version);
	}
	/**
	 * 获取所有插件上下文
	 * @return
	 */
	public Collection<PluginContext> getAllPluginContext(){
		return Collections.unmodifiableCollection(uniqueMapping.values());
	}
}
