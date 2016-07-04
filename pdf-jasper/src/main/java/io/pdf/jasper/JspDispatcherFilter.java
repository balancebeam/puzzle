package io.pdf.jasper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jasper.Constants;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.security.SecurityUtil;
import org.apache.jasper.servlet.JspServletWrapper;

import io.pdf.core.aware.ElementPostfixAware;
import io.pdf.core.common.PluginRequestUtil;
import io.pdf.core.hook.wrapper.ResourceWrapper;
import io.pdf.core.servlet.PluginServletConfig;
import io.pdf.jasper.internal.PluginJspServletContext;
import io.pdf.jasper.internal.PluginJspServletWrapper;
import io.pdf.jasper.internal.PluginServletOptions;

/**
 * 处理组件中的jsp
 * @author yangzz
 *
 */
public class JspDispatcherFilter implements Filter,ElementPostfixAware{
	
	private static JspDispatcherFilter instance;
	
	private Log logger = LogFactory.getLog(JspDispatcherFilter.class);
	
	private ServletContext servletContext;
	
	private ServletConfig servletConfig;
	
	private Options options;
	
	private JspRuntimeContext rctxt;
	
	public static void clearComponentJSP(String componentName){
		if(instance!=null){
			instance.clearVisitedJsps(componentName);
		}
	}
	
