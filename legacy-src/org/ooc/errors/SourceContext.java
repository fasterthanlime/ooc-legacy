package org.ooc.errors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.ooc.backends.ProjectInfo;
import org.ooc.compiler.SourceInfo;
import org.ooc.compiler.libraries.LibraryManager;
import org.ooc.nodes.CHeader;
import org.ooc.nodes.RootNode;
import org.ooc.nodes.clazz.ClassDef;
import org.ooc.nodes.doc.MultiLineComment;
import org.ooc.nodes.doc.SingleLineComment;
import org.ooc.nodes.functions.FunctionDef;
import org.ooc.nodes.libs.Include;
import org.ooc.nodes.libs.Include.IncludePosition;
import org.ooc.nodes.libs.Include.IncludeType;
import org.ooc.nodes.others.EnumNode;
import org.ooc.nodes.others.SyntaxNode;
import org.ooc.nodes.others.SyntaxNodeList;
import org.ooc.nodes.preprocessor.Define;
import org.ooc.nodes.types.TypeDef;
import org.ooc.parsers.SourceParser;
import org.ooc.structures.Source;
import org.ubi.FileLocation;
import org.ubi.SourceReader;
import org.ubi.SyntaxError;

/**
 * A source context is heavily used during parsing and assembling.
 * It groups everything you have to know about a .ooc file, most notably its
 * path/dependencies/libraries informations, and its syntax tree, in the
 * {@link Source} class. 
 * 
 * @author Amos Wenger
 */
public class SourceContext {

	/** the library manager resolves includes and linking to predefined libs for us */
	public static final LibraryManager libManager = new LibraryManager();
	
	/** the structure that contains the syntax tree. the "data" counterpart of this class */
	public final Source source;
	
	/** this source context belongs to a "project", including e.g. the source path */
	public final ProjectInfo projInfo;
	
    protected final List<SourceContext> dependencies;
    
    /** points to the beginning of the .ooc file */
    public final FileLocation location;
    
    /** utility reading class used during the parsing. @see SourceParser */
	public transient final SourceReader reader;
	
	/** our very own parser. It's useful to have it here in case of dependencies. */
	public transient final SourceParser parser;
	
	protected final List<SyntaxError> errors;
	protected final Stack<SyntaxNodeList> stack;
	protected SyntaxNode lastClosed;
	
    protected final SyntaxNodeList pathboundIncludes;
    protected final SyntaxNodeList localIncludes;
    protected final SyntaxNodeList dependencyGroup;
    
    protected final Map<String, Define> defineSymbols;
    
	protected boolean isAssembled;

	/**
	 * Default constructor.
	 * @param projInfo
	 * @param info
	 * @param reader
	 * @param parser
	 */
	public SourceContext(ProjectInfo projInfo, SourceInfo info, SourceReader reader, SourceParser parser) {
		
		this.source = new Source(new RootNode(reader.getLocation(), info.underName, this), info);
		this.projInfo = projInfo;
		this.dependencies = new ArrayList<SourceContext>();
		this.reader = reader;
		this.parser = parser;
		this.errors = new ArrayList<SyntaxError>();
		this.location =  reader.getLocation();
		
		this.stack = new Stack<SyntaxNodeList>();
		this.stack.push(source.getRoot());
        
        this.pathboundIncludes = new SyntaxNodeList(location);
        this.localIncludes = new SyntaxNodeList(location);
        
        this.dependencyGroup = new SyntaxNodeList(location);
        addDefaultIncludes(location, info);
        
        this.defineSymbols = new HashMap<String, Define>();
        
        this.isAssembled = false;
		
	}
	
	protected void addDefaultIncludes(FileLocation location, SourceInfo info) {
		
		source.getRoot().add(new MultiLineComment(location, "", 
				"\n * Generated by ooc, the Object-Oriented C compiler, by Amos Wenger, 2009\n", "\n\n"));

        dependencyGroup.add(pathboundIncludes);

        dependencyGroup.add(new SingleLineComment(location, "OOC dependencies"));
        dependencyGroup.add(new Include(location, IncludeType.LOCAL, IncludePosition.SOURCE, info.simpleName + ".h"));
        dependencyGroup.add(localIncludes);

        source.getRoot().add(dependencyGroup);
        
	}
	
	/**
	 * Issue a syntax error while parsing. This happens very rarely, though, since
	 * parsing is pretty lax. Most errors are detected during the assembly phase.
	 * @param message a description of the error message
	 * @return the SyntaxError issued
	 */
	public SyntaxError err(String message) {
		
		return err(new SyntaxError(reader.getLocation(), message));
		
	}
	
	/**
	 * Issue a syntax error while parsing. This happens very rarely, though, since
	 * parsing is pretty lax. Most errors are detected during the assembly phase.
	 * @param error the error to issue
	 * @return the SyntaxError issued
	 */
	public SyntaxError err(SyntaxError error) {
		
		errors.add(error);
		return error;
		
	}
	
	/**
	 * Print to the logger all syntax errors notified
	 */
	public void printErrors() {
		
		for(SyntaxError error: errors) {
			System.err.println(error);
		}
		
	}
	
