//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package param;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Function factory implementing a cache for functions from other factories.
 * This way, each function is only stored in memory once, so as to reduce
 * memory usage and improve speed. This function factory can also use a
 * cache for operations on functions, so that if an operation on the same
 * two functions has already been performed before, the result will be
 * looked up instead of being performed again.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 * @see FunctionFactory
 * @see CachedFunction
 */
final class CachedFunctionFactory extends FunctionFactory {
	/**
	 * Represents an entry of the operation cache.
	 *
	 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
	 */
	private class OpCacheKey
	{
		/** first operand */
		CachedFunction first;
		/** second operand */
		CachedFunction second;
		
		/**
		 * Construct a new operation cache entry.
		 * 
		 * @param first first operand
		 * @param second second operand
		 */
		public OpCacheKey(CachedFunction first, CachedFunction second)
		{
			if (second.getNumber() > first.getNumber()) {
				CachedFunction swap = first;
				first = second;
				second = swap;
			}
			this.first = first;
			this.second = second;
		}
		
		@Override
		public boolean equals(Object obj) {
			OpCacheKey key = (OpCacheKey) obj;
			return first == key.first && second == key.second;
		}
		
		@Override
		public int hashCode() {
			int hash = first.getNumber();
			hash = second.getNumber() + (hash << 6) + (hash << 16) - hash;
			return hash;
		}
	}
	
	/** function factory of which we cache functions */
	private FunctionFactory context;
	/** maps each function from {@code context} to a unique integer */
	private ConcurrentHashMap<Function, CachedFunction> cachedFunctions;
	/** maps each integer to function from {@code context}, cf. also {@code cachedFunctions} */
	private ConcurrentHashMap<CachedFunction,Function> functions;
	/** next new function will be assigned this number */
	private AtomicInteger nextFunctionNumber;
	/** function representing one (1) */
	private CachedFunction one;
	/** function representing zero (0) */
	private CachedFunction zero;
	/** true iff operation cache is to be used */
	private boolean useOpCache;
	/** cache for additions (and indirectly subtractions) */		
	private ConcurrentHashMap<OpCacheKey, CachedFunction> addCache;
	/** cache for multiplications (and indirectly divisions) */	
	private ConcurrentHashMap<OpCacheKey, CachedFunction> multCache;
	/** cache for star operation */
	private ConcurrentHashMap<CachedFunction, CachedFunction> starCache;
	
	/**
	 * Constructs a new cached function factory.
	 * Will cache functions of the given {@code context}.
	 * 
	 * @param context function factory to cache functions of
	 */
	CachedFunctionFactory(FunctionFactory context) {
		super(context.parameterNames, context.lowerBounds, context.upperBounds);
		this.context = context;
		cachedFunctions = new ConcurrentHashMap<>();
		functions = new ConcurrentHashMap<>();
		nextFunctionNumber = new AtomicInteger();
		one = cacheFunction(context.getOne());
		zero = cacheFunction(context.getZero());
		addCache = new ConcurrentHashMap<OpCacheKey, CachedFunction>();
		multCache = new ConcurrentHashMap<OpCacheKey, CachedFunction>();
		starCache = new ConcurrentHashMap<CachedFunction, CachedFunction>();
		useOpCache = true;
	}
	
	/**
	 * Returns whether operations cache is used.
	 * 
	 * @return true iff operation cache is used
	 */
	boolean isUseOpCache()
	{
		return useOpCache;
	}
	
	/**
	 * Returns the unique integer representing the given function.
	 * In case the function already exists in the function cache, returns
	 * the assigned integer. Otherwise, inserts function in the cache and
	 * returns the newly assigned integer.
	 * 
	 * @param function function to return unique integer of
	 * @return unique integer representing function
	 */
	private CachedFunction cacheFunction(Function function)
	{
		CachedFunction cached = cachedFunctions.get(function);
		if (cached == null) {
			int i = nextFunctionNumber.incrementAndGet();
			cached = new CachedFunction(this, i);
			functions.put(cached, function);
			cachedFunctions.put(function, cached);
		}
		return cached;
	}
	
	/**
	 * Returns integer with the given unique number.
	 * 
	 * @param number number of function to return
	 * @return function with the given number
	 */
	Function getFunction(CachedFunction number)
	{
		return functions.get(number);
	}
	
