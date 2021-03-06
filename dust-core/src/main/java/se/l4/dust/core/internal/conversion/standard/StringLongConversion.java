package se.l4.dust.core.internal.conversion.standard;

import se.l4.dust.api.conversion.ConversionException;
import se.l4.dust.api.conversion.NonGenericConversion;

public class StringLongConversion
	implements NonGenericConversion<String, Long>
{

	@Override
	public Long convert(String in)
	{
		if(in == null)
		{
			return 0l;
		}

		try
		{
			return Long.parseLong(in);
		}
		catch(NumberFormatException e)
		{
			throw new ConversionException("Can not convert input to long; "
				+ e.getMessage(), e);
		}
	}

	@Override
	public Class<String> getInput()
	{
		return String.class;
	}

	@Override
	public Class<Long> getOutput()
	{
		return Long.class;
	}

}
