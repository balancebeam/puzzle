package io.pdf.jasper.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.AnnotationProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.servlet.JspServletWrapper;
import org.springframework.util.ReflectionUtils;

import io.pdf.bridge.plugin.PluginFramework;
import io.pdf.core.PluginContext;
import io.pdf.core.hook.wrapper.ResourceWrapper;
import io.pdf.core.servlet.wrapper.HttpRequestWrapper;


@SuppressWarnings("deprecation")
public class PluginJspServletWrapper extends JspServletWrapper {
	
	private static Log logger = LogFactory.getLog(PluginJspServletWrapper.class);
	
	private ResourceWrapper pluginResourceWrapper;
	
	public PluginJspServletWrapper(ServletConfig servletConfig, Options options,
			String jspUri, ResourceWrapper pluginResourceWrapper, JspRuntimeContext rctxt)
			throws JasperException {
		super(servletConfig, options, jspUri, false, rctxt);
		this.pluginResourceWrapper= pluginResourceWrapper;
	}

	private PluginContext pluginContext = null;
	
	private long available = 0L;

	public void service(HttpServletRequest request,
			HttpServletResponse response,
			boolean precompile)
			throws ServletException, IOException,
			FileNotFoundException {	
		
		Options options = (Options) getValue("options");
		String jspUri = (String) getValue("jspUri");
		Servlet servlet;
		
		try {
			JspCompilationContext ctxt = this.getJspEngineContext();
			
			if (ctxt.isRemoved()) {
				throw new FileNotFoundException(jspUri);
			}

			if ((available > 0L) && (available < Long.MAX_VALUE)) {
				if (available > System.currentTimeMillis()) {
					response.setDateHeader("Retry-After", available);
					response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,Localizer.getMessage("jsp.error.unavailable"));
					return;
				} else {
					// Wait period has expired. Reset.
					available = 0;
				}
			}
			/*
			 * (1) Compile
			 */
			boolean firstTime = (Boolean) getValue("firstTime");
			if (options.getDevelopment() || firstTime) {
				synchronized (this) {
					// firstTime = false;
					setValue("firstTime", false);
					// The following sets reload to
					// true, if
					// necessary
					// 设置JSP编译器的ClassLoader
					pluginContext= pluginResourceWrapper.getPluginContext();
					final ClassLoader pluginClassLoader =pluginContext.getClassLoader();
					//jasper参与其他插件的jsp编译工作，因为会使用到org.apache.AnnotationProcessor类，此类在jasper里不在工程中
					final ClassLoader jasperClassLoader= getClass().getClassLoader();
					ctxt.setClassLoader(new URLClassLoader(new URL[] {},pluginClassLoader){
						@Override
						public Class<?> loadClass(String name)throws ClassNotFoundException {
							try{
								return jasperClassLoader.loadClass(name);
							}catch(ClassNotFoundException e){
								return pluginClassLoader.loadClass(name);
							}
						}

						@Override
						public URL getResource(String name) {
							//先要用jasper的类加载器去加载资源org.apache.jasper.compiler.*，否则其他组件会加载容器中的类
							URL url= jasperClassLoader.getResource(name);
							if(url== null){
								url= pluginClassLoader.getResource(name);
							}
							return url;
						}
					});
				}
				ctxt.compile();
			} else {
				JasperException compileException = (JasperException) getValue("compileException");
				if (compileException != null) {
					// Throw cached compilation
					// exception
					throw compileException;
				}
			}

			/*
			 * (2) (Re)load servlet class file
			 */
			servlet = getServlet();

			// If a page is to be precompiled only,
			// return.
			if (precompile) {
				return;
			}

		} catch (ServletException ex) {
			if (options.getDevelopment()) {
				throw handleJspException(ex);
			} else {
				throw ex;
			}
		} catch (FileNotFoundException fnfe) {
			// File has been removed. Let caller handle
			// this.
			throw fnfe;
		} catch (IOException ex) {
			if (options.getDevelopment()) {
				throw handleJspException(ex);
			} else {
				throw ex;
			}
		} catch (IllegalStateException ex) {
			if (options.getDevelopment()) {
				throw handleJspException(ex);
			} else {
				throw ex;
			}
		} catch (Exception ex) {
			if (options.getDevelopment()) {
				throw handleJspException(ex);
			} else {
				throw new JasperException(ex);
			}
		}

