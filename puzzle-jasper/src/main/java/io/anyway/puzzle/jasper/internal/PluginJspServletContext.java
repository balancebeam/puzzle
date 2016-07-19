package io.anyway.puzzle.jasper.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.common.PluginRequestUtil;
import io.anyway.puzzle.core.hook.wrapper.ResourceWrapper;
import io.anyway.puzzle.core.servlet.PluginServletContext;

public class PluginJspServletContext extends PluginServletContext{
	
	private static Log logger = LogFactory.getLog(PluginJspServletContext.class);

	public PluginJspServletContext(PluginContext pluginContext) {
		super(pluginContext);
	}
	
	public URL getResource(String path) throws MalformedURLException {
		if (path == null)
			return null;
		
		if(logger.isDebugEnabled()){
			logger.debug("解析JSP时所需访问文件的路径："+path);
		}
		ResourceWrapper wrapper= PluginRequestUtil.getPluginResourceWrapper(path);
		if(null!=wrapper){
			if(logger.isDebugEnabled()){
				logger.debug("解析JSP时所需访问组件中文件的信息："+wrapper.getResource());
			}
			return wrapper.getResource();
		}
		return getDelegate().getResource(path);
	}	

	@SuppressWarnings("unchecked")
	public Set<String> getResourcePaths(String path) {
		return getDelegate().getResourcePaths(path);
	}

}