	@Override
	public Function getOne()
	{
		return one;
	}

	@Override
	public Function getZero()
	{
		return zero;
	}

	private Function getFunctionFromCache(CachedFunction cached)
	{
		return functions.get(cached);
	}
	
	CachedFunction add(CachedFunction cached1, CachedFunction cached2)
	{
		CachedFunction result;
		OpCacheKey opCacheKey = null;
		if (useOpCache) {
			opCacheKey = new OpCacheKey( cached1, cached2);
			result = addCache.get(opCacheKey);
			if (result != null) {
				return result;
			}
		}
		Function function1 = getFunctionFromCache(cached1);
		Function function2 = getFunctionFromCache(cached2);
		result = cacheFunction(function1.add(function2));
		if (useOpCache) {
			addCache.put(opCacheKey, result);
		}
		return result;
	}

	CachedFunction negate(CachedFunction cached)
	{
		Function function = getFunctionFromCache(cached);
		return cacheFunction(function.negate());
	}

	CachedFunction multiply(CachedFunction cached1, CachedFunction cached2)
	{
		CachedFunction result;
		OpCacheKey opCacheKey = null;
		if (useOpCache) {
			opCacheKey = new OpCacheKey((CachedFunction) cached1, (CachedFunction) cached2);
			result = multCache.get(opCacheKey);
			if (result != null) {
				return result;
			}
		}
		Function function1 = getFunctionFromCache(cached1);
		Function function2 = getFunctionFromCache(cached2);
		result = cacheFunction(function1.multiply(function2));
		if (useOpCache) {
			multCache.put(opCacheKey, (CachedFunction) result);
		}
		return result;
	}

	CachedFunction divide(CachedFunction cached1, CachedFunction cached2)
	{
		Function function1 = getFunctionFromCache(cached1);
		Function function2 = getFunctionFromCache(cached2);
		return cacheFunction(function1.divide(function2));
	}
	
	CachedFunction star(CachedFunction cached) {
		CachedFunction result;
		if (useOpCache) {
			result = starCache.get(cached);
			if (result != null) {
				return result;
			}
		}
		Function function = getFunctionFromCache(cached);
		result = cacheFunction(function.star());
		if (useOpCache) {
			starCache.put(cached, result);
		}
		return result;
	}

	Function toConstraint(CachedFunction cachedFunction)
	{
		return getFunctionFromCache(cachedFunction).toConstraint();
	}

	public BigRational evaluate(CachedFunction cached, Point point, boolean cancel)
	{
		Function function = getFunctionFromCache(cached);
		return function.evaluate(point, cancel);
	}
	
	public BigRational evaluate(CachedFunction cached, Point point)
	{
		Function function = getFunctionFromCache(cached);
		return function.evaluate(point);
	}

	@Override
	public Function fromBigRational(BigRational from)
	{
		Function fn = context.fromBigRational(from);
		return cacheFunction(fn);
	}

	public BigRational asBigRational(CachedFunction cached) {
		Function function = getFunctionFromCache(cached);
		return function.asBigRational();
	}

	public boolean check(CachedFunction cached, Point point,
			boolean strict) {
		Function function = getFunctionFromCache(cached);
		return function.check(point, strict);
	}

	public boolean isNaN(CachedFunction cached) {
		Function function = getFunctionFromCache(cached);
		return function.isNaN();
	}

	public boolean isInf(CachedFunction cached) {
		Function function = getFunctionFromCache(cached);
		return function.isInf();
	}

	public boolean isMInf(CachedFunction cached) {
		Function function = getFunctionFromCache(cached);
		return function.isMInf();
	}

	public boolean isOne(CachedFunction cached) {
		Function function = getFunctionFromCache(cached);
		return function.isOne();
	}

	public boolean isZero(CachedFunction cached) {
		Function function = getFunctionFromCache(cached);
		return function.isZero();
	}

	public boolean isConstant(CachedFunction cached) {
		Function function = getFunctionFromCache(cached);
		return function.isConstant();
	}

	@Override
	Function getNaN() {
		return cacheFunction(context.getNaN());
	}

	@Override
	Function getInf() {
		return cacheFunction(context.getInf());
	}

	@Override
	Function getMInf() {
		return cacheFunction(context.getMInf());
	}

	@Override
	Function getVar(int var) {
		return cacheFunction(context.getVar(var));
	}
}
