import lang.String;
include string;

class StringTokenizer {

	String str;
	String delim;
	Int index, length;

	func new(=str, =delim) {
		
		length = this.str.length;
		index = 0;
		
	}

	func hasNext -> Bool {
		
		return index < str.length;
		
	}

	func nextToken(String delim) -> String {
		
		//printf("In String '%s', at index '%d', with delimiter '%s'\n", str, index, delim);
		String buffer = malloc(length + 1); // chars are always of size 1
		Int bufIndex = 0;
		Int delimLength = delim.length;
		while(index < length) {
			buffer[bufIndex] = str[index++];
			for(Int i: 0..delimLength) {
				if(buffer[bufIndex] == delim[i]) {
					goto done;
				}
			}
			bufIndex++;
		}
		done:
		buffer[bufIndex] = '\0';
		//printf("Now at index '%d', and buffer='%s'\n", index, buffer);
		return buffer;
		
	}
	
	func nextToken -> String {
		return nextToken(delim);
	}

}
