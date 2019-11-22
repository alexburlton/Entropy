package object;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Wrap up a hashmap to introduce extra helper methods
 */
public class SuperHashMap<K, V> extends HashMap<K, V>
{
	public HandyArrayList<K> getKeysAsVector()
	{
		HandyArrayList<K> keys = new HandyArrayList<>();
		
		Iterator<K> it = keySet().iterator();
		for (; it.hasNext(); )
		{
			K key = it.next();
			keys.add(key);
		}
		
		return keys;
	}
	
	public HandyArrayList<V> getValuesAsVector()
	{
		return getValuesAsVector(false);
	}
	public HandyArrayList<V> getValuesAsVector(boolean distinct)
	{
		HandyArrayList<V> values = new HandyArrayList<>();
		
		Iterator<V> it = values().iterator();
		for (; it.hasNext(); )
		{
			V val = it.next();
			if (!distinct
			  || !values.contains(val))
			{
				values.add(val);
			}
		}
		
		return values;
	}
	
	public SuperHashMap<K, V> factoryCopy()
	{
		SuperHashMap<K, V> ret = new SuperHashMap<>();
		ret.putAll(this);
		return ret;
	}
	
	public void removeAllWithValue(V value)
	{
		HandyArrayList<K> keys = getKeysAsVector();
		for (K key : keys)
		{
			V val = get(key);
			if (val.equals(value))
			{
				remove(key);
			}
		}
	}
}
