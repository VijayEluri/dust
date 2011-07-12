package se.l4.dust.js.uglify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.mozilla.javascript.JavaScriptException;

import com.google.inject.Inject;

import se.l4.crayon.Environment;
import se.l4.dust.api.asset.AssetProcessor;
import se.l4.dust.api.resource.MemoryResource;
import se.l4.dust.api.resource.NamedResource;
import se.l4.dust.api.resource.Resource;
import se.l4.dust.js.env.JavascriptEnvironment;

/**
 * Processor that runs UglifyJS on JavaScript files and compresses them. This
 * processor can be used together with asset merging for maximum effect.
 * 
 * @author Andreas Holstenson
 *
 */
public class UglifyProcessor
	implements AssetProcessor
{
	private final Environment env;

	@Inject
	public UglifyProcessor(Environment env)
	{
		this.env = env;
	}

	public Resource process(String namespace, String path, Resource in,
			Object... arguments)
		throws IOException
	{
		Environment minimum = Environment.PRODUCTION;
		if(arguments.length > 0)
		{
			if(arguments[0] instanceof Environment)
			{
				minimum = (Environment) arguments[0];
			}
			else
			{
				throw new RuntimeException("Passed unknown argument to UglifyProcessor");
			}
		}
		
		if(minimum == Environment.PRODUCTION && minimum != env)
		{
			/*
			 * Do nothing if we should only run in production, but we are in
			 * development. 
			 */
			return in;
		}
		
		InputStream stream = in.openStream();
		ByteArrayOutputStream out = new ByteArrayOutputStream(in.getContentLength());
		try
		{
			int len = 0;
			byte[] buf = new byte[1024];
			while((len = stream.read(buf)) != -1)
			{
				out.write(buf, 0, len);
			}
		}
		finally
		{
			stream.close();
		}
		
		String value = new String(out.toByteArray(), in.getContentEncoding() != null ? in.getContentEncoding() : "UTF-8");
		
		try
		{
			Object result = new JavascriptEnvironment()
				.add(UglifyProcessor.class.getResource("parse-js.js"))
				.add(UglifyProcessor.class.getResource("process.js"))
				.add(UglifyProcessor.class.getResource("uglify-js.js"))
				.define("jsSource", value)
				.evaluate("uglify(jsSource, {});");
			
			MemoryResource res = new MemoryResource("text/css", "UTF-8", ((String) result).getBytes("UTF-8"));
			return new NamedResource(res, path);
		}
		catch(JavaScriptException e)
		{
			throw new IOException(e);
		}
	}

}