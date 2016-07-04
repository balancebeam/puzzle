package io.pdf.core.spring;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.storage.url.BundleURLConnection;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

public class PluginResourcePatternResolver extends ServletContextResourcePatternResolver{

	public PluginResourcePatternResolver(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}
	
	protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
		ClassLoader cl = getClassLoader();
		if(cl instanceof ModuleClassLoader){
			Set<Resource> result = new LinkedHashSet<Resource>(16);
			Enumeration<URL> resourceUrls= ((ModuleClassLoader)cl).findLocalResources(path);
			if(null!= resourceUrls){
				while (resourceUrls.hasMoreElements()) {
					URL url = resourceUrls.nextElement();
					result.add(convertClassLoaderURL(url));
				}
			}
			return result;
		}
		return super.doFindAllClassPathResources(path);
	}
	
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {
		URL url = rootDirResource.getURL();
		if("bundleresource".equals(url.getProtocol())){
			BundleURLConnection con = (BundleURLConnection)rootDirResource.getURL().openConnection();
			File rootDir = new File(con.getFileURL().getPath());
			return doFindMatchingFileSystemResources(rootDir, subPattern);
		}
		return super.doFindPathMatchingFileResources(rootDirResource, subPattern);
	}
}
