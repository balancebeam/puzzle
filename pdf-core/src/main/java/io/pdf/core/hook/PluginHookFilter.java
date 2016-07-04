package io.pdf.core.hook;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import io.pdf.bridge.ext.XPropertyConfigurer;
import io.pdf.bridge.hook.FrameworkFilter;
import io.pdf.core.common.Constants;
import io.pdf.core.common.PluginRequestUtil;
import io.pdf.core.exception.BaseException;
import io.pdf.core.servlet.PluginWebInstanceRepository;
import io.pdf.core.servlet.wrapper.FilterWrapper;
import io.pdf.core.servlet.wrapper.HttpRequestWrapper;
/**
 * Hook Filter delegate handle Web Filter and Servlet request
 * @author yangzz
 *
 */
public class PluginHookFilter implements Filter,ApplicationContextAware{

	private Log logger = LogFactory.getLog(PluginHookFilter.class);
	
	public PluginHookFilter(){
		FrameworkFilter.registerFilterDelegate(this);
	}
	
	private String[] iInterceptPatternsNone= new String[]{};
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}
	
	@Override
	final public void doFilter(ServletRequest request, 
			ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		String requestPath= PluginRequestUtil.getRequestPath((HttpServletRequest)request);
		for(String pattern: iInterceptPatternsNone){
			if(PluginRequestUtil.match(pattern, requestPath)){
				chain.doFilter(request, response);
				return;
			}
		}
		
		if(logger.isDebugEnabled()){
			logger.debug("RequestPath: "+requestPath);
		}
		try{
			VirtualFilterChain vfc = new VirtualFilterChain(chain,requestPath);
			vfc.doFilter(request, response);
		}catch(BaseException e){ //unified exception handler
			logger.error(e.getMessage(),e);
			e.handle((HttpServletRequest)request, (HttpServletResponse)response);
		}
		catch(Throwable e){
			logger.error(e.getMessage(),e);
			new BaseException(e).handle((HttpServletRequest)request, (HttpServletResponse)response);
		}
	}
	
	final public void destroy() {
		FrameworkFilter.unregisterFilterDelegate();
	}
	
	private class VirtualFilterChain implements FilterChain {
		private final FilterChain originalChain;
		private final List<FilterWrapper> additionalFilters;
		private int currentPosition = 0;
		private String requestPath;

		public VirtualFilterChain(FilterChain chain,String requestPath) {
			this.originalChain= chain;
			this.requestPath= requestPath;
			additionalFilters= PluginWebInstanceRepository.getFilters();
		}

		public void doFilter(final ServletRequest req,
				final ServletResponse response) throws IOException,
				ServletException {
			HttpServletRequest request= (HttpServletRequest)req;			
			if (currentPosition== additionalFilters.size()) {
				originalChain.doFilter(request, response);
			} else {
				currentPosition++;
				FilterWrapper wrapper= additionalFilters.get(currentPosition - 1);
				Filter nextFilter= wrapper.getFilter();
				List<String> patterns= wrapper.getPatterns();
				for (String pattern: patterns) {
					if (PluginRequestUtil.match(pattern, requestPath)) {
						HttpRequestWrapper requestWrapper= null;
						if(request instanceof HttpRequestWrapper){
							requestWrapper= (HttpRequestWrapper)request;
							requestWrapper.setServletContext(wrapper.getServletContext());
						}
						else{
							requestWrapper= new HttpRequestWrapper(request,wrapper.getServletContext());
						}
						nextFilter.doFilter(requestWrapper, response, this);
						return;
					}
				}
				doFilter(request, response);
			}
		}
	}
	
	@Override
	final public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		XPropertyConfigurer configurer = applicationContext.getBean(XPropertyConfigurer.class);
		String patterns= configurer.getProperties().getProperty(Constants.INTERCEPT_NONE_PATTERN);
		if(!StringUtils.isEmpty(patterns)){
			iInterceptPatternsNone= patterns.split(",");
		}
	}
}
