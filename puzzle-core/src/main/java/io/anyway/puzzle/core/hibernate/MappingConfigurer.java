package io.anyway.puzzle.core.hibernate;

import io.anyway.puzzle.bridge.hibernate.FrameworkSessionFactoryBean;
import io.anyway.puzzle.core.spring.PluginXmlWebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.storage.url.BundleURLConnection;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class MappingConfigurer  implements ApplicationContextAware,InitializingBean, DisposableBean{
	
	private String[] packagesToScan;
	
	private Resource[] mappingLocations; 
	
	private PluginXmlWebApplicationContext applicationContext;
	
	private List<Class<?>> addedClassByScan= new ArrayList<Class<?>>();
	
	private List<String> addedClassNameByXml= new ArrayList<String>();
	
	private List<String> addedClasses= new ArrayList<String>();
	
	@SuppressWarnings("deprecation")
	private TypeFilter[] entityTypeFilters = new TypeFilter[] {
			new AnnotationTypeFilter(Entity.class, false),
			new AnnotationTypeFilter(Embeddable.class, false),
			new AnnotationTypeFilter(MappedSuperclass.class, false),
			new AnnotationTypeFilter(org.hibernate.annotations.Entity.class, false)};
	
	public void afterPropertiesSet() throws Exception {
		ResourcePatternResolver resourcePatternResolver = applicationContext.getResourcePatternResolver();
		FrameworkSessionFactoryBean sf = (FrameworkSessionFactoryBean)this.applicationContext.getBean("&sessionFactory");
		
		if(null!= this.mappingLocations){
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(new EntityResolver(){
                   public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException{
                       return new InputSource(new String(""));   
                    }
             });
			for(Resource resource: mappingLocations){
				Document doc = builder.parse(resource.getInputStream());
				NodeList classes = doc.getElementsByTagName("class");
				for (int i= 0; i< classes.getLength(); i++) {
					Element element = (Element) classes.item(i);				
					String className = element.getAttribute("name");
					sf.getEntityClassLoader().addClassloader4ClassName(className, applicationContext.getClassLoader());
					addedClassNameByXml.add(className);
				}
			}
			sf.setMappingLocations(mappingLocations);
		}
		
		if(null!= this.packagesToScan){
			for(String pkg : packagesToScan) {
				ModuleClassLoader classLoader = (ModuleClassLoader)applicationContext.getClassLoader();
				String pattern =  ClassUtils.convertClassNameToResourcePath(pkg);
				URL url = classLoader.findLocalResource(pattern);
				if(null== url){
					continue;
				}
				MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
				if("bundleresource".equals(url.getProtocol())){
					BundleURLConnection conn = (BundleURLConnection)url.openConnection();
					ClassLoader classloader= applicationContext.getClassLoader();
					
					List<File> files= collectClassFile(new File(conn.getFileURL().getPath()));
					for(File file: files){
						Resource resource = new UrlResource(file.toURI().toURL());
						if (resource.isReadable()) {
							MetadataReader reader = readerFactory.getMetadataReader(resource);
							String className = reader.getClassMetadata().getClassName();
							if (matchesFilter(reader, readerFactory)) {
								sf.getEntityClassLoader().addClassloader4ClassName(className, classloader);
								addedClassByScan.add(classloader.loadClass(className));
								//primary key
								IdClass id= classloader.loadClass(className).getAnnotation(IdClass.class);
								if(null!= id){
									className= id.value().getName();
									addedClasses.add(className);
									sf.getEntityClassLoader().addClassloader4ClassName(className, classloader);
								}
							}
						}
					}
				}
			}
			sf.setAnnotatedClasses(addedClassByScan.toArray(new Class<?>[addedClassByScan.size()]));
		}
		
		try{
			sf.refresh();
		}catch(Exception e){
			destroy();
			throw e;
		}
	}
	
	private List<File> collectClassFile(File file){
		List<File> result= new ArrayList<File>();
		if(file.exists()){
			if(file.isDirectory()){
				File[] files= file.listFiles();
				if(null!= files){
					for(File f: files){
						result.addAll(collectClassFile(f));
					}
				}
			}
			else if(file.getName().endsWith(".class")){
				result.add(file);
			}
		}
		return result;
	}

	public void destroy() throws Exception {
		if(addedClassByScan== null){
			return;
		}
		FrameworkSessionFactoryBean sf = (FrameworkSessionFactoryBean)this.applicationContext.getBean("&sessionFactory");
		if(null!= mappingLocations){
			sf.removeMappingLocations(mappingLocations);
			for(String className: addedClassNameByXml){
				sf.getEntityClassLoader().removeClassName(className);
			}
		}
		if(null!= this.packagesToScan){
			sf.removeAnnotatedClasses(addedClassByScan.toArray(new Class<?>[addedClassByScan.size()]));
			for(Class<?> clazz: addedClassByScan){
				sf.getEntityClassLoader().removeClassName(clazz.getName());
			}
		}
		for(String className: addedClasses){
			sf.getEntityClassLoader().removeClassName(className);
		}
		addedClassNameByXml= null;
		addedClassByScan= null;
		addedClasses= null;
		sf.refresh();
	}
	
	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	public void setPackagesToScan(String[] packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	public Resource[] getMappingLocations() {
		return mappingLocations;
	}

	public void setMappingLocations(Resource[] mappingLocations) {
		this.mappingLocations = mappingLocations;
	}

	public void setApplicationContext(ApplicationContext applicationContext)throws BeansException {
		this.applicationContext =(PluginXmlWebApplicationContext) applicationContext;
	}
	
	private boolean matchesFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
		if (this.entityTypeFilters != null) {
			for (TypeFilter filter: this.entityTypeFilters) {
				if (filter.match(reader, readerFactory)) {
					return true;
				}
			}
		}
		return false;
	}
}
