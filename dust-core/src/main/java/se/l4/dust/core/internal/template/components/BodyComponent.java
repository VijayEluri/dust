package se.l4.dust.core.internal.template.components;

import java.io.IOException;

import se.l4.dust.api.template.RenderingContext;
import se.l4.dust.api.template.dom.Content;
import se.l4.dust.api.template.spi.TemplateOutputStream;
import se.l4.dust.core.internal.template.dom.Emitter;

public class BodyComponent
	extends EmittableComponent
{
	public BodyComponent()
	{
		super("body", BodyComponent.class);
	}
	
	@Override
	public Content copy()
	{
		return new BodyComponent().copyAttributes(this);
	}
	
	@Override
	public void emit(
			Emitter emitter,
			RenderingContext ctx, 
			TemplateOutputStream out,
			Object data,
			EmittableComponent lastComponent,
			Object lastData)
		throws IOException
	{
		if(lastComponent != null)
		{
			Attribute attr = getAttribute("parameter");
			if(attr != null)
			{
				ParameterComponent param = lastComponent.getParameter(attr.getStringValue(ctx, data), false);
				if(param != null)
				{
					for(Content c : param.getRawContents())
					{
						emitter.emit(ctx, out, lastData, this, data, c);
					}
				}
			}
			else
			{
				for(Content c : lastComponent.getRawContents())
				{
					/*
					 * lastData and data are swapped so that expressions run
					 * on the correct object.
					 */
					emitter.emit(ctx, out, lastData, this, data, c);
				}
			}
		}
	}

}
