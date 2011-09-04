package se.l4.dust.jaxrs.internal;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Path;

import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;
import org.scannotation.WarUrlFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.dust.api.Context;
import se.l4.dust.api.NamespaceManager;
import se.l4.dust.api.TemplateException;
import se.l4.dust.api.TemplateManager;
import se.l4.dust.api.annotation.Component;
import se.l4.dust.api.annotation.Template;
import se.l4.dust.api.discovery.ClassDiscovery;
import se.l4.dust.api.discovery.DiscoveryFactory;
import se.l4.dust.api.resource.variant.ResourceVariantManager;
import se.l4.dust.api.template.TemplateCache;
import se.l4.dust.jaxrs.PageManager;

import com.google.inject.Inject;
import com.google.inject.Stage;

/**
 * Module that performs a single contribution scanning the classpath for
 * classes that are either pages or components. It then checks if they are
 * within a registered package and if so registers them automatically.
 * 
 * @author Andreas Holstenson
 *
 */
public class PageDiscovery
{
	private static final Logger logger = LoggerFactory.getLogger(PageDiscovery.class);
	private final NamespaceManager manager;
	private final PageManager pages;
	private final TemplateManager components;
	private final TemplateCache templateCache;
	private final ResourceVariantManager variants;
	private final Stage stage;
	private final DiscoveryFactory discovery;
	
	@Inject
	public PageDiscovery(
			NamespaceManager manager,
			PageManager pages,
			TemplateManager components,
			TemplateCache templateCache,
			ResourceVariantManager variants,
			Stage stage,
			DiscoveryFactory discovery)
	{
		this.manager = manager;
		this.pages = pages;
		this.components = components;
		this.templateCache = templateCache;
		this.variants = variants;
		this.stage = stage;
		this.discovery = discovery;
	}

	public void discover(final ServletContext ctx)
		throws Exception
	{
		logger.info("Attempting to discover classes within registered namespaces");
		
		ClassDiscoveryImpl cd = new ClassDiscoveryImpl(ctx);
		cd.index();
		discovery.addTopLevel(cd);
		
		Map<String, Set<String>> index = cd.index;
		int c = handleComponents(index);
		int p = handlePages(index);
		
		logger.info("Found " + p + " pages and " + c + " components");
		
		if(stage == Stage.PRODUCTION)
		{
			int t = handleTemplate(index);
			logger.info("Loaded " + t + " templates");
		}
	}
	
	private static List<URL> findClasspath()
		throws IOException
	{
		Enumeration<URL> enumeration = Thread.currentThread()
			.getContextClassLoader()
			.getResources("META-INF/MANIFEST.MF");
		
		List<URL> urls = new LinkedList<URL>();
		
		while(enumeration.hasMoreElements())
		{
			URL u = enumeration.nextElement();
			if(u.getProtocol().equals("jar"))
			{
				String url = u.toString();
				int idx = url.indexOf('!');
				String newUrl = url.substring(4, idx);
				urls.add(new URL(newUrl));
			}
		}
		
		return urls;
	}
	
	/**
	 * Handle all pages found (everything annotated with {@link Path}).
	 * 
	 * @param manager
	 * @param pages
	 * @param s
	 * @return
	 * @throws Exception
	 */
	private int handlePages(Map<String, Set<String>> s)
		throws Exception
	{
		int count = 0;
		Set<String> classes = s.get(Path.class.getName());
		if(classes != null)
		{
			for(String className : classes)
			{
				NamespaceManager.Namespace ns = findNamespace(className);
				if(ns != null)
				{
					// This class is handled so we register it
					pages.add(Class.forName(className));
					count++;
				}
			}
		}
		
		return count;
	}
	
	/**
	 * Handle all components (annotated with {@link Component}).
	 * 
	 * @param manager
	 * @param pages
	 * @param components
	 * @param s
	 * @return
	 * @throws Exception
	 */
	private int handleComponents(Map<String, Set<String>> s)
		throws Exception
	{
		int count = 0;
		Set<String> classes = s.get(Component.class.getName());
		if(classes != null)
		{
			for(String className : classes)
			{
				NamespaceManager.Namespace ns = findNamespace(className);
				if(ns != null)
				{
					Class<?> component = Class.forName(className);
					
					// This class is handled so we register it
					components.getNamespace(ns.getUri())
						.addComponent(component);
					count++;

					if(stage == Stage.PRODUCTION)
					{
						try
						{
							for(Context ctx : variants.getInitialContexts())
							{
								templateCache.getTemplate(ctx, component, (Template) null);
							}
						}
						catch(TemplateException e)
						{
						}
					}
				}
			}
		}
		
		return count;
	}
	
	private int handleTemplate(Map<String, Set<String>> index)
		throws Exception
	{
		int count = 0;
		Set<String> classes = index.get(Template.class.getName());
		if(classes != null)
		{
			for(String className : classes)
			{
				NamespaceManager.Namespace ns = findNamespace(className);
				if(ns != null)
				{
					Class<?> c = Class.forName(className);
					
					for(Context ctx : variants.getInitialContexts())
					{
						templateCache.getTemplate(ctx, c, c.getAnnotation(Template.class));
					}
					count++;
				}
			}
		}
		return count;
	}
	
	/**
	 * Try to find a registered namespace for a given class name by starting
	 * with its package and slowly reducing it downwards until either a match
	 * can be found or no more segments are available in the package.
	 * 
	 * <p>
	 * For example if we have the class {@code org.example.deep.pkg.TestClass}
	 * and a namespace registered for {@code org.example} the search would be
	 * {@code org.example.deep.pkg}, {@code org.example.deep} and finally
	 * {@code org.example}.
	 *  
	 * @param pages
	 * @param className
	 * @return
	 */
	private NamespaceManager.Namespace findNamespace(String className)
	{
		int idx = className.lastIndexOf('.');
		while(idx > 0)
		{
			className = className.substring(0, idx);
			
			NamespaceManager.Namespace ns = manager.getBinding(className);
			if(ns != null)
			{
				return ns;
			}
			
			idx = className.lastIndexOf('.');
		}
		
		return null;
	}
	
	private static class ClassDiscoveryImpl
		implements ClassDiscovery
	{
		private final ServletContext ctx;
		
		private AnnotationDB db;

		private Map<String, Set<String>> index;
		
		public ClassDiscoveryImpl(ServletContext ctx)
		{
			this.ctx = ctx;
		}
		
		public synchronized void index()
		{
			AnnotationDB db = new AnnotationDB();
			db.setScanClassAnnotations(true);
			db.setScanFieldAnnotations(false);
			db.setScanMethodAnnotations(false);
			db.setScanParameterAnnotations(false);
			
			try
			{
				Set<URL> urls = new HashSet<URL>();
				for(URL url : ClasspathUrlFinder.findClassPaths())
				{
					urls.add(url);
				}
				
				for(URL url : WarUrlFinder.findWebInfLibClasspaths(ctx))
				{
					urls.add(url);
				}
				
				urls.addAll(findClasspath());
				
				db.scanArchives(urls.toArray(new URL[urls.size()]));
				
				index = db.getAnnotationIndex();
			}
			catch(IOException e)
			{
				
			}
		}
		
		public Set<String> getAnnotatedWith(Class<? extends Annotation> annotation)
		{
			Set<String> result = index.get(annotation.getName());
			return result == null ? Collections.<String>emptySet() : result;
		}
	};
}
