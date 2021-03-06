package se.l4.dust.core.internal.expression.ast;

/**
 * Node for division between a left and a right part.
 *
 * @author Andreas Holstenson
 *
 */
public class DivideNode
	extends LeftRightNode
{

	public DivideNode(int line, int position, Node left, Node right)
	{
		super(line, position, left, right);
	}

}
