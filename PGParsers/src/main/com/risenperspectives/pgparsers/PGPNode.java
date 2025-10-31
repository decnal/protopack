package com.risenperspectives.pgparsers;
import java.lang.StringBuilder;

// PGPNode (Pretty Good Parser Node)
//
// By: D. Lance Robinson
// November 2020
//
// This object is used to create a structured dataset
// which can be built from an XML, JSON, YAML, FLAT or other data schemes.
//
//          NODE <--
//           (.parent_)
//           +---------+
//           |  NODE   |(.headAttr_)--> NODE (first node in a list of attribute nodes)
//           |  ----   |
//   (.prev_)| .type_  |(.next_)
// NODE <--  | .name_  |  --> NODE (next in the sibling list of nodes)
//           | .value_ |
//           +---------+
//            (.headChild_)
//              --> NODE (first node in a sibling list of nodes)
//
// A NODE has 5 possible pointers to other nodes that help create
// a network of nodes that can be easily traversed.
//    .prev_      -- specifies the previous node in a list of nodes at the same level, or null if none.
//    .next_      -- specifies the next node in a list of nodes at the same level, Is a circular list.
//					 .prev_ and .next_ will be null if there are no siblings.
//    .parent_    -- specifies the upper level node, or null if none (top Node).
//    .headChild_ -- specifies the first sub node in a list of nodes, is null if no children.
//    .headAttr_  -- specifies the first attribute node in a list of nodes, is null if no attributes.
//
// The topmost node:
// * .parent_ and .prev_ and .next_ will always be null.
// * For JSON and YAML, the topmost node usually has no name and is used to
//   define brackets around all the nodes starting with .headChild_.
// * For XML, The topmost node can be named node with attributes, siblings and children.
//
// Only Nodes with a name can have a .headAttr_ (This may not be true for JSON/YAML Objects and Arrays)
//
// When a Node is an attribute:
// * .headAddr_ and .headChild_ will always be null.
// * Its .parent_ will be the node that the attribute is specifying.
// * .prev_ and .next_ are circular pointers to sibling list

public class PGPNode {

	// The value of this node can take form in several different forms...
	enum PGPDataType {
		NULL,			// unquoted 'null' data type as with JSON
		VARIANT,		// Guess what kind of value this is.
		STRING,			// Normal String value. Can be Quoted
		UNQUOTED_STRING,// Unquoted String (cannot hold non-printable chars, and typically not spaces)
		UNESCAPED_STRING,// Used by xml for <![CDATA[...]]> sections. Can be normal String for other protocols.
		NUMBER,			// Mostly like UNQUOTED_STRING since the number is stored as a string.
//		INTEGER,        // Int (YAML)
//		FLOAT,          // float (YAML)
		BLOB,			// Binary [large] object (TBD)
		BOOLEAN,		// The value is either 'true' or 'false' (not quoted)
		OBJECT,			// No 'value', The children are {x,y,z} elements
		ARRAY			// No 'value', and the children are array [x,y,z] elements.
	};

	// pParent -- owner/parent of this node.
	protected PGPNode parent_;

	// pHeadChild -- First child added. Points to circular list of nodes.
	protected PGPNode headChild_;

	// pHeadAttribute -- First attribute added of a circular list of attributes.
	// XML is only format that supports attributes.
	protected PGPNode headAttr_;

	// pNextNode and pPrevNode is a circular list of sibling nodes.
	// When the node comes back and matches paraen_->headChild_ it is past the end.
	protected PGPNode next_;
	protected PGPNode prev_;

	protected String name_;
	protected String value_;
	protected PGPDataType type_;

	static private String nameSeparator_ = "\\.";
	static private int    debug_level    = 0;
	static private int    depthSpaces_   = 2;
	static private String depthString_   = "  ";

	// Methods

	public PGPNode() {
		type_  = PGPDataType.VARIANT;
	}

	public PGPNode( String name ) {
		name_  = name;
		type_  = PGPDataType.VARIANT;
	}

	public PGPNode( String name, String value ) {
		name_  = name;
		value_ = value;
		type_  = PGPDataType.VARIANT;
	}

	public PGPNode( String name, String value, PGPDataType type ) {
		name_  = name;
		value_ = value;
		type_  = type;
		setDepthSpaces(2);
	}