	/**
	 * @return true if any error has been issued since the beginning of the
	 * parsing.
	 */
	public boolean hasErrors() {
		
		return !errors.isEmpty();
		
	}
	
	/**
	 * @return true if the node stack has one or zero elements, which means
	 * we're in the root node (e.g. add() will add something to the root node)
	 */
	public boolean isInRoot() {
		
		return stack.size() <= 1;
		
	}

	/**
	 * Find out if we're in a certain type of node.
	 * @param clazz
	 * @param recursive If true, will not only check the top node, but the nodes
	 * under, 'till the root node itself.
	 * @return true if there is a node of type 'clazz' in the stack, either on top,
	 * or deeper, if 'recursive' is true.
	 */
    public boolean isIn(Class<? extends SyntaxNodeList> clazz, boolean recursive) {

        if(!recursive) {
            return clazz.isInstance(stack.peek());
        }
		int index = stack.size() - 1;
		boolean is = false;
		while(index > 0) {
		    SyntaxNodeList current = stack.get(index--);
		    if(clazz.isInstance(current)) {
		        is = true;
		        break;
		    }
		}
		return is;

    }

    /**
     * Get the nearest (e.g. search from top to root in the stack) node
     * of type 'clazz'
     * @param <T>
     * @param clazz
     * @return the nearest node of the right type, or null if none matches.
     */
    @SuppressWarnings("unchecked")
	public <T extends SyntaxNode>T getNearest(Class<T> clazz) {

        int index = stack.size() - 1;
        while(index > 0) {
            SyntaxNodeList current = stack.get(index--);
            if(clazz.isInstance(current)) {
                return (T) current;
            }
        }
        return null;

    }
    
    /**
     * Get the last node from the stack
     * @return
     */
    public SyntaxNode getLast() {
    
    	SyntaxNodeList list = stack.peek();
    	if(list == null || list.nodes.isEmpty()) {
    		return null;
    	}
    	
    	return list.nodes.get(list.nodes.size() - 1);
    	
    }

    /**
     * Add 'group' to the current stack node and push group into the stack
     * @param group the node to open
     */
    public void open(SyntaxNodeList group) {
    	
        add(group);
        stack.push(group);
        
    }

    /**
     * Close the top node of the stack. The type must be specified as a safety
     * measure, e.g. to make sure you're closing what you really meant to close
     * @param clazz the type of the node to close
     */
    public void close(Class<? extends SyntaxNodeList> clazz) {
    	
        if(isInRoot()) {
            err("Trying to close the root! Heeeeeeeelp! To the kiiiiiiing! Guards! (last closed was a "
            		+(lastClosed == null ? "null" : lastClosed.getClass().getSimpleName())+")");
        } else if(clazz.isInstance(stack.peek())) {
        	lastClosed = stack.pop();
        } else {
            err("Trying to close a "+clazz.getSimpleName()
            		+"  but we have an unfinished "+stack.peek().getClass().getSimpleName()
            		+" left.");
        }
        
    }

    /**
     * Add a syntax node to the current stack node.
     * @param node the node to be added
     */
    public void add(SyntaxNode node) {
    	
		stack.peek().add(node);
        
    }
    
    /**
     * Handle an Include node, especially : check for doublons, add them to the
     * special group nodes localIncludes and pathboundIncludes so that they are
     * written to the right place in the .c / .h files
     * @param include
     * @param manager
     */
    public void processInclude(Include include, AssemblyManager manager) {
    	
    	if(include.getParent() == localIncludes || include.getParent() == pathboundIncludes) {
    		include.freeze(manager);
    		return; // Nothing to do, ma'am.
    	}
    	
		include.drop();
    	
    	if(include.getType() == IncludeType.LOCAL) {
    		boolean already = false;
    		for(Include candidate: localIncludes.getNodesTyped(Include.class)) {
    			if(include.equals(candidate)) {
    				already = true;
    				break;
    			}
    		}
    		if(!already) {
    			localIncludes.add(include);
    		}
    	} else if(include.getType() == IncludeType.PATHBOUND) {
    		boolean already = false;
    		for(Include candidate: pathboundIncludes.getNodesTyped(Include.class)) {
    			if(include.equals(candidate)) {
    				already = true;
    				break;
    			}
    		}
    		if(!already) {
    			pathboundIncludes.add(include);
    		}
    	}
    	
    }
    
    /**
     * Add a define symbol to this source context. This is needed so that
     * defines are added to the right place in the .h header files, and so that
     * symbols are resolved for their types.
     * @param symbol
     */
    public void addDefineSymbol(Define symbol) {
    	
    	defineSymbols.put(symbol.name, symbol);
    	
    }
    
    /**
     * Add a dependency to this source context. It will mostly add an include, and
     * update the dependencies list.
     * @param dependency
     */
    public void addDependency(SourceContext dependency) {
    	
    	StringBuilder builder = new StringBuilder();
    	builder.append(source.getInfo().getRelativePath(dependency.source.getInfo()));
    	builder.append(dependency.source.getInfo().simpleName);
    	builder.append(".h");

        dependencyGroup.add(new Include(location, IncludeType.LOCAL, IncludePosition.HEADER, builder.toString()));
        dependencies.add(dependency);
        
    }

