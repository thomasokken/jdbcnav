///////////////////////////////////////////////////////////////////////////////
// JDBC Navigator - A Free Database Browser and Editor
// Copyright (C) 2001-2010	Thomas Okken
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License, version 2,
// as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////

package jdbcnav.util;

import java.lang.reflect.*;
import java.util.*;


public class ArrayCollection<T> implements Collection<T> {
	private Object array;

	public ArrayCollection(Object array) {
		if (array == null)
			this.array = new Object[0];
		else {
			if (!array.getClass().isArray())
				throw new IllegalArgumentException("Argument is not an array");
			this.array = array;
		}
	}

	public boolean add(T o) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean contains(Object o) {
		int length = Array.getLength(array);
		for (int i = 0; i < length; i++) {
			Object o2 = Array.get(array, i);
			if (o2 == null ? o == null : o2.equals(o))
				return true;
		}
		return false;
	}

	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!contains(o))
				return false;
		return true;
	}

	public boolean isEmpty() {
		return Array.getLength(array) == 0;
	}

	public Iterator<T> iterator() {
		return new ArrayIterator();
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public int size() {
		return Array.getLength(array);
	}

	public Object[] toArray() {
		int length = Array.getLength(array);
		Class<?> klass = array.getClass().getComponentType();
		Object[] ret;
		if (klass.isPrimitive()) {
			if (klass == Boolean.TYPE)
				ret = new Boolean[length];
			else if (klass == Character.TYPE)
				ret = new Character[length];
			else if (klass == Byte.TYPE)
				ret = new Byte[length];
			else if (klass == Short.TYPE)
				ret = new Short[length];
			else if (klass == Integer.TYPE)
				ret = new Integer[length];
			else if (klass == Long.TYPE)
				ret = new Long[length];
			else if (klass == Float.TYPE)
				ret = new Float[length];
			else if (klass == Double.TYPE)
				ret = new Double[length];
			else if (klass == Void.TYPE)
				ret = new Void[length];
			else
				throw new IllegalArgumentException("Unknown primitive type \""
						+ klass.getName() + "\"");
			for (int i = 0; i < length; i++)
				Array.set(ret, i, Array.get(array, i));
		} else {
			ret = (Object[]) Array.newInstance(klass, length);
			System.arraycopy(array, 0, ret, 0, length);
		}
		return ret;
	}
	
	@SuppressWarnings(value={"unchecked"})
	public <T2> T2[] toArray(T2[] a) {
		int length = Array.getLength(array);
		if (a.length < length) {
			a = (T2[]) Array.newInstance(a.getClass().getComponentType(),
											 length);
			System.arraycopy(array, 0, a, 0, length);
		} else {
			System.arraycopy(array, 0, a, 0, length);
			if (a.length > length)
				a[length] = null;
		}
		return a;
	}

	private class ArrayIterator implements Iterator<T> {
		private int index = 0;

		public boolean hasNext() {
			return index < Array.getLength(array);
		}

		@SuppressWarnings(value={"unchecked"})
		public T next() {
			try {
				return (T) Array.get(array, index++);
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
