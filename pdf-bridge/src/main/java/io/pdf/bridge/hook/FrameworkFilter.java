package io.pdf.bridge.hook;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.pdf.bridge.plugin.PluginFramework;

/**
 * Framework Bridge Filter
 * @author yangzz
 *
 */
public class FrameworkFilter implements Filter {
	
	private static Log logger = LogFactory.getLog(FrameworkFilter.class);
	
	private Filter delegate;
	
	private static FrameworkFilter instance;
	
	private FilterConfig filterConfig;
	
	private PluginFramework framework;
	
	public synchronized static void registerFilterDelegate(Filter delegate) {
		instance.delegate= delegate;
		try {
			delegate.init(instance.filterConfig);
		} catch (ServletException e) {
			logger.error("Framework Bridge Filter started failure",e);
		}
	}
	
	public synchronized static void unregisterFilterDelegate() {
		instance.delegate= null;
	}

	public synchronized void init(FilterConfig config) throws ServletException {
		instance= this;
		filterConfig= config;
		framework= new PluginFramework();
		try{
			framework.init(config.getServletContext());
			logger.info("Framework Bridge Filter bootup successful");
		}catch(IllegalArgumentException e){
			destroy();
		}
	}
	
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if(null!= delegate){
			delegate.doFilter(request, response, chain);
			return;
		}
		chain.doFilter(request, response);
	}
	
	public synchronized void destroy() {
		if(null!= delegate){
			delegate.destroy();
		}
		framework.destroy();
		framework= null;
		logger.info("Framework Bridge Filter destroy success");
	}
}