	// delete() -- Recursively free this node and all child and attribute nodes.
	public void delete() {
		// free all child and attribute nodes
		while ( null != headAttr_ )
			headAttr_.delete();
		while ( null != headChild_ )
			headChild_.delete();

		// remove this from the sibling list
		unlink();

	}//delete()

	// unlink() -- detach this link from its parents and siblings
	//             But, keep its attributes, children, and value.
	public PGPNode unlink() {
		if ( null == parent_ )
			return this;	// Nothing to do if this is the top.

		// unlink from sibling list.
		if ( next_ == this ) {

			// Lonely child. remove from whatever list it was in (child or attr.)
			if ( parent_.headChild_ == this)
				parent_.headChild_ = null;
			else if ( parent_.headAttr_ == this )
				parent_.headAttr_ = null;

		} else {
			// There are siblings.
			if ( parent_.headChild_ == this )
				parent_.headChild_ = next_;
			else if ( parent_.headAttr_ == this )
				parent_.headAttr_ = next_;

			// disconnect from circular chain
			next_.prev_ = prev_;
			prev_.next_ = next_;

		}

		// This node can be used as a top node now.
		parent_ = next_ = prev_ = null;

		return this;

	}//unlink()


	public String getName()   {		return name_;     }
	public String getValue()   {	return value_;    }
	public PGPDataType getType() {	return type_;     }
	public PGPNode getParent() {	return parent_;	  }
	public PGPNode getAttr()   {	return headAttr_; }
	public PGPNode getChild()  {	return headChild_;}
	public PGPNode getNext()   {	return next_;     }
	public PGPNode getPrev()   {	return prev_;     }

	public PGPNode setName(String name)      { name_  = name; return this; }
	public PGPNode setValue(String val)      { value_ = val;  return this; }
	public PGPNode setValue(String val, PGPDataType type) {
			value_ = val;
			type_  = type;
			return this;
	}
	public PGPNode setType(PGPDataType type) { type_  = type; return this; }

	public PGPNode setValueFmt(String fmt, Object obj) {
		value_ = String.format(fmt, obj);
		return this;
    }

	public static String getNameSeparator()         { return nameSeparator_; }

	// DepthSpaces define how many spaces to use per deeper depth.
	public void setDepthSpaces( int spaces )
	{
		if ( depthSpaces_ != spaces )
		{
			depthSpaces_ = spaces;
			depthString_ = "                ".substring(0,spaces);
		}
	}
	public int getDepthSpaces()
	{
		return depthSpaces_;
	}
	public String getDepthString()
	{
		return depthString_;
	}


	public boolean isNameEqual( String name ) {
		return ( name_.equals(name) );
	}//isNameEqual()


	public PGPNode addChild() {
		return addChild( new PGPNode() );
	}
	public PGPNode addChild( String name ) {
		return addChild( new PGPNode( name ) );
	}
	public PGPNode addChild( String name, String value ) {
		return addChild( new PGPNode( name, value ) );
	}
	public PGPNode addChild( String name, int number ) {
		return addChild( name, Integer.toString(number), PGPDataType.NUMBER );
	}
	public PGPNode addChild( String name, boolean b ) {
		return addChild( name, Boolean.toString(b), PGPDataType.BOOLEAN );
	}
	public PGPNode addChild( String name, String value, PGPDataType type ) {
		return addChild( new PGPNode( name, value, type ) );
	}

	public PGPNode addChild( PGPNode node ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.addChild("+node.flatName()+":"+node.value_+")");

		node.parent_ = this;
		if ( headChild_ != null ) {
			// Add to end of child list. The child list is a circular list.
			node.next_ = headChild_;
			node.prev_ = headChild_.prev_;
			headChild_.prev_ = node;
			node.prev_.next_ = node;
		} else {
			// Is first child
			// Since this is circular list, this lone node points to itself.
			headChild_ = node.next_ = node.prev_ = node;
		}

		return node;

	}//addChild()


	public PGPNode addNext() {
		return addNext( new PGPNode() );
	}
	public PGPNode addNext( String name, String value ) {
		return addNext( new PGPNode( name, value ) );
	}
	public PGPNode addNext( String name, int number ) {
		return addNext( name, Integer.toString(number), PGPDataType.NUMBER );
	}
	public PGPNode addNext( String name, boolean b ) {
		return addNext( name, Boolean.toString(b), PGPDataType.BOOLEAN );
	}
	public PGPNode addNext( String name, String value, PGPDataType type ) {
		return addNext( new PGPNode( name, value, type ) );
	}

