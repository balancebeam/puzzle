package io.anyway.puzzle.core.internal;

import java.lang.reflect.Field;

import io.anyway.puzzle.core.common.Constants;
import io.anyway.puzzle.core.common.PluginLifeCycleManager;
import io.anyway.puzzle.core.event.PluginFrameworkListener;
import io.anyway.puzzle.core.hook.PluginHookFilter;
import io.anyway.puzzle.core.hook.PluginHookListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.io.Resources;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.anyway.puzzle.bridge.ext.ClassLoaderAppender;
import io.anyway.puzzle.bridge.plugin.PluginFramework;
import io.anyway.puzzle.core.PluginContext;
import io.anyway.puzzle.core.servlet.PluginServletManager;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;

public class PluginActivator implements BundleActivator,SynchronousBundleListener,FrameworkListener{
	
	private Log logger = LogFactory.getLog(PluginActivator.class);
	
	private PluginHookListener hookListener;
	
	private PluginHookFilter hookFilter;
	
	private PluginServletManager servletManager;
	
	private static ThreadLocal<ClassLoader> classLoaderHolder= new ThreadLocal<ClassLoader>();
	
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		try{
		bundleContext.addBundleListener(this);
		bundleContext.addFrameworkListener(this);
		hookListener= new PluginHookListener();
		hookFilter= new PluginHookFilter();
		hookFilter.setApplicationContext(WebApplicationContextUtils.getWebApplicationContext(PluginFramework.getFrameworkServletContext()));
	
		PluginContext ctx= new PluginContextSupport(bundleContext.getBundle());
		servletManager= new PluginServletManager(ctx);
		servletManager.init(bundleContext.getBundle().getResource(Constants.WEB_CONFIG));
		servletManager.newInstance();
		//重载Mybatis的ClassLoader
		overrideMybatisResourcesClassLoaderWrapper();
		
		}catch(Throwable e){
			logger.error("Plugin Activator start error", e);
			throw new Exception(e);
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		bundleContext.removeBundleListener(this);
		bundleContext.addFrameworkListener(this);
		servletManager.destroy();
		hookListener.destroy();
		hookFilter.destroy();
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		Bundle bundle= event.getBundle();
		//如果不是PDF插件
		if(!"true".equals(bundle.getHeaders().get("Bundle-PDF"))){
			return;
		}
		
		PluginContextSupport ctx= (PluginContextSupport)PluginRepository.getInstance().getPluginContext(bundle);
		switch(event.getType()){
			case BundleEvent.INSTALLED:
			case BundleEvent.RESOLVED:
				break;
			case BundleEvent.STARTING:
				PluginRepository.getInstance().addPluginContext(ctx = new PluginContextSupport(bundle));
				logger.info("Plugin ["+ctx.getName()+"] Context has been registered");
				try {
					classLoaderHolder.set(ctx.getClassLoader());
					ctx.setState(Bundle.STARTING);
					PluginLifeCycleManager.start(ctx);
				} catch (Throwable e) {
					ctx.setState(Bundle.STOPPING);
					logger.error("Plugin ["+ctx.getName()+"] started failure",e);
				}
				finally{
					classLoaderHolder.set(null);
				}
				break;
			case BundleEvent.STARTED:
				if(null!= ctx){
					if(ctx.getState()==Bundle.STOPPING){
						try {
							bundle.stop(); 
						} catch (BundleException e) {
							logger.error("Plugin ["+ctx.getName()+"] stopped failure",e);
						}
					}
					else{
						ctx.setState(Bundle.ACTIVE);
					}
				}
				break;
			case BundleEvent.STOPPING:
				if(null!= ctx){
					ctx.setState(Bundle.STOPPING);
					try {
						PluginLifeCycleManager.stop(ctx);
					} catch (Throwable e) {
						logger.error("Plugin ["+ctx.getName()+"] stopped failure",e);
					}
				}
				break;
			case BundleEvent.STOPPED:
				if(ctx!=null){
					ctx.setState(Bundle.RESOLVED);
					PluginRepository.getInstance().removePluginContext(bundle);
					logger.info("Plugin ["+ctx.getName()+"] Context has been unregistered");
				}
				break;
			case BundleEvent.UNINSTALLED:
				break;
		}
	}

	@Override
	public void frameworkEvent(FrameworkEvent event) {
		if(event.getType() ==FrameworkEvent.STARTLEVEL_CHANGED){
			PluginFrameworkListener.Processor.execute(true);
		}
	}
	
	private void overrideMybatisResourcesClassLoaderWrapper() throws Exception {
		ClassPool pool = new ClassPool(true);
		pool.appendClassPath(new LoaderClassPath(Resources.class.getClassLoader()));
		pool.importPackage(ClassLoaderAppender.class.getName());
		CtClass cc = pool.makeClass("org.apache.ibatis.io.PluginClassLoaderWrapper");
		cc.setSuperclass(pool.get("org.apache.ibatis.io.ClassLoaderWrapper"));
		//构造ClassLoaderAssembler属性
		CtField fild = CtField.make("private ClassLoaderAppender appender= null;", cc);
		cc.addField(fild);
		//创建构造方法
		CtConstructor ctor = CtNewConstructor.make(
				"public PluginClassLoaderWrapper(ClassLoaderAppender appender){ "
				+ "super(); "
				+ "this.appender= appender;"
				+ "}", cc);
		cc.addConstructor(ctor);
		//构造方法
		CtMethod mthd = CtNewMethod.make(
				"ClassLoader[] getClassLoaders(ClassLoader classLoader){ "
				+ "return appender.makeup(super.getClassLoaders(classLoader)); "
				+ "}",cc);
		cc.addMethod(mthd);
		//生成Class
		Class<?> newCls = cc.toClass(Resources.class.getClassLoader(), Resources.class.getProtectionDomain());
		//实例化动态类
		Object instance = newCls.getConstructor(ClassLoaderAppender.class).newInstance(new ClassLoaderAppender(){
			@Override
			public ClassLoader[] makeup(ClassLoader[] classloaders) {
				if(classLoaderHolder.get()!= null){
					ClassLoader[] result= new ClassLoader[classloaders.length+1];
					System.arraycopy(classloaders, 0, result, 1, classloaders.length);
					result[0]= classLoaderHolder.get();
					return result;
				}
				return classloaders;
			}
		});
		Field f = ReflectionUtils.findField(Resources.class, "classLoaderWrapper");
		ReflectionUtils.makeAccessible(f);
		ReflectionUtils.setField(f, null, instance);
	}

}
