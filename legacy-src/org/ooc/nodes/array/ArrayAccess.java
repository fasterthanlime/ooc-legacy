package org.ooc.nodes.array;

import java.io.IOException;

import org.ooc.nodes.interfaces.LinearNode;
import org.ooc.nodes.others.SyntaxNode;
import org.ooc.nodes.others.VariableAccess;
import org.ooc.structures.Variable;
import org.ubi.FileLocation;

/**
 * Access to an array
 *
 * @author Amos Wenger
 */
public class ArrayAccess extends VariableAccess implements LinearNode {
	
	protected VariableAccess access;

	/**
	 * Create a new array access
	 * @param location
	 * @param access
	 * @param subscript
	 */
    public ArrayAccess(FileLocation location, VariableAccess access, Subscript subscript) {
    	
        super(location, new Variable(access.getType().deriveArrayLevel(-1), access.toString()));
        this.addAll(subscript);
        this.access = access;
        access.setContext(this);
        
    }

    
    @Override
	public void writeToCSource(Appendable a) throws IOException {
    	
    	writeWhitespace(a);
        access.writeToCSource(a);
        a.append("[");
        for(SyntaxNode node: this.nodes) {
        	if(node != access) {
        		node.writeToCSource(a);
        	}
        }
        a.append("]");
        
    }
    
}