	// addNext() -- Adds a sibling node imediately after Gthis node.
	public PGPNode addNext( PGPNode node ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.addNext("+node.flatName()+":"+node.value_+")");

		// The node is a sibling which has the same parent.
		node.parent_ = this.parent_;

		// Insert 'node' after this node
		node.next_  = this.next_;
		node.prev_  = this;
		next_.prev_ = node;
		next_       = node;

		return node;

	}//addNext()


	public PGPNode addAttr() {
		return addAttr( new PGPNode() );
	}
	public PGPNode addAttr( String name ) {
		return addAttr( new PGPNode( name ) );
	}
	public PGPNode addAttr( String name, String value ) {
		return addAttr( new PGPNode( name, value ) );
	}
	public PGPNode addAttr( String name, int number ) {
		return addAttr( name, Integer.toString(number), PGPDataType.NUMBER );
	}
	public PGPNode addAttr( String name, boolean b ) {
		return addAttr( name, Boolean.toString(b), PGPDataType.BOOLEAN );
	}
	public PGPNode addAttr( String name, String value, PGPDataType type ) {
		return addAttr( new PGPNode( name, value, type ) );
	}

	public PGPNode addAttr( PGPNode node ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.addAttr("+node.flatName()+":"+node.value_+")");

		node.parent_ = this;
		if ( headAttr_ != null ) {
			// Add to end of Attribute list.
			node.next_ = headAttr_;
			node.prev_ = headAttr_.prev_;
			node.next_.prev_ = node;
			node.prev_.next_ = node;
		} else {
			// Is first attribute
			// Since this is circular list, this lone node points to itself.
			headAttr_ = node.next_ = node.prev_ = node;
		}
		return node;
	}//addAttr()


	// Sets the node value from a source node.
	// This is useful for YAML alias references where the value
	// is copied from a source value to this value.
	// All attributes and children are copied over.
	// Everything from the source node is copied,
	// except for the name and parent is not copied.
	// The children of the source are recursively copied over.
	// returns this node.
	public PGPNode setValue( PGPNode srcNode ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.setValue("+srcNode.flatName()+")");

		// Copy over the Value and Type
		value_ = srcNode.value_;
		type_  = srcNode.type_;

		// go through and copy/clone the source attributes.
		PGPNode srcAttrNode = srcNode.headAttr_;
		if ( null != srcAttrNode )
		{
			do {
				addAttr( srcAttrNode.name_, srcAttrNode.value_, srcAttrNode.type_ );
				srcAttrNode = srcAttrNode.next_;
			} while ( srcAttrNode != srcNode.headAttr_ );
		}

		// go through the children and copy/clone them.
		PGPNode srcChildNode = srcNode.headChild_;
		if ( null != srcChildNode )
		{
			do {
				PGPNode childNode = addChild( srcChildNode.name_ );
				childNode.setValue( srcChildNode );
				srcChildNode = srcChildNode.next_;
			} while ( srcChildNode != srcNode.headChild_ );
		}

		return this;

	}//setValue(node)


	// findChild() -- look for a child node that matches the name.
	public PGPNode findChild( String name ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.findChild("+name+")");

		PGPNode node = headChild_;
		if ( null == node || null == name )
			return null;

		if ( name.equals(node.name_))
			return node;	// We found the node right off.

		PGPNode last = node.prev_;
		while ( node != last ) {
			node = node.next_;
			if ( name.equals(node.name_))
				return node;	// We found it.
		}
		return null;	// Not found.
	}//findChild()


	// findChild() -- look for a child node that matches the name.
	// findNode() -- does a deep search for a node using a flat name (dot notation)
	//    name -- uses dot name notation: like: "x.y.[4].z"
	// returns node or null if not found.
	public PGPNode findNode( String name ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.findNode("+name+")");

		return findNode( name.split(nameSeparator_) );
	}

	// findChild() -- look for a child node that matches the name.
	// findNode() -- does a deep search for a node using a flat name (dot notation)
	//    name -- uses dot name notation: like: "x.y.[4].z"
	// returns node or null if not found.
	public PGPNode findNode( String[] names ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.findNode("+names+")");

		// Go through each of the found names
		PGPNode node = this;

		for  (String n : names)
		{
			// See if this is an [index] value instead of a name.
			int nlen = n.length();
			if (   (nlen >= 3)
				&& (n.charAt(0) == '[')
				&& (n.charAt(nlen-1) == ']')
				&& Character.isDigit( n.charAt(1) )
			   )
			{
				// This is an index into an array.
				// Read the digits to make the index.
				int index=0;
				for (int i=1; i<(nlen-1); ++i) {
					char c = n.charAt(i);
					if ( !Character.isDigit(c) )
						break;
					index = (index*10) + (c - '0');
				}
				node = node.getChildAt(index);
			}
			else {
				node = node.findChild( n );
			}
			if ( null == node )
				return null;
		}
		return node;

	}//findNode