    /**
     * Search for a class definition.
     * @param name the name of the class, can be either simple, (MyClass), 
     * fully qualified (my.package.MyClass), or under (my_package_MyClass)
     * @return
     */
    public ClassDef getClassDef(String name) {
    	
    	return getClassDef(name, new ArrayList<SourceContext>());
    	
    }
    
    protected ClassDef getClassDef(String name, List<SourceContext> done) {
    	
    	if(name.isEmpty()) {
    		return null;
    	}
    	
        List<ClassDef> defs = source.getRoot().getNodesTyped(ClassDef.class);
        
        for(ClassDef def: defs) {
            if(def.clazz.isNamed(name)) {
                return def;
            }
        }
        
        for(SourceContext dependency: getDependenciesRecursive()) {
        	if(!done.contains(dependency)) {
        		done.add(dependency);
	            ClassDef def = dependency.getClassDef(name, done);
	            if(def != null) {
	                return def;
	            }
        	}
        }
        
        return null;
        
    }

	/**
	 * Search for a define symbol by name
	 * @param name the name of the symbol to look for
	 * @return the found symbol, or null if none found.
	 */
	public Define getDefineSymbol(String name) {
		
		return getDefineSymbol(name, new ArrayList<SourceContext>());
		
	}
	
	protected Define getDefineSymbol(String name, List<SourceContext> done) {
		
		Define symbol = defineSymbols.get(name);
		if(symbol != null) {
			return symbol; 
		}
		
		for(SourceContext dependency: dependencies) {
			if(!done.contains(dependency)) {
				done.add(dependency);
				symbol = dependency.getDefineSymbol(name, done);
				if(symbol !=  null) {
					return symbol;
				}
			}
		}
		
		return null;
		
	}
	
	/**
	 * Test the existence of a function by its unmangled name
	 * @param unmangledName
	 * @return
	 */
	public boolean hasUnmangledFunction(String unmangledName) {

		List<FunctionDef> defs = source.getRoot().getNodesTyped(FunctionDef.class);
		for(FunctionDef def: defs) {
			if(def.function.getSimpleName().equals(unmangledName)) {
				return true;
			}
		}
		
		return false;
		
	}

	/**
	 * @return the list of source contexts this one directly depends on
	 */
	public List<SourceContext> getDependencies() {

		return dependencies;
		
	}
	
	/**
	 * @return the list of all source contexts this on depends on, recursively
	 * (e.g. include indirect dependencies)
	 */
	public List<SourceContext> getDependenciesRecursive() {
		
		List<SourceContext> list = new ArrayList<SourceContext>();
		list.add(this);
		addWithDependenciesRecursive(list);
		return list;
		
	}
	
	protected void addWithDependenciesRecursive(List<SourceContext> list) {
		
		for(SourceContext dependency: dependencies) {
			if(!list.contains(dependency)) {
				list.add(dependency);
				dependency.addWithDependenciesRecursive(list);
			}
		}
		
	}
	
	
	@Override
	public String toString() {
		
		return source.getInfo().fullName;
		
	}

	/**
	 * @return true if the source context has finished assembling successfully =)
	 */
	public boolean isAssembled() {
		
		return isAssembled;
		
	}

	/**
	 * change the assembled state of this source context
	 * @param isAssembled
	 */
	public void setAssembled(boolean isAssembled) {

		this.isAssembled = isAssembled;
		
	}

	/**
	 * Searches through all the pathbound includes to find typedefs, in
	 * order to help with type resolution
	 * @param name
	 * @return
	 */
	public TypeDef getTypeDef(String name) {

		// Check our own typedefs / enums and the neighbors' ones
		for(SourceContext context: getDependenciesRecursive()) {
			List<TypeDef> typeDefs = context.source.getRoot().getNodesTyped(TypeDef.class, true);
			for(TypeDef typeDef: typeDefs) {
				if(typeDef.getName().equals(name)) {
					return typeDef;
				}
			}
		}
		
		List<Include> incs = pathboundIncludes.getNodesTyped(Include.class, true);
		incs.addAll(source.getRoot().getNodesTyped(Include.class, true));
		
		// Check in our includes
		for(Include include: incs) {
			CHeader header = include.getHeader();
			if(header != null) {
				TypeDef typeDef = header.getTypeDef(name);
				if(typeDef != null) {
					return typeDef;
				}
			}
		}
		
		return null;
		
	}
	
	/**
	 * Searches through all the pathbound includes to find typedefs, in
	 * order to help with type resolution
	 * @param name
	 * @return
	 */
	public EnumNode getEnum(String name) {

		// Check our own typedefs / enums and the neighbors' ones
		for(SourceContext context: getDependenciesRecursive()) {
			List<EnumNode> enumNodes = context.source.getRoot().getNodesTyped(EnumNode.class, true);
			for(EnumNode enumNode: enumNodes) {
				if(enumNode.isNamed(name)) {
					return enumNode;
				}
			}
		}
		
		return null;
		
	}
	
}
