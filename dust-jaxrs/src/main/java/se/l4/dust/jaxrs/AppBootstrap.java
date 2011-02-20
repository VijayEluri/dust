package se.l4.dust.jaxrs;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import se.l4.crayon.Configurator;
import se.l4.crayon.Environment;
import se.l4.dust.Dust;

import com.google.inject.Injector;

/**
 * Boostrap that delegates to a {@link Configurator}.
 * 
 * @author Andreas Holstenson
 *
 */
public abstract class AppBootstrap
	extends AbstractBootstrap
{
	private Configurator configurator;
	
	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
		configurator.shutdown();
	}
	
	@Override
	protected Injector getInjector(ServletContext sce)
	{
		String productionStr = sce.getInitParameter(Dust.DUST_PRODUCTION);
		boolean production = "true".equalsIgnoreCase(productionStr);
			
		// New context, let's initialize the system
		configurator = new Configurator(production ? Environment.PRODUCTION : Environment.DEVELOPMENT);
	
		initialize(configurator);

		configurator.configure();
		
		return configurator.getInjector();
	}
	
	protected abstract void initialize(Configurator configurator);
}