	// findNodeFromFullName()
	// Like getNodeFromFullName, but does not create missing nodes.
	// returns null if not found.
	public PGPNode findNodeFromFullName( String names ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.findNodeFromFullname(" + names + ")");

		return findNodeFromNameList( names.split(nameSeparator_) );
	}

	// findNodeFromFullName()
	// Like getNodeFromFullName, but does not create missing nodes.
	// returns null if not found.
	public PGPNode findNodeFromNameList( String[] names ) {

		if ( debug_level >= 2 )
			System.out.println("ENTRY: "+flatName()+"'.getNodeFromNameList(" + names + ")");

		// Go through each of the found names
		PGPNode node = this;

		for  (int iName=0; iName<names.length; ++iName)
		{
			String name = names[iName];

			// See if this is an [index] value instead of a name.
			int nlen = name.length();
			if (   (nlen >= 3)
				&& (name.charAt(0) == '[')
				&& (name.charAt(nlen-1) == ']')
				&& Character.isDigit( name.charAt(1) )
			   )
			{
				// This is an index into an array.
				// Read the digits to make the index.
				int index=0;
				for (int i=1; i<(nlen-1); ++i) {
					char c = name.charAt(i);
					if ( !Character.isDigit(c) )
						break;
					index = (index*10) + (c - '0');
				}
				// Index 0 is the first array element.
				if ( null == (node = node.headChild_) )
					return null;
				// Scan through till we have found the n'th sibling.
				for (int atIndex=1; atIndex<=index; ++atIndex) {
					if ( node.next_ == node.parent_.headChild_ )
						return null;
					node = node.next_;
				}
				// node is now the atIndex'th child node of the parent.
			}
			else {
				// Not an array index.
				// search through child nodes.
				if ( null == (node = node.headChild_) )
					return null;	// This node has no children, Not found then.
				// This node has children, search though that list.
				while ( true ) {	// Keep going till we find the end or the named node.
					String thisName = node.name_;
					if ( null != thisName && thisName.equals(name) )
						break; // found it.
					node = node.next_;
					if ( null == node.parent_.headChild_ )
						return null; // back at the first child again.
				}//while
				// node is now the found.
			}
			// node is found for the current name.
		}//for iName

		return node;

	}// findNodeFromFullName


	// getNodeFromFlatName() -- does an anchored search for a node using a flat name (dot notation)
	//    Creates the node (and parent nodes) if it doesn't exist.
	//    names -- can include array index values like [3]. dot name notation: like: "x.y.[4].z"
	// returns node.
	public PGPNode getNodeFromFlatName( String names ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: "+flatName()+"'.getNodeFromFullname(" + names + ")");
		return getNodeFromNameList( names.split(nameSeparator_) );
	}