	public void init(FilterConfig filterConfig) throws ServletException {
		synchronized(this){
			instance=this;
		}
		
		servletContext= new PluginJspServletContext(null);
		servletConfig= new PluginServletConfig("jasper",servletContext,new Hashtable<String,String>());
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		// 解决类似于Weblogic的应用ClassLoader不是URLClassLoader的问题
		if (!(original instanceof URLClassLoader)) {
			Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[] {}, original));
		}
		
		String engineOptionsName = filterConfig.getInitParameter("engineOptionsClass");
		if (engineOptionsName != null) {
			try {
				ClassLoader loader = Thread.currentThread().getContextClassLoader();
				Class<?> engineOptionsClass = loader.loadClass(engineOptionsName);
				Class<?>[] ctorSig = { ServletConfig.class, ServletContext.class };
				Constructor<?> ctor = engineOptionsClass.getConstructor(ctorSig);
				Object[] args = { servletConfig, servletContext};
				this.options = ((Options) ctor.newInstance(args));
			} catch (Throwable e) {
				if(logger.isWarnEnabled()){
					logger.warn("Failed to load engineOptionsClass", e);
				}
				this.options = new PluginServletOptions(filterConfig, servletContext);
			}
		} else {
			this.options = new PluginServletOptions(filterConfig, servletContext);
		}
		//构建JSP编译的上下文
		this.rctxt = new JspRuntimeContext(this.servletContext, this.options);
		
		if (logger.isDebugEnabled()) {
			logger.debug(Localizer.getMessage("jsp.message.scratch.dir.is",
					this.options.getScratchDir().toString()));

			logger.debug(Localizer
					.getMessage("jsp.message.dont.modify.servlets"));
		}
		Thread.currentThread().setContextClassLoader(original);
	}
	
	public int getJspCount() {
		return this.rctxt.getJspCount();
	}

	public void setJspReloadCount(int count) {
		this.rctxt.setJspReloadCount(count);
	}

	public int getJspReloadCount() {
		return this.rctxt.getJspReloadCount();
	}
	//编译过访问过的JSP
	private Map<String,Set<String>> visitedJsps = new HashMap<String,Set<String>>();
	
	private void clearVisitedJsps(String componentName){
		if(visitedJsps.containsKey(componentName)){
			for(String jspUri : visitedJsps.get(componentName)){
				this.rctxt.removeWrapper(jspUri);
			}
			visitedJsps.remove(componentName);
		}
	}
	
	boolean preCompile(HttpServletRequest request) throws ServletException {
		String queryString = request.getQueryString();
		if (queryString == null) {
			return false;
		}
		int start = queryString.indexOf(Constants.PRECOMPILE);
		if (start < 0) {
			return false;
		}
		queryString = queryString.substring(start
				+ Constants.PRECOMPILE.length());

		if (queryString.length() == 0) {
			return true;
		}
		if (queryString.startsWith("&")) {
			return true;
		}
		if (!queryString.startsWith("=")) {
			return false;
		}
		int limit = queryString.length();
		int ampersand = queryString.indexOf("&");
		if (ampersand > 0) {
			limit = ampersand;
		}
		String value = queryString.substring(1, limit);
		if (value.equals("true"))
			return true;
		if (value.equals("false")) {
			return true;
		}
		throw new ServletException("Cannot have request parameter "
				+ Constants.PRECOMPILE + " set to " + value);
	}

	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest request= (HttpServletRequest)req;
		HttpServletResponse response= (HttpServletResponse)res;
		
		String jspUri = null;

		String jspFile = (String) request.getAttribute(Constants.JSP_FILE);
		if (jspFile != null) {
			jspUri = jspFile;
		} else {
			jspUri = (String) request	.getAttribute("javax.servlet.include.servlet_path");
			if (jspUri != null) {
				String pathInfo = (String) request.getAttribute("javax.servlet.include.path_info");
				if (pathInfo != null) {
					jspUri = jspUri + pathInfo;
				}
			} else {
				jspUri = PluginRequestUtil.getRequestPath(request);		
			}
		}
		ResourceWrapper wrapper= PluginRequestUtil.getPluginResourceWrapper(jspUri);
		if(wrapper==null){
			chain.doFilter(req, res);
			return;
		}
		//编译之前检查传递的参数
		boolean precompile = preCompile(request);
		serviceJspFile(request, response, jspUri,wrapper, precompile);
		
	}
	
	private void serviceJspFile(HttpServletRequest request,
			HttpServletResponse response, final String jspUri,
			ResourceWrapper pluginResourceWrapper, boolean precompile) throws ServletException,
			IOException {
		if(logger.isDebugEnabled()){
			logger.debug("请求JSP的详细信息为："+pluginResourceWrapper);
		}
		JspServletWrapper wrapper = this.rctxt.getWrapper(jspUri);
		
		// 访问Bundle中的jsp
		final ClassLoader original = Thread.currentThread().getContextClassLoader();
		if (wrapper == null) {
			synchronized (this) {
				wrapper = this.rctxt.getWrapper(jspUri);
				if (wrapper == null) {
					wrapper = new PluginJspServletWrapper(this.servletConfig,this.options, jspUri, pluginResourceWrapper, this.rctxt);
					String componentName= pluginResourceWrapper.getPluginContext().getName();
					if(!visitedJsps.containsKey(componentName)){
						visitedJsps.put(componentName, new HashSet<String>());
					}
					visitedJsps.get(componentName).add(jspUri);
					this.rctxt.addWrapper(jspUri, wrapper);
				}
			}
		}

		try {
			wrapper.service(request, response, precompile);
			if(logger.isDebugEnabled()){
				logger.debug("成功请求JSP："+pluginResourceWrapper);
			}
		} catch (FileNotFoundException fnfe) {
			logger.error(fnfe.getMessage(), fnfe);
			handleMissingResource(request, response, jspUri);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}
	
	private void handleMissingResource(HttpServletRequest request,
			HttpServletResponse response, String jspUri)
			throws ServletException, IOException {
		String includeRequestUri = (String) request.getAttribute("javax.servlet.include.request_uri");

		if (includeRequestUri != null) {
			String msg = Localizer.getMessage("jsp.error.file.not.found",jspUri);
			throw new ServletException(SecurityUtil.filter(msg));
		}
		try {
			throw new ServletException("JSP not found : "+jspUri);
		} catch (IllegalStateException ise) {
			logger.error(Localizer.getMessage("jsp.error.file.not.found",	jspUri));
		}
	}
	
	public JspRuntimeContext getJspRuntimeContext(){
		return this.rctxt;
	}

	public void destroy() {
		
	}
}
