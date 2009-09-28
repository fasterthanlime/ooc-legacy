use m; // libm is actually the math library
include math, stdio;
import Vector;

class Vector3i from Vector{

	Int x;
	Int y;
	Int z;

	func new {
		x = 0;
		y = 0;
		z = 0;
	}

	func new(=x, =y, =z);
	
	func new(This copy) {
		x = copy.x;
		y = copy.y;
		z = copy.z;
	}

	implement squaredLength {
		(x * x + y * y + z * z);
	}

	implement length {
		sqrt(x * x + y * y + z * z);
	}
	
	implement reverse {
		x = -x;
		y = -y;
		z = -z;
	}
	
	implement normalize {
		Float _length = length();
		if(_length < 0) {
			if(_length > -Vector.EPSILON) {
				return;
			}
		} else {
			if(_length < Vector.EPSILON) {
				return;
			}
		}
		x /= _length;
		y /= _length;
		z /= _length;
	}
	
	func set(=x, =y, =z);
	
	func set(This t) {
		x = t.x;
		y = t.y;
		z = t.z;
	}
	
	func add(This t) {
		x += t.x;
		y += t.y;
		z += t.z;
	}
	
	func sub(This t) {
		x -= t.x;
		y -= t.y;
		z -= t.z;
	}
	
	func mul(Float f) {
		x *= f;
		y *= f;
		z *= f;
	}
	
	func div(Int f) {
		x /= f;
		y /= f;
		z /= f;
	}
	
	func add(Int x, Int y) {
		this.x += x;
		this.y += y;
		this.z += z;
	}
	
	func sub(Int x, Int y) {
		this.x -= x;
		this.y -= y;
		this.z -= z;
	}
	
	implement repr {
		const Int BUFFER_SIZE = 50;
		String buffer = malloc(BUFFER_SIZE);
		snprintf(buffer, BUFFER_SIZE, "(%d, %d, %d)", x, y, z);
		return buffer;
	}

}
