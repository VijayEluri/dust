package se.l4.dust.core.internal.template;

import java.io.IOException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import se.l4.dust.api.Context;
import se.l4.dust.api.DefaultContext;
import se.l4.dust.api.Namespace;
import se.l4.dust.api.discovery.DiscoveryEncounter;
import se.l4.dust.api.discovery.DiscoveryHandler;
import se.l4.dust.api.template.Component;
import se.l4.dust.api.template.Template;
import se.l4.dust.api.template.TemplateCache;
import se.l4.dust.api.template.TemplateException;
import se.l4.dust.api.template.fragment.TemplateFragment;

public class TemplatePreloadingHandler
	implements DiscoveryHandler
{
	private static final Logger logger = LoggerFactory.getLogger(DiscoveryHandler.class);

	private final TemplateCache cache;

	@Inject
	public TemplatePreloadingHandler(TemplateCache cache)
	{
		this.cache = cache;
	}

	@Override
	public void handle(Namespace ns, DiscoveryEncounter encounter)
	{
		Context context = new DefaultContext();

		try
		{
			Collection<Class<?>> components = encounter.getAnnotatedWith(Component.class);
			for(Class<?> c : components)
			{
				if(! TemplateFragment.class.isAssignableFrom(c))
				{
					cache.getTemplate(context, c);
				}
			}

			Collection<Class<?>> templates = encounter.getAnnotatedWith(Template.class);
			for(Class<?> c : templates)
			{
				cache.getTemplate(context, c);
			}

			logger.info("{}: Loaded {} templates", ns.getUri(), components.size() + templates.size());
		}
		catch(IOException e)
		{
			throw new TemplateException("IO problem; " + e.getMessage(), e);
		}
	}
}