	// getNodeFromNameList() -- does a anchored search for a node using array of names.
	//    Creates the node (and parent nodes) if any don't exist.
	//    names -- can include array index values like [3]. dot name notation: like: "x.y.[4].z"
	// returns node.
	public PGPNode getNodeFromNameList( String[] names ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: "+flatName()+"'.getNodeFromNameList(" + names + ")");

		// Go through each of the found names
		PGPNode node = this;

		for  (int iName=0; iName<names.length; ++iName)
		{
			String name = names[iName];

			// See if this is an [index] value instead of a name.
			int nlen = name.length();
			if (   (nlen >= 3)
				&& (name.charAt(0) == '[')
				&& (name.charAt(nlen-1) == ']')
				&& Character.isDigit( name.charAt(1) )
			   )
			{
				// This is an index into an array.
				// Read the digits to make the index.
				int index=0;
				for (int i=1; i<(nlen-1); ++i) {
					char c = name.charAt(i);
					if ( !Character.isDigit(c) )
						break;
					index = (index*10) + (c - '0');
				}
				// Index 0 is the first array element.
				if ( null == node.headChild_ ) {
					// Create [0] node
					node.setType( PGPDataType.ARRAY );	// This parent is now an ARRAY
					node = node.addChild();
				} else
					node = node.headChild_;
				// Scan through till we have found the n'th sibling.
				for (int atIndex=1; atIndex<=index; ++atIndex) {
					if ( node.next_ == node.parent_.headChild_ )
						node = node.parent_.addChild();
					else
						node = node.next_;
				}
				// node is now the n'th child node of the parent.
			}
			else {
				// search through child nodes. If not found, create it.
				if ( null == node.headChild_ ) {
					// This node has no children, so make one and use it.
					node = node.addChild( name );
				} else {
					// This node has children, search though that list.
					node = node.headChild_;
					while ( true ) {	// Keep going till we find the end or the named node.
						String thisName = node.name_;
						if ( null != thisName && thisName.equals(name) )
							break; // found it.
						node = node.next_;
						if ( node == node.parent_.headChild_ ) {
							// We are back at the first child again
							// so, lets add a child
							// But first, lets call the parent an OBJECT
							node.parent_.setType( PGPDataType.OBJECT );
							node = node.parent_.addChild( name );
							break;
						}
					}//while
					// node is now the found or created child node.
				}
			}
			// node is found for the current name.
		}//for iName

		return node;

	}//getNodeFromNameList


	public PGPNode addNode( String name ) {
		return setNode( name, null, PGPDataType.VARIANT );
	}
	public PGPNode setNode( String name, String val ) {
		return setNode( name, val, PGPDataType.VARIANT );
	}
	public PGPNode setNode( String name, int number ) {
		return setNode( name, Integer.toString(number), PGPDataType.NUMBER );
	}
	public PGPNode setNode( String name, boolean b ) {
		return setNode( name, Boolean.toString(b), PGPDataType.BOOLEAN );
	}
	public PGPNode setNode( String name, String val, PGPDataType type ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.setNode("+name+":"+val+")");

		PGPNode node = getNode(name);
		return (null==val) ? node.setType(type) : node.setValue( val ).setType(type);

	}//setNode


	// getNode - Like find, but will create missing nodes along the way.
	//    name -- uses dot name notation: like: "x.y.[4].z"
	// returns node. Always.
	//
	public PGPNode getNode( String name ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.getNode("+name+")");

		String[] names = name.split(nameSeparator_);

		// Go through each of the found names
		PGPNode node = this;
		PGPNode fnode;

		for (String n  : names) {
			// See if this is an [index] value instead of a name.
			int nlen = n.length();
			if (   (nlen >= 3)
				&& (n.charAt(0) == '[')
				&& (n.charAt(nlen-1) == ']')
				&& Character.isDigit( n.charAt(1) )
			   )
			{
				// This is an index into an array.
				// Read the digits to make the index.
				int index=0;
				for (int i=1; i<(nlen-1); ++i) {
					char c = n.charAt(i);
					if ( !Character.isDigit(c) )
						break;
					index = (index*10) + (c - '0');
				}
				fnode = node.getChildAt(index);
				if ( null == fnode ) {
					// Create array elements so we can get the requested index.
					int count = node.getChildCount();
					if ( count == 0 )
						node.type_ = PGPDataType.ARRAY;
					while ( count-1 < index ) {
						fnode = node.addChild();
						++count;
					}
				}
			}
			else {
				fnode = node.findChild( n );
			}
			if ( null == fnode )
				fnode = node.addChild( n, null,  PGPDataType.VARIANT );
			node = fnode;
		}//for n

		return node;

	}//getNode


	// findAttr() -- look for an attribute with the matching name.
	public PGPNode findAttr( String name ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.findAttr("+name+")");
		PGPNode node = headAttr_;
		if ( null == node )
			return null;

		if ( name.equals(node.name_))
			return node;	// We found the node right off.

		PGPNode last = node.prev_;
		while ( node != last ) {
			node = node.next_;
			if ( name.equals(node.name_))
				return node;	// We found the node right off.
		}
		return null;	// Not found.
	}//findAttr()

	// getChildCount() -- Counts the number of children a node has.
	//
	public int getChildCount() {
		PGPNode node = headChild_;
		if ( null == node )
			return 0;

		int     count = 1;
		PGPNode last  = node.prev_;
		while ( node != last ) {
			++count;
			node = node.next_;
		}

		return count;

	}//getChildCount()


