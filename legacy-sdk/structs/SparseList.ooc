include stdlib, stdio, memory;

import List, Iterable, Iterator;

/**
 * A sparse list stores its data into an array, resizes it when it's too
 * small,
 * 
 */
class SparseList from List {

	Object* data;
	Int capacity;
	Int size;
	
	func new {
		this(10);
	}

	func new(=capacity) {
		size = 0;
		data = calloc(capacity, sizeof(Object));
		//data[0] = null; // To null out the first element is capital!
	}
	
	func size;
	func capacity;
	
	func add(Object element) {
		Int slot = getFreeSlot(-1);
		//printf("Adding to SparseList, capacity=%d, size=%d, free slot found at %d\n", capacity, size, slot);
		data[slot] = element;
		size++;
	}
	
	func add(Int index, Object element) {
		data[getFreeSlot(index)] = element;
		size++;
	}
	
	func clear {
		size = 0;
	}
	
	func get(Int index) -> Object {
		
		Int slot = indexToSlot(index);
		if(slot == -1) {
			//printf("Trying to retrieve index #%d, slot not found, fuck you?\n", index);
			fflush(stdout);
			Int x = 0;
			x = x / x;
		}
		return data[slot];
	}
	
	func indexOf(Object element) -> Int {
		
		if(element == null) {
			// No nulls are allowed in a sparse list
			return -1;
		}
		
		Int counter = 0;
		for(Int i: 0..capacity) {
			if(data[i] == element) {
				break;
			} else if(data[i] != null) {
				counter++;
			}
		}
		
		return counter;
		
	}
	
	func lastIndexOf(Object element) -> Int {
		
		if(element == null) {
			// No nulls are allowed in a sparse list
			return -1;
		}
		
		Int counter = size - 1;
		for(Int i = capacity - 1; i >= 0; i--) {
			if(data[i] == element) {
				break;
			} else if(data[i] != null) {
				counter--;
			}
		}
		
		return counter;
		
	}
	
	func remove(Int index) -> Object {
		Int slot = indexToSlot(index);
		Object element = data[slot];
		data[slot] = null;
		size--;
		return element;
	}
	
	func removeElement(Object element) -> Bool {
		Int slot = slotOf(element);
		if(slot != -1) {
			data[slot] = null;
			size--;
			return true;
		} else {
			return false;
		}
	}
	
	func set(Int index, Object element) {
		Int slot = indexToSlot(index);
		if(slot != -1) {
			data[slot] = element;
		}
	}
	
	/** private */
	func slotOf(Object element) -> Int {
		
		for(Int i: 0..capacity) {
			if(data[i] == element) {
				return i;
			}
		}
		return -1;
		
	}
	
	/**
	 * private
	 * 
	 * Here is quick rundown of how the function works:
	 *  - There is a distinction between an index and a slot. Indices
	 * are all the user sees. Slots are the real position of data in
	 * the array. Thus, often a slot number is greater than an index
	 * number, as there are holes in the data.
	 *  - The 'minimum' parameter is how much elements must there be before
	 * the free slot returned. If you don't care where the free slot is,
	 * -1 should be passed as a parameter
	 */
	func getFreeSlot(Int minimum) -> Int {
		
		if(minimum == -1) {
			// If we don't care, start at -1
			// In the future, this may be randomized
			// to allow for *statically* faster inserts
		}
		
		Int freeSlot = -1;
		
		// Counts the number of elements before the free slot
		Int counter = 0;
		
		for(Int i: 0..capacity) {
			if(data[i] == null) {
				// A free slot! Great! But do we have at
				// least 'minimum' elements before us ?
				if(counter >= minimum) {
					// Remember, a slot is the position in the real array
					freeSlot = i;
					break;
				}
			} else {
				if(counter >= minimum && minimum != -1) {
					// Huh oh, we reached the wanted index, and still no
					// free space ! We gotta grow.
					if(data[capacity - 1] != null) {
						grow();
					}
					memmove(data+i+1, data+i, (size-i) * sizeof(Object));
					freeSlot = i;
					break;
				}
				// Stumbled upon an element, increase the counter
				counter++;
			}
		}
		
		// No free slot found ?
		if(freeSlot == -1) {
			// Doesn't matter, we'll find room
			freeSlot = capacity;
			// Now pay the rent.
			grow();
		}
		
		// Now, except if malloc went woopsie-daisy, we have at least one free slot
		return freeSlot;
		
	}

	/**	
	 * private 
	 */
	func indexToSlot(Int index) -> Int {
		
		Int slot = -1;
		Int counter = 0;
		for(Int i: 0..capacity) {
			if(data[i] != null) {
				if(counter >= index) {
					slot = i;
					break;
				}
				counter++;
			}
		}
		
		return slot;
		
	}

	/** private */	
	func grow {
		Double temp1 = (Double) capacity;
		Double temp2 = temp1 * 1.1;
		Int temp3 = temp2;
		capacity = temp3 + 10;
		//printf("SparseList: Trying to grow temp1=%.2f, temp2=%2.f, temp3=%d, now capacity=%d\n", temp1, temp2, temp3, capacity);
		Object* tmpData = realloc(data, capacity * sizeof(Object));
		if(tmpData != null) {
			data = tmpData;
		} else {
			printf("Failed to allocate %d bytes of memory for array to grow! Exiting..\n", capacity * sizeof(Object));
			exit(1);
		}
	}
	
	/** private */
	func isValidIndex(Int i) -> Bool {
		if(i < 0) {
			printf("SparseList: Index out of bounds: index=%d < 0\n", i);
			return false;
		} else if(i >= size) {
			printf("SparseList: Index out of bounds: index=%d >= size=%d\n", i, size);
			return false;
		}
		return true;
	}
	
	implement iterator {
		return (Iterator) new SparseListIterator(this);
	}

}

class SparseListIterator from Iterator {
	
	SparseList list;
	Int index = 0;
	Int slot = 0;
	
	func new(=list);
	
	implement hasNext {
		return index < list.size;
	}
	
	implement next {
		while(list.data[slot] == null) {
			slot++;
		}
		Object o = list.data[slot];
		slot++;
		index++;
		return o;
	}
	
}
