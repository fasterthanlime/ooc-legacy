package org.ooc.errors;

import org.ubi.CompilationFailedError;

/**
 * Thrown when the pass limit is maxed out, thus the compilation process is
 * abandoned
 * 
 * @author Amos Wenger
 */
public class MaxedOutPassLimit extends CompilationFailedError {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 4857062585280357766L;
	
	/**
	 * Default constructor
	 * @param message
	 */
	public MaxedOutPassLimit(int passes) {
		
		super(null, "Assembly failed to complete in "+passes+" passes, abandoning..");

	}

}
