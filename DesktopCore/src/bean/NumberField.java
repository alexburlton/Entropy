package bean;

import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;

/**
 * Text field to contain integer values
 */
public class NumberField extends JFormattedTextField
{
	public NumberField()
	{
		this(null, null);
	}
	public NumberField(Integer min)
	{
		this(min, null);
	}
	public NumberField(Integer min, Integer max)
	{
		super(new NumberFormatter());
		
		NumberFormatter nf = (NumberFormatter)getFormatter();
		
		if (min != null)
		{
			nf.setMinimum(min);
		}
		
		if (max != null)
		{
			nf.setMaximum(max);
		}
	}
	
	public int getNumber()
	{
		Object val = getValue();
		if (val == null)
		{
			return -1;
		}
		
		return (int)val;
	}
}
