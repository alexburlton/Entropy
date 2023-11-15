package object;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HandyArrayList<E> extends ArrayList<E>
{
	public HandyArrayList()
	{}
	public HandyArrayList(Collection<? extends E> collection)
	{
		super(collection);
	}
	
	public E firstElement()
	{
		return get(0);
	}
	public E lastElement()
	{
		return get(size() - 1);
	}
	
	public HandyArrayList<E> factoryCopy()
	{
		HandyArrayList<E> ret = new HandyArrayList<>();
		for (E element : this)
		{
			ret.add(element);
		}
		
		return ret;
	}
	
	public HandyArrayList<E> createFilteredCopy(Predicate<? super E> p)
	{
		Stream<E> filteredStream = stream().filter(p);
		Collector<E, ?, HandyArrayList<E>> collector = Collectors.toCollection(HandyArrayList<E>::new);
		
		return filteredStream.collect(collector);
	}
	
	@SafeVarargs
	public static <X> HandyArrayList<X> factoryAdd(X... elements)
	{
		HandyArrayList<X> ret = new HandyArrayList<>();
		
		for (int i=0; i<elements.length; i++)
		{
			X element = elements[i];
			ret.add(element);
		}
		
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public E[] toGenericArray()
	{
		if (size() == 0)
		{
			//Nothing else for us to return here
			Object[] ret = {};
			return (E[])ret;
		}
		
		//Get an element so we can get a Class<E> (ffs - this shouldn't be this hard!!)
		E elt = firstElement();
		Class<E> clz = (Class<E>)elt.getClass();
		
		//Now we can construct our E[] array, although even with this you have to cast and suppress warnings
		E[] template = (E[])Array.newInstance(clz, size());
		
		//Finally...
		return toArray(template);
	}
	
	public HandyArrayList<E> reverse()
	{
		HandyArrayList<E> ret = new HandyArrayList<>();
		for (int i=size()-1; i>=0; i--)
		{
			E obj = get(i);
			ret.add(obj);
		}
	
		return ret;
	}
	
	public static <E> HandyArrayList<HandyArrayList<E>> getBatches(HandyArrayList<E> list, int batchSize)
	{
		HandyArrayList<HandyArrayList<E>> ret = new HandyArrayList<>();
		HandyArrayList<E> currentBatch = new HandyArrayList<>();
		for (int i=0; i<list.size(); i++)
		{
			E item = list.get(i);
			
			if (currentBatch.size() < batchSize)
			{
				currentBatch.add(item);
			}
			else
			{
				ret.add(currentBatch);
				
				currentBatch = new HandyArrayList<>();
				currentBatch.add(item);
			}
		}
		
		ret.add(currentBatch);
		return ret;
	}
}