	// indexOf() -- Returns the index of this node within its sibling list.
	// This node CANNOT be an Attribute node. If it is, time will stop.
	// returns 0..?
	public int indexOf() {
		if ( null == parent_ )
			return 0;
		// Scan through the children until we found ourself
		int index = 0;
		PGPNode node = parent_.headChild_;
		while ( node != this ) {
			node = node.next_;
			++index;
		}
		return index;
	}//indexOf


	// getChildAt() -- Get an item at the specified Index
	//   returns node at the specified index (0 is first node.)
	//           null returned if index is out of range.
	public PGPNode getChildAt( int index ) {
		if ( debug_level >= 2 )
			System.out.println("ENTRY: '"+flatName()+"'.getChildAt("+index+")");

		PGPNode node = headChild_;
		if ( null == node )
			return null;
		for ( int i=0; i<index; ++i )
		{
			node = node.next_;
			if ( node == headChild_ )
				return null;	// we looped before reaching the index.
		}
		return node;

	}//getChildAt()

	// Encode a string and append to a StringBuilder
	static public StringBuilder appendEncode( String s, StringBuilder sb ) {
		int len = s.length();
		for (int i=0; i<len; ++i) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
			case '\\':
				sb.append('\\').append(c);
				break;
			case '\n':
				sb.append('\\').append('n');
				break;
			case '\r':
				sb.append('\\').append('r');
				break;
			case '\t':
				sb.append('\\').append('t');
				break;
			case '\b':
				sb.append('\\').append('b');
				break;
			case '\f':
				sb.append('\\').append('f');
				break;
			default:
				sb.append(c);
			}
		}
		return sb;
	}//appendEncode()

	// appendValue() -- appends just the value to a StringBuilder object
	// returns the same StringBuilder object.
	//
	public StringBuilder appendValue( StringBuilder sb) {
		switch( type_ ) {
		case NULL:	sb.append("null");
					break;
		case STRING:sb.append('"');
					appendEncode(value_,sb).append('"');
					break;
		default:
					if ( null == value_ )
						sb.append("null");
					else
						sb.append(value_);
					break;
		}
		return sb;
	}//appendValue


	// fullName() -- Get the Address of this node (dot name notation)
	// Recursion is used to build the addresses of the parents.
	// returns String
	//         or a StringBuilder version is available as well.
	//
	public String flatName() {
		return appendFlatName( new StringBuilder() ).toString();
	}

	public StringBuilder appendFlatName( StringBuilder sb ) {
		if ( null == parent_)
			return sb;
		if ( null != parent_.parent_ )
		{
			//parent_.fullName(sb).append( nameSeparator_ );
			parent_.appendFlatName(sb).append( '.' );
		}
		if ( null != name_ )
			sb.append( name_ );
		else {
			int index = indexOf();
			sb.append( "["+index+"]" );
		}
		return sb;
	}//appendFlatName


	// detach() -- Unlinks the node from it's parent and siblings.
	@SuppressWarnings("unused")
	public PGPNode detach() {

		if ( null == this )
			return null;

		if ( null != parent_ ) {
			// This node has a parent. We could be an attribute or child.
			// We only care if we are the first of one of the lists.
			if ( parent_.headChild_ == this ) {
				// We are the head Child. Wiggle out of the list.
				if ( next_ == this )
					parent_.headChild_ = null;	// Parent is now child-less.
				else {
					parent_.headChild_ = next_;
					next_.prev_ = prev_;
					prev_.next_ = next_;
				}
			} else if ( parent_.headAttr_ == this ) {
				// We are the head Attribute. Wiggle out of the list.
				if ( next_ == this )
					parent_.headAttr_ = null;	// Parent is now attribute-less
				else {
					parent_.headAttr_ = next_;
					next_.prev_ = prev_;
					prev_.next_ = next_;
				}
			}
			else {
				// A child (or attribute) amongst others and not the first
				next_.prev_ = prev_;
				prev_.next_ = next_;
			}
		}//if has parent

		// Do the final internal detachment.
		parent_ = prev_ = next_ = null;

		return this;

	}//detach()


	// dump() -- helpful for debugging the node structure data
	// It recursively shows the current, attribute and children nodes.
	// Sibling nodes are shown by the parent

	public void dump() {
		setDepthSpaces(2);
		System.out.println( dump( new StringBuilder(500) ).toString() );
	}

	public void dump( int depthSpaces) {
		setDepthSpaces(depthSpaces);
		System.out.println( dump( new StringBuilder(500) ) );
	}

	public StringBuilder dump( StringBuilder sb ) {
		// This shows the top level node that may have siblings.
		// Normal recursion doesn't support the top level siblings.
		PGPNode node = this;
		dump( 0, sb );	// Show this node first
		if ( null != next_ ) {
			while ( (node = node.next_) != this )
				node.dump( 0, sb );
		}
		return sb;
	}//show

	public StringBuilder dump( int atDepth, StringBuilder sb ) {
		int d;

		if ( (null != parent_) && (this != parent_.headChild_) ) {
			// we are mid list
			sb.append(',');
		}

		if ( getDepthSpaces() > 0 ) {
			// Flush the current line if any.
			if ( sb.length() > 0 )
				sb.append('\n');

			// Show depth
			for (d=0; d<atDepth; ++d)
				sb.append( getDepthString() );
		}

		// Show the Name;
		if ( null != name_ )
			sb.append(name_).append(':');

		// Show the value;
		if ( null != value_ ) {
			appendValue(sb);
		}//if value_

		PGPNode node;

		// Show the Attributes
		// This assumes attributes have no children.
		if ( headAttr_ != null ) {
			sb.append( (getDepthSpaces()<=0)?",attr:{" : ", attr:{" );
			node = headAttr_;
			boolean isFirst = true;
			do {
				if ( isFirst ) isFirst = false;
				else sb.append(' ');
				sb.append(node.name_).append(':');
				node.appendValue(sb);
				node = node.next_;
			} while ( node != headAttr_ );
			sb.append("}");
		}//if headAttr_

		// Show The Children
		if ( null != headChild_ ) {
			switch( type_ ) {
			case ARRAY:	sb.append("[" );
						break;
			default:
						sb.append("{" );
						break;
			}

			node = headChild_;
			do {
				node.dump( atDepth+1, sb );
				node = node.next_;
			} while ( node != headChild_ );

			if ( getDepthSpaces() > 0 ) {
				// Flush sb buffer if not empty
				if ( sb.length() > 0 ) {
					sb.append('\n');
				}
			}

			// Show depth
			for (d=0; d<atDepth; ++d)
				sb.append( getDepthString() );
			switch( type_ ) {
			case ARRAY:	sb.append(']' );
						break;
			default:
						sb.append('}' );
						break;
			}

		}//if headChild_

		return sb;

	}//dump()


	public static void main( String[] args ) {
		debug_level = 0;	// Enable Tracing
		PGPNode topNode = new PGPNode();
		PGPNode node;

		topNode.setNode("[1].ee", "N\n T\t Q\" R\r /\\",PGPDataType.STRING );//.addNext("zz","ZEE");
		//topNode.setNode("aa.bb.[3].ff", "EFF" );
		//topNode.setNode("aa.bb.[0]", "zero" );
		topNode.dump();
		topNode.dump(0);
//		topNode.delete();
		node = topNode.addChild("isArray",null,PGPDataType.ARRAY);
		node.addChild(null,"index0");
		node.addChild(null,null,PGPDataType.OBJECT).addChild("sub1","val1").addNext("sub2","v2").addNext("sub3","third");
		topNode.addChild( "listX",null,PGPDataType.OBJECT).addChild("subObj","item1")
			   .addNext("subObj2",null,PGPDataType.OBJECT).addChild("subr","it'sDeep");
		node.addChild(null,"index1");
		topNode.setNode("aa.bb.[4].dd", "DEE" );
		topNode.dump();
		//topNode.free();

		topNode = new PGPNode(null,null,PGPDataType.ARRAY);
		topNode.setNode("a.b.c", "v_abc");
		topNode.setNode("a.b",   "v_ab");
		topNode.setNode( "z.y.x", null, PGPDataType.ARRAY).addChild(null,4).addNext(null,"for").addNext(null,true);
		topNode.dump();
		node = topNode.findNode("z.y").setValue("why");
		topNode.dump();

		topNode.setNode("N1a.N2a", "atN4a" );

		topNode.setNode("N1a.N2b.N3b.N4b", "atN4b" );
		topNode.setNode("N1a.N2b","atN2b").addAttr("type","frog").addNext("color","green");
		PGPNode array = topNode.setNode("P1a.P2b",null).setType(PGPDataType.ARRAY);
		array.addChild(null,"one").addNext(null,"2").addNext(null,"three");
		topNode.dump();
	}//main
}//PGPNode
