package io.pdf.demo.filter;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.pdf.core.PluginContext;
import io.pdf.core.aware.PluginContextAware;

public class DemoFilter implements Filter,PluginContextAware,ApplicationContextAware{

	private ApplicationContext applicationContext;
	
	private PluginContext pluginContext;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext= applicationContext;
	}

	@Override
	public void setPluginContext(PluginContext ctx) {
		this.pluginContext= ctx;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		@SuppressWarnings("unchecked")
		Enumeration<String> each= filterConfig.getInitParameterNames();
		while(each.hasMoreElements()){
			String key= each.nextElement();
			String value= filterConfig.getInitParameter(key);
			System.out.println(filterConfig.getFilterName()+": "+key+"="+value);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		System.out.println(applicationContext);
		System.out.println(pluginContext);
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		
	}

}
