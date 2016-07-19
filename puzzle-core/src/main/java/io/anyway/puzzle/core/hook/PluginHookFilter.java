package io.anyway.puzzle.core.hook;

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

import io.anyway.puzzle.core.common.Constants;
import io.anyway.puzzle.core.servlet.PluginWebInstanceRepository;
import io.anyway.puzzle.core.servlet.wrapper.HttpRequestWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import io.anyway.puzzle.bridge.ext.XPropertyConfigurer;
import io.anyway.puzzle.bridge.hook.PuzzleFilter;
import io.anyway.puzzle.core.common.PluginRequestUtil;
import io.anyway.puzzle.core.exception.PuzzleException;
import io.anyway.puzzle.core.servlet.wrapper.FilterWrapper;

/**
 * Hook Filter delegate handle Web Filter and Servlet request
 * @author yangzz
 *
 */
public class PluginHookFilter implements Filter,ApplicationContextAware{

	private Log logger = LogFactory.getLog(PluginHookFilter.class);
	
	public PluginHookFilter(){
		PuzzleFilter.registerFilterDelegate(this);
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
		
		if(logger.isInfoEnabled()){
			logger.info("RequestPath: "+requestPath);
		}
		
		for(String pattern: iInterceptPatternsNone){
			if(PluginRequestUtil.match(pattern, requestPath)){
				if(logger.isInfoEnabled()){
					logger.info("skip over path: {pattern="+pattern+",requestPath="+requestPath+"}");
				}
				chain.doFilter(request, response);
				return;
			}
		}
		
		try{
			VirtualFilterChain vfc = new VirtualFilterChain(chain,requestPath);
			vfc.doFilter(request, response);
		}catch(PuzzleException e){ //unified exception handler
			logger.error(e.getMessage(),e);
			e.handle((HttpServletRequest)request, (HttpServletResponse)response);
		}
		catch(Throwable e){
			logger.error(e.getMessage(),e);
			new PuzzleException(e).handle((HttpServletRequest)request, (HttpServletResponse)response);
		}
	}
	
	final public void destroy() {
		PuzzleFilter.unregisterFilterDelegate();
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
			if(logger.isInfoEnabled()){
				logger.info("framework.intercept.none.patterns: "+patterns);
			}
			iInterceptPatternsNone= patterns.split(",");
		}
	}
}
