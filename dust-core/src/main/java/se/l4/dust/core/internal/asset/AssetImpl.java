package se.l4.dust.core.internal.asset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.core.UriBuilder;

import org.jdom.Namespace;

import se.l4.dust.api.NamespaceManager;
import se.l4.dust.api.asset.Asset;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class AssetImpl
	implements Asset
{
	private final Namespace ns;
	private final String name;
	private final URL url;
	private final String checksum;
	private final URI uri;
	
	public AssetImpl(NamespaceManager manager, Namespace ns, String name, URL url)
	{
		this.ns = ns;
		this.name = name;
		this.url = url;
		
		String checksum = "";
		URI uri = null;
		if(ns != null)
		{
			checksum = getChecksum(url);
			
			String prefix = manager.getNamespaceByURI(ns.getURI())
				.getPrefix();
			
			uri = UriBuilder.fromPath("/asset/{ns}/{checksum}/{name}")
					.build(prefix, checksum, name);
		}
		
		this.checksum = checksum;
		this.uri = uri;
	}
	
	private String getChecksum(URL url)
	{
		try
		{
			byte[] digest = digest(url);
			return Base64.encode(digest);
		}
		catch(NoSuchAlgorithmException e)
		{
		}
		catch(DigestException e)
		{
		}
		catch(IOException e)
		{
		}
		
		throw new RuntimeException("Unable to access asset located at " + url);
	}
	
	private byte[] digest(URL url)
		throws IOException, NoSuchAlgorithmException, DigestException
	{
		InputStream stream = url.openStream();
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] buf = new byte[1024];
			int len;
			while((len = stream.read(buf)) != -1)
			{
				digest.update(buf, 0, len);
			}
			
			return digest.digest();
		}
		finally
		{
			stream.close();
		}
	}
	
	public String getChecksum()
	{
		return checksum;
	}

	public String getName()
	{
		return name;
	}

	public Namespace getNamespace()
	{
		return ns;
	}

	public URL getURL()
	{
		return url;
	}

	@Override
	public String toString()
	{
		return uri.toString();
	}
}