		try {
			Thread.currentThread().setContextClassLoader(PluginFramework.getFrameworkContextClassLoader());
			/*
			 * (3) Service request
			 */
			HttpRequestWrapper requestWrapper= new HttpRequestWrapper(request,pluginResourceWrapper.getServletContext());
			if (servlet instanceof SingleThreadModel) {
				// sync on the wrapper so that the
				// freshness
				// of the page is determined right
				// before
				// servicing
				synchronized (this) {
					servlet.service(requestWrapper,response);
				}
			} else {
				servlet.service(requestWrapper, response);
			}

		} catch (UnavailableException ex) {
			String includeRequestUri = (String) request
					.getAttribute("javax.servlet.include.request_uri");
			if (includeRequestUri != null) {
				// This file was included. Throw an
				// exception as
				// a response.sendError() will be
				// ignored by
				// the
				// servlet engine.
				throw ex;
			} else {
				int unavailableSeconds = ex
						.getUnavailableSeconds();
				if (unavailableSeconds <= 0) {
					unavailableSeconds = 60; // Arbitrary
												// default
				}
				available = System.currentTimeMillis()+ (unavailableSeconds * 1000L);
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,ex.getMessage());
			}
		} catch (ServletException ex) {
			if (options.getDevelopment()) {
				throw handleJspException(ex);
			} else {
				throw ex;
			}
		} catch (IOException ex) {
			if (options.getDevelopment()) {
				throw handleJspException(ex);
			} else {
				throw ex;
			}
		} catch (IllegalStateException ex) {
			if (options.getDevelopment()) {
				throw handleJspException(ex);
			} else {
				throw ex;
			}
		} catch (Exception ex) {
			if (options.getDevelopment()) {
				throw handleJspException(ex);
			} else {
				throw new JasperException(ex);
			}
		}
	}
	private Class<?> servletClass;
	private Servlet theServlet;
	private ServletConfig config;
	public Servlet getServlet() throws ServletException, IOException, FileNotFoundException
	    {
	        // DCL on 'reload' requires that 'reload' be volatile
	        // (this also forces a read memory barrier, ensuring the 
	        // new servlet object is read consistently)
			boolean reload = (Boolean) getValue("reload");
	        if (reload) {
	            synchronized (this) {
	                // Synchronizing on jsw enables simultaneous loading
	                // of different pages, but not the same page.
	                if (reload) {
	                    // This is to maintain the original protocol.
	                    destroy();
	                    
	                    final Servlet servlet;
	                    
	                    try {
	                        servletClass = getJspEngineContext().load();
	                        servlet = (Servlet) servletClass.newInstance();
	                        config= pluginContext.getServletConfig();
	                        
	                        AnnotationProcessor annotationProcessor = (AnnotationProcessor) config.getServletContext().getAttribute(AnnotationProcessor.class.getName());
	                        if (annotationProcessor != null) {
	                           annotationProcessor.processAnnotations(servlet);
	                           annotationProcessor.postConstruct(servlet);
	                        }
	                    } catch (IllegalAccessException e) {
	                        throw new JasperException(e);
	                    } catch (InstantiationException e) {
	                        throw new JasperException(e);
	                    } catch (Exception e) {
	                        throw new JasperException(e);
	                    }
	                    
	                    servlet.init(config);

	                    if (!(Boolean) getValue("firstTime")) {
	                    	getJspEngineContext().getRuntimeContext().incrementJspReloadCount();
	                    }

	                    theServlet = servlet;
	                    setValue("reload",false);
//	                    reload = false;
	                    // Volatile 'reload' forces in order write of 'theServlet' and new servlet object
	                }
	            }    
	        }
	        return theServlet;
	    }
	
	public void destroy() {
        if (theServlet != null) {
            theServlet.destroy();
            AnnotationProcessor annotationProcessor = (AnnotationProcessor) config.getServletContext().getAttribute(AnnotationProcessor.class.getName());
            if (annotationProcessor != null) {
                try {
                    annotationProcessor.preDestroy(theServlet);
                } catch (Exception e) {
                	logger.error(e.getMessage(),e);
//                    // Log any exception, since it can't be passed along
//                    log.error(Localizer.getMessage("jsp.error.file.not.found",
//                           e.getMessage()), e);
                }
            }
        }
    }

	final private Object getValue(String name) {
		Field f= ReflectionUtils.findField(JspServletWrapper.class, name);
		ReflectionUtils.makeAccessible(f);
		return ReflectionUtils.getField(f, this);
	}

	final private void setValue(String name,Object value) {
		Field f= ReflectionUtils.findField(JspServletWrapper.class, name);
		ReflectionUtils.makeAccessible(f);
		ReflectionUtils.setField(f, this,value);
	}
}
