/*
 *  This software may only be used by you under license from AT&T Corp.
 *  ("AT&T").  A copy of AT&T's Source Code Agreement is available at
 *  AT&T's Internet website having the URL:
 *  <http://www.research.att.com/sw/tools/graphviz/license/source.html>
 *  If you received this software without first entering into a license
 *  with AT&T, you have an infringing copy of this software and cannot use
 *  it without violating AT&T's intellectual property rights.
 */

package att.grappa;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import att.grappa.util.IteratorEnumeration;

/**
 * This class describes a subgraph, which can consist of nodes, edges and other subgraphs. Note: The topmost or root
 * subgraph is the entire graph (the Graph object), which is an extension of this class.
 *
 * @version $Id$
 * @author <a href="mailto:john@research.att.com">John Mocenigo</a>, <a href="http://www.research.att.com">Research @
 *         AT&T Labs</a>
 * @see Graph
 */
public class Subgraph extends Element implements Comparator<Element>
{
    /**
     * Default graph name prefix used by setName().
     *
     * @see Subgraph#setName()
     */
    public final static String defaultNamePrefix = "G";

    // node, edge and graph dictionaries for this subgraph
    private Map<String, Node> nodedict = null;

    private Map<String, Edge> edgedict = null;

    private Map<String, Subgraph> graphdict = null;

    // indicators for displaying element labels when drawing
    private boolean nodeLabels = true;

    private boolean edgeLabels = true;

    private boolean subgLabels = true;

    // default node attributes
    private Map<String, Attribute> nodeAttributes = null;

    // default edge attributes
    private Map<String, Attribute> edgeAttributes = null;

    // for cluster subgraphs
    private boolean cluster = false;

    /**
     * Reference to the current selection (or vector of selections). Normally set and used by a GrappaAdapter.
     */
    public Object currentSelection = null;

    /**
     * This constructor is needed by the Graph constructor
     */
    Subgraph()
    {
        // super();
        this.cluster = true; // the root is a cluster subgraph

        subgraphAttrsOfInterest();
    }

    /**
     * Use this constructor when creating a subgraph within a subgraph.
     *
     * @param subg the parent subgraph.
     * @param name the name of this subgraph.
     */
    public Subgraph(Subgraph subg, String name)
    {
        super(SUBGRAPH, subg);
        setName(name);

        Enumeration<Attribute> enm = subg.getNodeAttributePairs();
        while (enm.hasMoreElements()) {
            setNodeAttribute(enm.nextElement());
        }
        enm = subg.getEdgeAttributePairs();
        while (enm.hasMoreElements()) {
            setEdgeAttribute(enm.nextElement());
        }
        enm = subg.getLocalAttributePairs();
        while (enm.hasMoreElements()) {
            setAttribute(enm.nextElement());
        }

        subgraphAttrsOfInterest();
    }

    /**
     * Use this constructor when creating a subgraph within a subgraph with an automatically generated name.
     *
     * @param subg the parent subgraph.
     * @see Subgraph#setName()
     */
    public Subgraph(Subgraph subg)
    {
        this(subg, (String) (null));
    }

    // a listing of the attributes of interest for Subgraphs
    private void subgraphAttrsOfInterest()
    {
        // attrOfInterest(BBOX_ATTR);
        attrOfInterest(MINBOX_ATTR);
        attrOfInterest(MINSIZE_ATTR);
        attrOfInterest(LABEL_ATTR);
        attrOfInterest(LP_ATTR);
        attrOfInterest(STYLE_ATTR);
    }

    /**
     * Check if this element is a subgraph. Useful for testing the subclass type of a Element object.
     *
     * @return true if this object is a Subgraph.
     */
    @Override
    public boolean isSubgraph()
    {
        return (true);
    }

    /**
     * Get the type of this element. Useful for distinguishing Element objects.
     *
     * @return the class variable constant SUBGRAPH.
     * @see GrappaConstants#SUBGRAPH
     */
    @Override
    public int getType()
    {
        return (SUBGRAPH);
    }

    /**
     * Generates and sets the name for this subgraph. The generated name is the concatenation of the
     * Subgraph.defaultNamePrefix with the numeric id of this subgraph Instance. Implements the abstract Element method.
     *
     * @see Element#getId()
     */
    @Override
    void setName()
    {
        String oldName = this.name;

        while (true) {
            this.name = Subgraph.defaultNamePrefix + getId() + "_" + System.currentTimeMillis();
            if (getGraph().findSubgraphByName(this.name) == null) {
                break;
            }
        }

        // update subgraph graph dictionary
        if (getSubgraph() != null) {
            if (oldName != null) {
                getSubgraph().removeSubgraph(oldName);
            }
            getSubgraph().addSubgraph(this);
        }

        this.canonName = null;
    }

    /**
     * Sets the subgraph name to a copy of the supplied argument. When the argument is null, setName() is called. When
     * the name is not unique or when the name has the same format as that generated by setName(), a
     * IllegalArgumentException is thrown.
     *
     * @param newName the new name for the subgraph.
     * @see Subgraph#setName()
     */
    public void setName(String newName) throws IllegalArgumentException
    {
        if (newName == null) {
            setName();
            return;
        }

        String oldName = this.name;

        // test if the new name is the same as the old name (if any)
        if (oldName != null && oldName.equals(newName)) {
            return;
        }

        // is name unique?
        if (getGraph().findSubgraphByName(newName) != null) {
            throw new IllegalArgumentException("graph name (" + newName + ") is not unique");
        }
        this.name = newName;

        if (this.name.startsWith("cluster")) {
            this.cluster = true;
        }

        // update subgraph graph dictionary
        if (getSubgraph() != null) {
            if (oldName != null) {
                getSubgraph().removeSubgraph(oldName);
            }
            getSubgraph().addSubgraph(this);
        }

        this.canonName = null;
    }

    /**
     * Check if the subgraph is a cluster subgraph.
     *
     * @return true, if the graph is a cluster subgraph.
     */
    public boolean isCluster()
    {
        return this.cluster;
    }

    /**
     * Check if the subgraph is the root of the graph.
     *
     * @return true, if the graph is the root of the graph.
     */
    public boolean isRoot()
    {
        return (this == getGraph());
    }

    /**
     * Gets the subgraph-specific default attribute for the named node attribute.
     *
     * @param key the name of the node attribute pair to be retrieved.
     * @return the requested attribute pair or null if not found.
     */
    public Attribute getNodeAttribute(String key)
    {
        if (this.nodeAttributes == null) {
            return (null);
        }
        return this.nodeAttributes.get(key);
    }

    /**
     * Gets the subgraph-specific default value for the named node attribute.
     *
     * @param key the name of the node attribute pair to be retrieved.
     * @return the requested attribute value or null if not found.
     */
    public Object getNodeAttributeValue(String key)
    {
        Attribute attr;
        if (this.nodeAttributes == null) {
            return (null);
        }
        if ((attr = this.nodeAttributes.get(key)) == null) {
            return (null);
        }
        return (attr.getValue());
    }

    /**
     * Gets an enumeration of the subgraph-specific node attribute keys
     *
     * @return an enumeration of String objects.
     */
    public Enumeration<String> getNodeAttributeKeys()
    {
        if (this.nodeAttributes == null) {
            return Collections.emptyEnumeration();
        }
        return new IteratorEnumeration<>(this.nodeAttributes.keySet().iterator());
    }

    /**
     * Gets an enumeration of the subgraph-specific node attributes
     *
     * @return an enumeration of Attribute objects.
     */
    public Enumeration<Attribute> getNodeAttributePairs()
    {
        if (this.nodeAttributes == null) {
            return Collections.emptyEnumeration();
        }
        return new IteratorEnumeration<>(this.nodeAttributes.values().iterator());
    }

    /**
     * Sets the subgraph-specific default for the specified node attribute. If the attribute is not from the parent
     * subgraph, then setNodeAttribute(attr.getName(), attr.getValue()) is called.
     *
     * @param attr the node Attribute object to set as a default.
     * @return the Attribute object previously stored for this attribute, if any.
     * @see Subgraph#setNodeAttribute(java.lang.String, java.lang.String)
     */
    public Object setNodeAttribute(Attribute attr)
    {
        if (attr == null) {
            return null;
        }
        if (this.nodeAttributes == null) {
            this.nodeAttributes = new Hashtable<>();
        }
        // check to see if attr is being passed down the subgraph chain
        Subgraph sg = getSubgraph();
        Attribute prntAttr = (sg == null) ? null : sg.getNodeAttribute(attr.getName());
        if (attr != prntAttr) {
            // it's not, so use the other method;
            // use getStringValue to make sure value is treated properly
            // when converted to an Object
            return setNodeAttribute(attr.getName(), attr.getStringValue());
        }
        Object oldValue = null;
        Attribute newAttr = null;
        Attribute crntAttr = getNodeAttribute(attr.getName());
        if (attr == crntAttr) {
            return attr.getValue();
        }
        if (crntAttr == null) {
            if (attr.getValue() == null) {
                return null;
            }
            this.nodeAttributes.put(attr.getName(), crntAttr = attr);
            // System.err.println("Adding passthru1 node attr("+attr.getName()+","+attr.getValue()+") to "+getName());
            // it's a pass down, so no need to set observers
        } else {
            oldValue = crntAttr.getValue();
            crntAttr.setChanged(); // so notifyObservers is sure to be called
            // it's a pass down, so pass it down
            this.nodeAttributes.put(attr.getName(), attr);
            // System.err.println("Adding passthru2 node attr("+attr.getName()+","+attr.getValue()+") to "+getName());
            // this is why we need notifyObservers called
            newAttr = attr;
        }
        // this should only be possible when "else" above has occurred
        if (crntAttr.hasChanged()) {
            crntAttr.notifyObservers(new Object[] { newAttr, new Long(System.currentTimeMillis()) });
        }
        return oldValue;
    }

    /**
     * Sets the subgraph-specific default using the specified name/value pair. A new attribute will be created if
     * needed.
     *
     * @param name the node attribute name
     * @param value the node attribute value
     * @return the Attribute object previously stored for this attribute, if any.
     */
    public Object setNodeAttribute(String name, Object value)
    {
        if (this.nodeAttributes == null) {
            this.nodeAttributes = new Hashtable<>();
        }
        if (name == null) {
            throw new IllegalArgumentException("cannot set an attribute using a null name");
        }
        // check to see if this name value is the same as the parent default
        Subgraph sg = getSubgraph();
        Attribute prntAttr = (sg == null) ? null : sg.getNodeAttribute(name);
        // if(prntAttr != null && value != null ) {
        // System.err.println("check new node attr ("+name+","+value+") against ("+prntAttr.getName()+","+prntAttr.getValue()+")");
        // if(name.equals(prntAttr.getName()) && value.equals(prntAttr.getValue())) {
        // it is, so call other form
        // System.err.println("set node attr to same as default ("+name+","+value+")");
        // return setNodeAttribute(prntAttr);
        // }
        // }
        Object oldValue = null;
        Attribute crntAttr = getNodeAttribute(name);
        if (crntAttr == null || crntAttr == prntAttr) {
            if (value == null) {
                return null;
            }
            this.nodeAttributes.put(name, (crntAttr = new Attribute(NODE, name, value)));
            // TODO: scan subnodes to see if this attr is of interest and then add it
            // to observer list, but for now leave it
            //
            // System.err.println("adding new node attr("+name+","+value+") to "+getName());
            /*
             * just concerned with subgraphs that share the same default (or null) and nodes that do not have a local
             * attribute
             */
        } else {
            oldValue = crntAttr.getValue();
            if (value == null) {
                if (prntAttr == null) {
                    removeNodeAttribute(name);
                    return oldValue;
                } else {
                    return setNodeAttribute(prntAttr);
                }
            } else {
                crntAttr.setValue(value);
                // System.err.println("changing node attr("+name+","+value+") in "+getName());
            }
        }
        if (crntAttr.hasChanged()) {
            crntAttr.notifyObservers(new Long(System.currentTimeMillis()));
        }
        return oldValue;
    }

    /*
     * Remove named default node attribute (assumes there is no default in the subgraph chain).
     * @param name the name of the attribute to remove
     */
    private void removeNodeAttribute(String name)
    {
        if (name == null || this.nodeAttributes == null) {
            return;
        }
        // System.err.println("Remove '" + name + "' from " + getName());
        Attribute attr = this.nodeAttributes.remove(name);
        if (attr == null) {
            return;
        }
        attr.setValue("");
        if (attr.hasChanged()) {
            attr.notifyObservers(new Long(System.currentTimeMillis()));
        }
        attr.deleteObservers();
    }

    /**
     * Sets the subgraph-specific default for the specified edge attribute. If the attribute is not from the parent
     * subgraph, then setEdgeAttribute(attr.getName(), attr.getValue()) is called.
     *
     * @param attr the edge attribute pair to set.
     * @return the attribute pair previously stored for this attribute.
     * @see Subgraph#setEdgeAttribute(java.lang.String, java.lang.String)
     */
    public Object setEdgeAttribute(Attribute attr)
    {
        if (attr == null) {
            return null;
        }
        if (this.edgeAttributes == null) {
            this.edgeAttributes = new Hashtable<>();
        }
        // check to see if attr is being passed down the subgraph chain
        Subgraph sg = getSubgraph();
        Attribute prntAttr = (sg == null) ? null : sg.getEdgeAttribute(attr.getName());
        if (attr != prntAttr) {
            // it's not, so use the other method;
            // use getStringValue to make sure value is treated properly
            // when converted to an Object
            return setEdgeAttribute(attr.getName(), attr.getStringValue());
        }
        Object oldValue = null;
        Attribute newAttr = null;
        Attribute crntAttr = getEdgeAttribute(attr.getName());
        if (attr == crntAttr) {
            return attr.getValue();
        }
        if (crntAttr == null) {
            if (attr.getValue() == null) {
                return null;
            }
            this.edgeAttributes.put(attr.getName(), crntAttr = attr);
            // System.err.println("Adding passthru1 edge attr("+attr.getName()+","+attr.getValue()+") to "+getName());
            // it's a pass down, so no need to set observers
        } else {
            oldValue = crntAttr.getValue();
            crntAttr.setChanged(); // so notifyObservers is sure to be called
            // it's a pass down, so pass it down
            this.edgeAttributes.put(attr.getName(), attr);
            // System.err.println("Adding passthru2 edge attr("+attr.getName()+","+attr.getValue()+") to "+getName());
            newAttr = attr;
        }
        // this should only be possible when "else" above has occurred
        if (crntAttr.hasChanged()) {
            crntAttr.notifyObservers(new Object[] { newAttr, new Long(System.currentTimeMillis()) });
        }
        return oldValue;
    }

    /**
     * Sets the subgraph-specific default using the specified name/value pair. A new attribute will be created if
     * needed.
     *
     * @param name the edge attribute name
     * @param value the edge attribute value
     * @return the attribute pair previously stored for this attribute.
     */
    public Object setEdgeAttribute(String name, Object value)
    {
        if (this.edgeAttributes == null) {
            this.edgeAttributes = new Hashtable<>();
        }
        if (name == null) {
            throw new IllegalArgumentException("cannot set an attribute using a null name");
        }
        // check to see if this name value is the same as the parent default
        Subgraph sg = getSubgraph();
        Attribute prntAttr = (sg == null) ? null : sg.getEdgeAttribute(name);
        // if(prntAttr != null && value != null ) {
        // if(name.equals(prntAttr.getName()) && value.equals(prntAttr.getValue())) {
        // it is, so call other form
        // return setEdgeAttribute(prntAttr);
        // }
        // }
        Object oldValue = null;
        Attribute crntAttr = getEdgeAttribute(name);
        if (crntAttr == null || crntAttr == prntAttr) {
            if (value == null) {
                return null;
            }
            this.edgeAttributes.put(name, (crntAttr = new Attribute(EDGE, name, value)));
            // System.err.println("adding new edge attr("+name+","+value+") to "+getName());
            /*
             * just concerned with subgraphs that share the same default (or null) and edges that do not have a local
             * attribute
             */
        } else {
            oldValue = crntAttr.getValue();
            if (value == null) {
                if (prntAttr == null) {
                    removeEdgeAttribute(name);
                    return oldValue;
                } else {
                    return setEdgeAttribute(prntAttr);
                }
            } else {
                crntAttr.setValue(value);
                // System.err.println("changing edge attr("+name+","+value+") in "+getName());
            }
        }
        if (crntAttr.hasChanged()) {
            crntAttr.notifyObservers(new Long(System.currentTimeMillis()));
        }
        return oldValue;
    }

    /*
     * Remove named default edge attribute (assumes there is no default in the subgraph chain).
     * @param name the name of the attribute to remove
     */
    private void removeEdgeAttribute(String name)
    {
        if (name == null || this.edgeAttributes == null) {
            return;
        }
        Attribute attr = this.edgeAttributes.remove(name);
        if (attr == null) {
            return;
        }
        attr.setValue("");
        if (attr.hasChanged()) {
            attr.notifyObservers(new Long(System.currentTimeMillis()));
        }
        attr.deleteObservers();
    }

    /**
     * Sets the subgraph-specific default for the specified graph attribute. If the attribute is not from the parent
     * subgraph, then setAttribute(attr.getName(), attr.getValue()) is called. Overrides Element method.
     *
     * @param attr the graph attribute pair to set.
     * @return the attribute pair previously stored for this attribute.
     * @see Subgraph#setAttribute(java.lang.String, java.lang.String)
     */
    @Override
    public Object setAttribute(Attribute attr)
    {
        if (attr == null) {
            return null;
        }
        if (this.attributes == null) {
            this.attributes = new Hashtable<>();
        }
        // check to see if attr is being passed down the subgraph chain
        Subgraph sg = getSubgraph();
        Attribute prntAttr = (sg == null) ? null : sg.getLocalAttribute(attr.getName());
        if (attr != prntAttr) {
            // it's not, so use the other method;
            // use getStringValue to make sure value is treated properly
            // when converted to an Object
            return setAttribute(attr.getName(), attr.getStringValue());
        }
        Object oldValue = null;
        Attribute newAttr = null;
        Attribute crntAttr = getLocalAttribute(attr.getName());
        if (attr == crntAttr) {
            return attr.getValue();
        }
        if (crntAttr == null) {
            if (attr.getValue() == null) {
                return null;
            }
            this.attributes.put(attr.getName(), crntAttr = attr);
            // System.err.println("Adding passthru1 graph attr("+attr.getName()+","+attr.getValue()+") to "+getName());
            // it's a pass down, so no need to set observers
        } else {
            oldValue = crntAttr.getValue();
            crntAttr.setChanged(); // so notifyObservers is sure to be called
            // it's a pass down, so pass it down
            this.attributes.put(attr.getName(), attr);
            // System.err.println("Adding passthru2 graph attr("+attr.getName()+","+attr.getValue()+") to "+getName());
            // this is why we need notifyObservers called
            newAttr = attr;
        }
        // this should only be possible when "else" above has occurred
        if (crntAttr.hasChanged()) {
            crntAttr.notifyObservers(new Object[] { newAttr, new Long(System.currentTimeMillis()) });
        }
        return oldValue;
    }

    /**
     * Sets the subgraph-specific default using the specified name/value pair. A new attribute will be created if
     * needed. Overrides Element method.
     *
     * @param name the graph attribute name
     * @param value the graph attribute value
     * @return the attribute pair previously stored for this attribute.
     */
    @Override
    public Object setAttribute(String name, Object value)
    {
        if (this.attributes == null) {
            this.attributes = new Hashtable<>();
        }
        if (name == null) {
            throw new IllegalArgumentException("cannot set an attribute using a null name");
        }
        // check to see if this name value is the same as the parent default
        Subgraph sg = getSubgraph();
        Attribute prntAttr = (sg == null) ? null : sg.getLocalAttribute(name);
        // if(prntAttr != null && value != null ) {
        // if(name.equals(prntAttr.getName()) && value.equals(prntAttr.getValue())) {
        // it is, so call other form
        // return setAttribute(prntAttr);
        // }
        // }
        Object oldValue = null;
        Attribute crntAttr = getLocalAttribute(name);
        if (crntAttr == null || crntAttr == prntAttr) {
            if (value == null) {
                return null;
            } else if (value instanceof String && ((String) value).trim().length() == 0
                && Attribute.attributeType(getType(), name) != STRING_TYPE) {
                return null;
            }
            this.attributes.put(name, (crntAttr = new Attribute(SUBGRAPH, name, value)));
            if (this.grappaNexus != null && isOfInterest(name)) {
                crntAttr.addObserver(this.grappaNexus);
            }

            // System.err.println("adding new graph attr("+name+","+value+") to "+getName());
        } else {
            oldValue = crntAttr.getValue();
            if (value == null) {
                if (prntAttr == null) {
                    // System.err.println("removing graph attr("+name+","+value+") in "+getName());
                    super.setAttribute(name, null);
                    return oldValue;
                } else {
                    // System.err.println("defaulting graph attr("+name+","+value+") in "+getName());
                    return setAttribute(prntAttr);
                }
            } else if (value instanceof String && ((String) value).trim().length() == 0
                && Attribute.attributeType(getType(), name) != STRING_TYPE) {
                if (prntAttr == null) {
                    // System.err.println("removing graph attr("+name+","+value+") in "+getName());
                    super.setAttribute(name, null);
                    return oldValue;
                } else {
                    // System.err.println("defaulting graph attr("+name+","+value+") in "+getName());
                    return setAttribute(prntAttr);
                }
            } else {
                crntAttr.setValue(value);
                // System.err.println("changing graph attr("+name+","+value+") in "+getName());
            }
        }
        if (crntAttr.hasChanged()) {
            crntAttr.notifyObservers(new Long(System.currentTimeMillis()));
        }
        return oldValue;
    }

    /**
     * Gets the subgraph-specific default attribute for the named edge attribute.
     *
     * @param key the name of the edge attribute pair to be retrieved.
     * @return the requested attribute pair or null if not found.
     */
    public Attribute getEdgeAttribute(String key)
    {
        if (this.edgeAttributes == null) {
            return (null);
        }
        return this.edgeAttributes.get(key);
    }

    /**
     * Gets the subgraph-specific default value for the named edge attribute.
     *
     * @param key the name of the edge attribute pair to be retrieved.
     * @return the requested attribute value or null if not found.
     */
    public Object getEdgeAttributeValue(String key)
    {
        Attribute attr;
        if (this.edgeAttributes == null) {
            return (null);
        }
        if ((attr = this.edgeAttributes.get(key)) == null) {
            return (null);
        }
        return (attr.getValue());
    }

    /**
     * Gets an enumeration of the subgraph-specific edge attribute keys
     *
     * @return an enumeration of String objects.
     */
    public Enumeration<String> getEdgeAttributeKeys()
    {
        if (this.edgeAttributes == null) {
            return Collections.emptyEnumeration();
        }
        return new IteratorEnumeration<>(this.edgeAttributes.keySet().iterator());
    }

    /**
     * Gets an enumeration of the subgraph-specific edge attributes
     *
     * @return an enumeration of Attribute objects.
     */
    public Enumeration<Attribute> getEdgeAttributePairs()
    {
        if (this.edgeAttributes == null) {
            return Collections.emptyEnumeration();
        }
        return new IteratorEnumeration<>(this.edgeAttributes.values().iterator());
    }

    /**
     * Get the bounding box of the subgraph.
     *
     * @return the bounding box of the subgraph.
     */
    public java.awt.geom.Rectangle2D getBoundingBox()
    {
        java.awt.geom.Rectangle2D bbox = null;
        if (this.grappaNexus == null || (bbox = this.grappaNexus.bbox) == null) {
            if (this.grappaNexus == null) {
                buildShape();
            }
            bbox = null;
            Element elem = null;
            GraphEnumeration enm = elements();
            while (enm.hasMoreElements()) {
                elem = enm.nextGraphElement();
                if (elem == this) {
                    continue;
                }
                switch (elem.getType()) {
                    case NODE:
                    case EDGE:
                        elem.buildShape();
                        if (bbox == null) {
                            bbox = elem.grappaNexus.getBounds2D();
                        } else {
                            bbox.add(elem.grappaNexus.getBounds2D());
                        }
                        break;
                    case SUBGRAPH:
                        if (bbox == null) {
                            bbox = ((Subgraph) elem).getBoundingBox();
                        } else {
                            bbox.add(((Subgraph) elem).getBoundingBox());
                        }
                        break;
                    default: // cannot happen
                        throw new InternalError("unknown type (" + elem.getType() + ")");
                }
            }
            GrappaSize minSize = (GrappaSize) getAttributeValue(MINSIZE_ATTR);
            if (minSize != null) {
                if (bbox == null) {
                    bbox = new java.awt.geom.Rectangle2D.Double(0, 0, minSize.getWidth(), minSize.getHeight());
                } else {
                    bbox.add(new java.awt.geom.Rectangle2D.Double(bbox.getCenterX() - (minSize.getWidth() / 2.0), bbox
                        .getCenterY() - (minSize.getHeight() / 2.0), minSize.getWidth(), minSize.getHeight()));
                }
            }
            GrappaBox minBox = (GrappaBox) getThisAttributeValue(MINBOX_ATTR);
            if (minBox != null) {
                if (bbox == null) {
                    bbox = new java.awt.geom.Rectangle2D.Double(minBox.x, minBox.y, minBox.width, minBox.height);
                } else {
                    bbox.add(new java.awt.geom.Rectangle2D.Double(minBox.x, minBox.y, minBox.width, minBox.height));
                }
            }
            minBox = (GrappaBox) getThisAttributeValue(BBOX_ATTR);
            if (minBox != null) {
                if (bbox == null) {
                    bbox = new java.awt.geom.Rectangle2D.Double(minBox.x, minBox.y, minBox.width, minBox.height);
                } else {
                    bbox.add(new java.awt.geom.Rectangle2D.Double(minBox.x, minBox.y, minBox.width, minBox.height));
                }
            }
            if (bbox == null) {
                bbox = new java.awt.geom.Rectangle2D.Double();
            }
            bbox.add(bbox.getX() + bbox.getWidth() + 1, bbox.getY() + bbox.getHeight() + 1);

            this.grappaNexus.bbox = bbox;
            if (Grappa.provideBBoxAttribute) {
                setAttribute(BBOX_ATTR, new GrappaBox(bbox));
            }
            this.grappaNexus.updateShape();
        }
        return ((java.awt.geom.Rectangle2D) (bbox.clone()));
    }

    /**
     * Removes bounding box information from this subgraph and any contained subgraphs including the BBOX_ATTR value and
     * then recomputes the bounding boxes.
     *
     * @return the new bounding box of the subgraph.
     */
    public java.awt.geom.Rectangle2D resetBoundingBox()
    {
        Element elem = null;
        GraphEnumeration enm = elements(SUBGRAPH);
        while (enm.hasMoreElements()) {
            elem = enm.nextGraphElement();
            elem.grappaNexus.bbox = null;
            elem.setAttribute(BBOX_ATTR, null);
        }
        return (getBoundingBox());
    }

    /**
     * Prints an ascii description of each graph element to the supplied stream.
     *
     * @param output the OutputStream for writing the graph description.
     */
    public void printSubgraph(PrintWriter out)
    {
        Graph graph = getGraph();
        String indent = new String(graph.getIndent());

        if (Grappa.printVisibleOnly && (!this.visible || this.grappaNexus.style.invis)) {
            return;
        }

        if (getSubgraph() == null) {
            // this subgraph is the root
            out.println(indent + (graph.isStrict() ? "strict " : "") + (graph.isDirected() ? "digraph" : "graph") + " "
                + graph.toString() + " {");
        } else if (getName().startsWith(ANONYMOUS_PREFIX)) {
            out.println(indent + "{");
        } else {
            out.println(indent + "subgraph " + this.toString() + " {");
        }

        graph.incrementIndent();

        printDflt(out, SUBGRAPH);
        printDflt(out, NODE);
        printDflt(out, EDGE);

        if (this.graphdict != null && !this.graphdict.isEmpty()) {
            for (Subgraph sg : this.graphdict.values()) {
                sg.printSubgraph(out);
            }
        }

        if (this.nodedict != null && !this.nodedict.isEmpty()) {
            for (Node n : this.nodedict.values()) {
                n.printNode(out);
            }
        }

        if (this.edgedict != null && !this.edgedict.isEmpty()) {
            for (Edge e : this.edgedict.values()) {
                e.printEdge(out);
            }
        }

        graph.decrementIndent();

        out.println(indent + "}");
    }

    // print the subgraph default elements
    private void printDflt(PrintWriter out, int type)
    {
        String indent = new String(getGraph().getIndent());
        Map<String, Attribute> attr = null;
        String label = null;

        switch (type) {
            case SUBGRAPH:
                attr = this.attributes;
                label = "graph";
                break;
            case NODE:
                attr = this.nodeAttributes;
                label = "node";
                break;
            case EDGE:
                attr = this.edgeAttributes;
                label = "edge";
                break;
        }

        if (attr == null || attr.isEmpty()) {
            getGraph().printError("no " + label + " atrtibutes for " + getName());
            return;
        }

        getGraph().incrementIndent();
        printDfltAttr(out, attr, type, indent + label + " [", indent + "];");
        getGraph().decrementIndent();
    }

    // print the subgraph default element attribute values
    private void printDfltAttr(PrintWriter out, Map<String, Attribute> dfltAttr, int type, String prefix, String suffix)
    {
        String indent = new String(getGraph().getIndent());
        String value;
        String key;
        int nbr = 0;
        Subgraph sg = getSubgraph();
        Hashtable<String, String> printlist = null;

        if (type == SUBGRAPH && (Grappa.usePrintList || usePrintList)) {
            printlist = (Hashtable<String, String>) getAttributeValue(PRINTLIST_ATTR);
        }

        for (Attribute attr : dfltAttr.values()) {
            if (attr == null) {
                continue;
            }
            key = attr.getName();
            if (printlist != null && printlist.get(key) == null) {
                continue;
            }
            value = attr.getStringValue();
            if (Grappa.elementPrintAllAttributes || Grappa.elementPrintDefaultAttributes || this.printAllAttributes
                || this.printDefaultAttributes || !attr.equalsValue(getParentDefault(type, key))) {
                nbr++;
                if (nbr == 1) {
                    out.println(prefix);
                    out.print(indent + key + " = " + canonString(value));
                } else {
                    out.println(",");
                    out.print(indent + key + " = " + canonString(value));
                }
            }
        }
        if (nbr > 0) {
            out.println();
            out.println(suffix);
            out.println();
        }
    }

    /**
     * Returns the attribute conversion type for the supplied attribute name. After subgraph specific attribute
     * name/type mappings are checked, mappings at the element level are checked.
     *
     * @param attrname the attribute name
     * @return the currently associated attribute type
     */
    public static int attributeType(String attrname)
    {
        int convtype = -1;
        int hashCode;

        if (attrname != null) {
            hashCode = attrname.hashCode();

            if (hashCode == MARGIN_HASH && attrname.equals(MARGIN_ATTR)) {
                convtype = SIZE_TYPE;
            } else if (hashCode == MCLIMIT_HASH && attrname.equals(MCLIMIT_ATTR)) {
                convtype = DOUBLE_TYPE;
            } else if (hashCode == MINBOX_HASH && attrname.equals(MINBOX_ATTR)) {
                convtype = BOX_TYPE;
            } else if (hashCode == NODESEP_HASH && attrname.equals(NODESEP_ATTR)) {
                convtype = DOUBLE_TYPE;
            } else if (hashCode == MINSIZE_HASH && attrname.equals(MINSIZE_ATTR)) {
                convtype = SIZE_TYPE;
            } else if (hashCode == NODESEP_HASH && attrname.equals(NODESEP_ATTR)) {
                convtype = DOUBLE_TYPE;
            } else if (hashCode == RANKSEP_HASH && attrname.equals(RANKSEP_ATTR)) {
                convtype = DOUBLE_TYPE;
            } else if (hashCode == SIZE_HASH && attrname.equals(SIZE_ATTR)) {
                convtype = SIZE_TYPE;
            } else {
                return (Element.attributeType(attrname));
            }
        }

        return (convtype);
    }

    // get the parent default attribute value for the specified type and key
    private Attribute getParentDefault(int type, String key)
    {
        Attribute attr = null;
        Subgraph subg = getSubgraph();
        switch (type) {
            case SUBGRAPH:
                while (subg != null && (attr = subg.getLocalAttribute(key)) == null) {
                    subg = subg.getSubgraph();
                }
                if (attr == null) {
                    getGraph();
                    attr = Graph.getGlobalAttribute(SUBGRAPH, key);
                }
                return attr;
            case NODE:
                while (subg != null && (attr = subg.getNodeAttribute(key)) == null) {
                    subg = subg.getSubgraph();
                }
                if (attr == null) {
                    getGraph();
                    attr = Graph.getGlobalAttribute(NODE, key);
                }
                return attr;
            case EDGE:
                while (subg != null && (attr = subg.getEdgeAttribute(key)) == null) {
                    subg = subg.getSubgraph();
                }
                if (attr == null) {
                    getGraph();
                    attr = Graph.getGlobalAttribute(EDGE, key);
                }
                return attr;
        }
        return null;
    }

    /*
     * Find an Element by name.
     * @param type the type of the element
     * @param name the name of the element
     * @return the found element or null
     * @see Subgraph#findNodeByName(java.lang.String)
     * @see Subgraph#findEdgeByName(java.lang.String)
     * @see Subgraph#findSubgraphByName(java.lang.String)
     */
    private Element findElementByName(int type, String name)
    {
        if (name == null) {
            return (null);
        }

        return findElementInSubgraphByName(type, name);
    }

    // used above
    private Element findElementInSubgraphByName(int type, String name)
    {
        Element elem = null;

        switch (type) {
            case NODE:
                if (this.nodedict != null) {
                    elem = this.nodedict.get(name);
                }
                break;
            case EDGE:
                if (this.edgedict != null) {
                    elem = this.edgedict.get(name);
                }
                break;
            case SUBGRAPH:
                if (this.graphdict != null) {
                    elem = this.graphdict.get(name);
                }
                break;
        }

        if (elem != null || this.graphdict == null) {
            return elem;
        }

        for (Subgraph sg : this.graphdict.values()) {
            if ((elem = sg.findElementInSubgraphByName(type, name)) != null) {
                return elem;
            }
        }

        return elem;
    }

    /**
     * Searches current subgraph and, by recursion, descendent subgraphs for the node matching the supplied name.
     *
     * @param nodeName the name of the node to be found.
     * @return the Node matching the name or null, if there is no match.
     */
    public Node findNodeByName(String nodeName)
    {
        return (Node) findElementByName(NODE, nodeName);
    }

    /**
     * Searches current subgraph and, by recursion, descendent subgraphs for the edge matching the supplied name.
     *
     * @param edgeName the name of the edge to be found.
     * @return the Edge matching the name or null, if there is no match.
     */
    public Edge findEdgeByName(String edgeName)
    {
        return (Edge) findElementByName(EDGE, edgeName);
    }

    /**
     * Searches current subgraph and, by recursion, descendent subgraphs for the subgraph matching the supplied name.
     *
     * @param graphName the name of the subgraph to be found.
     * @return the Subgraph matching the name or null, if there is no match.
     */
    public Subgraph findSubgraphByName(String graphName)
    {
        return (Subgraph) findElementByName(SUBGRAPH, graphName);
    }

    /**
     * Creates a new element and adds it to the subgraph's element dictionary. For nodes, the <I>info</I> vector can be
     * null or contains:
     * <ul>
     * <li>String - name of the node (optional, for automatic name generation)
     * </ul>
     * For edges, the <I>info</I> vector must contain (in this order) at least:
     * <ul>
     * <li>Node - head node,
     * <li>String - headport tag (or null),
     * <li>Node - tail node,
     * </ul>
     * Optionally, the <I>info</I> vector can also contain at its end (in this order):
     * <ul>
     * <li>String - tailport tag (or null),
     * <li>String - a key for distinguishing multiple edges between the same nodes (or null),
     * </ul>
     * For subgraphs, the <I>info</I> vector can be null or contains:
     * <ul>
     * <li>String - name of the subgraph (optional, for automatic name generation)
     * </ul>
     *
     * @param type type of the element to be added
     * @param info a vector specifics for the particular type of element being created
     * @param attrs attributes describing the element to be created
     * @exception InstantiationException whenever element cannot be created
     */
    public Element createElement(int type, Object[] info, Attribute[] attrs)
    {
        Element elem = null;

        switch (type) {
            case NODE:
                String nodeName = null;
                if (info != null && info.length >= 1) {
                    nodeName = (String) info[0];
                }
                Node node = new Node(this, nodeName);
                if (attrs != null) {
                    for (Attribute attr : attrs) {
                        node.setAttribute(attr);
                    }
                }
                elem = node;
                break;
            case EDGE:
                if (info == null || info.length < 3) {
                    throw new IllegalArgumentException("insufficient info supplied for edge creation");
                }
                Node head = (Node) info[0];
                String headPort = (String) info[1];
                Node tail = (Node) info[2];
                String tailPort = null;
                String key = null;
                if (info.length > 3) {
                    tailPort = (String) info[3];
                    if (info.length > 4) {
                        key = (String) info[4];
                    }
                }
                Edge edge = new Edge(this, tail, tailPort, head, headPort, key);
                if (attrs != null) {
                    for (Attribute attr : attrs) {
                        edge.setAttribute(attr);
                    }
                }
                elem = edge;
                break;
            case SUBGRAPH:
                String subgName = null;
                if (info != null && info.length >= 1) {
                    subgName = (String) info[0];
                }
                Subgraph newSubg = new Subgraph(this, subgName);
                if (attrs != null) {
                    for (Attribute attr : attrs) {
                        newSubg.setAttribute(attr);
                    }
                }
                elem = newSubg;
                break;
            default:
                return null;
        }
        return elem;
    }

    /**
     * Adds the specified node to the subgraph's Node dictionary.
     *
     * @param newNode the node to be added to the dictionary.
     */
    public void addNode(Node newNode)
    {
        if (newNode == null) {
            return;
        }
        if (this.nodedict == null) {
            this.nodedict = new HashMap<>();
        }
        this.nodedict.put(newNode.getName(), newNode);
    }

    /**
     * Removes the node matching the specified name from the subgraph's Node dictionary.
     *
     * @param nodeName the name of the node to be removed from the dictionary.
     * @return the node that was removed.
     */
    public Node removeNode(String nodeName)
    {
        if (this.nodedict == null) {
            return (null);
        }
        return this.nodedict.remove(nodeName);
    }

    /**
     * Adds the specified edge to the subgraph's Edge dictionary.
     *
     * @param newEdge the edge to be added to the dictionary.
     */
    public void addEdge(Edge newEdge)
    {
        if (newEdge == null) {
            return;
        }
        if (this.edgedict == null) {
            this.edgedict = new HashMap<>();
        }
        this.edgedict.put(newEdge.getName(), newEdge);
    }

    /**
     * Removes the edge matching the specified name from the subgraph's Edge dictionary.
     *
     * @param edgeName the name of the edge to be removed from the dictionary.
     * @return the edge that was removed.
     */
    public Edge removeEdge(String edgeName)
    {
        if (this.edgedict == null) {
            return (null);
        }
        return this.edgedict.remove(edgeName);
    }

    /**
     * Adds the specified subgraph to the subgraph's graph dictionary.
     *
     * @param newGraph the subgraph to be added to the dictionary.
     */
    public void addSubgraph(Subgraph newGraph)
    {
        if (newGraph == null) {
            return;
        }
        if (this.graphdict == null) {
            this.graphdict = new HashMap<>();
        }
        this.graphdict.put(newGraph.getName(), newGraph);
    }

    /**
     * Removes the subgraph matching the specified name from the subgraph's graph dictionary.
     *
     * @param graphName the name of the subgraph to be removed from the dictionary.
     * @return the subgraph that was removed.
     */
    public Subgraph removeSubgraph(String graphName)
    {
        if (this.graphdict == null) {
            return (null);
        }
        return this.graphdict.remove(graphName);
    }

    /**
     * Set flag to indicate if subgraph labels should be rendered
     *
     * @return the previous value
     */
    public boolean setShowSubgraphLabels(boolean value)
    {
        boolean oldValue = this.subgLabels;
        this.subgLabels = value;
        return (oldValue);
    }

    /**
     * Set flag to indicate if node labels should be rendered
     *
     * @return the previous value
     */
    public boolean setShowNodeLabels(boolean value)
    {
        boolean oldValue = this.nodeLabels;
        this.nodeLabels = value;
        return (oldValue);
    }

    /**
     * Set flag to indicate if edge labels should be rendered
     *
     * @return the previous value
     */
    public boolean setShowEdgeLabels(boolean value)
    {
        boolean oldValue = this.edgeLabels;
        this.edgeLabels = value;
        return (oldValue);
    }

    /**
     * Get flag that indicates if subgraph labels should be rendered
     *
     * @return the flag value
     */
    public boolean getShowSubgraphLabels()
    {
        return (this.subgLabels);
    }

    /**
     * Get flag that indicates if node labels should be rendered
     *
     * @return the flag value
     */
    public boolean getShowNodeLabels()
    {
        return (this.nodeLabels);
    }

    /**
     * Get flag that indicates if edge labels should be rendered
     *
     * @return the flag value
     */
    public boolean getShowEdgeLabels()
    {
        return (this.edgeLabels);
    }

    /**
     * Check if the orientation of this subgraph is LR (left-to-right) as opposed to TB (top-to-bottom).
     *
     * @return true if the orientation is left-to-right.
     */
    public boolean isLR()
    {
        Attribute attr = getAttribute("rankdir");

        if (attr == null)
        {
            return false; // the default
        }
        String value = attr.getStringValue();
        if (value == null)
        {
            return false; // the default
        }
        if (value.equals("LR")) {
            return true;
        }
        return false;
    }

    /**
     * Adds a default tag for the specified element type within this subgraph.
     *
     * @param type the element type for this tag operation
     * @param tag the tag to associate with this element type.
     */
    public void addTypeTag(int type, String tag)
    {
        if (tag == null || tag.indexOf(',') >= 0) {
            throw new RuntimeException("tag value null or contains a comma (" + tag + ")");
        }
        Attribute attr = null;
        Hashtable<String, String> tags;
        switch (type) {
            case NODE:
                attr = getNodeAttribute(TAG_ATTR);
                break;
            case EDGE:
                attr = getEdgeAttribute(TAG_ATTR);
                break;
            case SUBGRAPH:
                attr = getLocalAttribute(TAG_ATTR);
                break;
        }
        if (attr == null) {
            attr = new Attribute(type, TAG_ATTR, new Hashtable<String, String>());
            setAttribute(attr);
            switch (type) {
                case NODE:
                    setNodeAttribute(attr);
                    break;
                case EDGE:
                    setEdgeAttribute(attr);
                    break;
                case SUBGRAPH:
                    setAttribute(attr);
                    break;
            }
        }
        tags = (Hashtable<String, String>) (attr.getValue());

        tags.put(tag, tag);
        // if it becomes desireable to retain the original order, we
        // could always use the value in the following (instead of
        // what is done above) to reconstruct the original order
        // (Note that no code makes use of the value at this point,
        // so that would all have to be added in printAttributes, for
        // example)
        // tags.put(tag,new Long(System.currentTimeMillis()));
    }

    /**
     * Check if the specified element type has the supplied default tag within this subgraph.
     *
     * @param type the element type for this tag operation
     * @param tag tag value to be searched for
     * @return true, if this subgraph contains the supplied tag as a default for the given type
     */
    public boolean hasTypeTag(int type, String tag)
    {
        Attribute attr = null;
        Hashtable<String, String> tags;
        switch (type) {
            case NODE:
                attr = getNodeAttribute(TAG_ATTR);
                break;
            case EDGE:
                attr = getEdgeAttribute(TAG_ATTR);
                break;
            case SUBGRAPH:
                attr = getLocalAttribute(TAG_ATTR);
                break;
        }
        if (attr == null) {
            return false;
        }
        tags = (Hashtable<String, String>) (attr.getValue());
        if (tags == null || tags.size() == 0) {
            return false;
        }
        return (tags.containsKey(tag));
    }

    /**
     * Check if this element type has any default tags at all.
     *
     * @param type the element type for this tag operation
     * @return true, if this Element has any tags
     */
    public boolean hasTypeTags(int type)
    {
        Attribute attr = null;
        Hashtable<String, String> tags;
        switch (type) {
            case NODE:
                attr = getNodeAttribute(TAG_ATTR);
                break;
            case EDGE:
                attr = getEdgeAttribute(TAG_ATTR);
                break;
            case SUBGRAPH:
                attr = getLocalAttribute(TAG_ATTR);
                break;
        }
        if (attr == null) {
            return false;
        }
        tags = (Hashtable<String, String>) (attr.getValue());
        if (tags == null || tags.size() == 0) {
            return false;
        }
        return (true);
    }

    /**
     * Removes any and all default tags associated with this element type.
     *
     * @param type the element type for this tag operation
     */
    public void removeTypeTags(int type)
    {
        Attribute attr = null;
        Hashtable<String, String> tags;
        switch (type) {
            case NODE:
                attr = getNodeAttribute(TAG_ATTR);
                break;
            case EDGE:
                attr = getEdgeAttribute(TAG_ATTR);
                break;
            case SUBGRAPH:
                attr = getLocalAttribute(TAG_ATTR);
                break;
        }
        if (attr == null) {
            return;
        }
        tags = (Hashtable) (attr.getValue());
        if (tags == null || tags.size() == 0) {
            return;
        }
        tags.clear();
    }

    /**
     * Removes the specified tag from this element.
     *
     * @param type the element type for this tag operation
     * @param tag the tag value to remove
     */
    public void removeTypeTag(int type, String tag)
    {
        Attribute attr = null;
        Hashtable<String, String> tags;
        switch (type) {
            case NODE:
                attr = getNodeAttribute(TAG_ATTR);
                break;
            case EDGE:
                attr = getEdgeAttribute(TAG_ATTR);
                break;
            case SUBGRAPH:
                attr = getLocalAttribute(TAG_ATTR);
                break;
        }
        if (attr == null) {
            return;
        }
        tags = (Hashtable<String, String>) (attr.getValue());
        if (tags == null || tags.size() == 0) {
            return;
        }
        tags.remove(tag);
    }

    /**
     * Get a count of elements in this subgraph. No recursion to descendants is done.
     *
     * @param types a bitwise-oring of NODE, EDGE, SUBGRAPH to determine which element types should be in the count
     * @return a count of the specified elements in this subgraph.
     * @see GrappaConstants#NODE
     * @see GrappaConstants#EDGE
     * @see GrappaConstants#SUBGRAPH
     */
    public int countOfLocalElements(int types)
    {
        int count = 0;
        if ((types & NODE) != 0 && this.nodedict != null) {
            count += this.nodedict.size();
        }
        if ((types & EDGE) != 0 && this.edgedict != null) {
            count += this.edgedict.size();
        }
        if ((types & SUBGRAPH) != 0 && this.graphdict != null) {
            count += this.graphdict.size();
        }
        return count;
    }

    /**
     * Get a count of elements in this subgraph and, by recursion, descendant subgraphs. The subgraph itself is not
     * counted.
     *
     * @param types a bitwise-oring of NODE, EDGE, SUBGRAPH to determine which element types should be in the count
     * @return a count of the specified elements in this subgraph and its descendants.
     * @see GrappaConstants#NODE
     * @see GrappaConstants#EDGE
     * @see GrappaConstants#SUBGRAPH
     */
    public int countOfElements(int types)
    {
        int count = 0;
        if ((types & NODE) != 0 && this.nodedict != null) {
            count += this.nodedict.size();
        }
        if ((types & EDGE) != 0 && this.edgedict != null) {
            count += this.edgedict.size();
        }
        if (this.graphdict != null) {
            if ((types & SUBGRAPH) != 0) {
                count += this.graphdict.size();
            }
            for (Subgraph sg : this.graphdict.values()) {
                count += sg.countOfElements(types);
            }
        }
        return count;
    }

    /**
     * Delete this subgraph or any contained subgraph, at any depth, if the subgraph contains no elements.
     */
    public void removeEmptySubgraphs()
    {
        if ((this.graphdict == null || this.graphdict.size() == 0)
            &&
            (this.nodedict == null || this.nodedict.size() == 0)
            &&
            (this.edgedict == null || this.edgedict.size() == 0)) {
            delete();
            return;
        }
        if (this.graphdict != null) {
            for (Subgraph sg : this.graphdict.values()) {
                sg.removeEmptySubgraphs();
            }
        }
    }

    /**
     * @return true if this subgraph or any subgraph contained within this subgraph, at any depth, is empty.
     */
    public boolean hasEmptySubgraphs()
    {
        if ((this.graphdict == null || this.graphdict.size() == 0)
            &&
            (this.nodedict == null || this.nodedict.size() == 0)
            &&
            (this.edgedict == null || this.edgedict.size() == 0)) {
            return (true);
        }
        if (this.graphdict != null) {
            for (Subgraph sg : this.graphdict.values()) {
                if (sg.hasEmptySubgraphs()) {
                    return (true);
                }
            }
        }
        return (false);
    }

    //
    // Start PatchWork (similar to TreeMap) stuff
    //

    private double PATCHEDGE = 2;

    private double PATCHEDGE2 = 2.0 * this.PATCHEDGE;

    private Element[] sgPatches = null;

    private Element[] elPatches = null;

    public void clearPatchWork()
    {

        prepPatchWork(null, -1);
    }

    public void patchWork(java.awt.geom.Rectangle2D.Double r, boolean square, int mode)
    {

        preparePatchWork(mode);
        computePatchWork(r instanceof GrappaBox ? r : new GrappaBox(r), square);
        if (mode == 0) {
            Subgraph sg;
            String style;
            Attribute attr;
            Enumeration<Element> enm = elements(GrappaConstants.SUBGRAPH);
            while (enm.hasMoreElements()) {
                sg = (Subgraph) (enm.nextElement());
                if (sg != this) {
                    attr = sg.getAttribute(STYLE_ATTR);
                    if (attr != null) {
                        style = attr.getStringValue();
                        sg.setAttribute(STYLE_ATTR, style != null && style.length() > 0 ? style + ",filled(false)"
                            : null);
                    }
                }
            }
        } else {
            float sgtot = countOfElements(GrappaConstants.SUBGRAPH) - 2;
            float nbr = 0;
            Subgraph sg;
            String style;
            Attribute attr;
            Enumeration<Element> enm = elements(GrappaConstants.SUBGRAPH);
            while (enm.hasMoreElements()) {
                sg = (Subgraph) (enm.nextElement());
                if (sg != this) {
                    sg.setAttribute(COLOR_ATTR,
                        java.awt.Color.getHSBColor((float) (0.05 + 0.9 * (nbr++ / sgtot)), (float) 1.0, (float) 1.0));
                    attr = sg.getAttribute(STYLE_ATTR);
                    if (attr == null) {
                        sg.setAttribute(STYLE_ATTR, "filled");
                    } else {
                        style = attr.getStringValue();
                        sg.setAttribute(STYLE_ATTR, style == null || style.length() == 0 ? "filled" : style + ",filled");
                    }
                }
            }
        }
    }

    public double preparePatchWork(int mode)
    {

        double total;

        total = prepPatchWork(PATCH_ATTR, mode);

        if (mode == 0) {
            combPatchWork();
            if (this.elPatches != null) {
                Arrays.sort(this.elPatches, 0, this.elPatches.length, this);
            }
        }

        return (total);
    }

    Element[] getPatches()
    {
        return (this.elPatches);
    }

    private void combPatchWork()
    {
        Subgraph sg;
        Element[] patches;
        Element[] elpat;
        Element[] sgpat;
        Element[] tmparr;

        patches = this.elPatches;

        sgpat = this.sgPatches; // snapshot

        if (sgpat != null && sgpat.length > 0) {
            for (Element element : sgpat) {
                sg = (Subgraph) element;
                sg.combPatchWork();
                elpat = sg.getPatches();
                if (elpat != null && elpat.length > 0) {
                    if (patches == null || patches.length == 0) {
                        patches = elpat;
                    } else {
                        tmparr = new Element[patches.length + elpat.length];
                        System.arraycopy(patches, 0, tmparr, 0, patches.length);
                        System.arraycopy(elpat, 0, tmparr, patches.length, elpat.length);
                        patches = tmparr;
                    }
                }
            }
        }

        this.sgPatches = null;
        this.elPatches = patches;
    }

    private double prepPatchWork(String attrname, int mode)
    {

        double total;
        Object obj;
        int m;
        int n;
        Element[] tmparr;

        total = 0;

        this.sgPatches = null;

        if (this.graphdict != null && !this.graphdict.isEmpty()) {
            if (attrname != null) {
                this.sgPatches = new Element[this.graphdict.size()];
            }
            n = 0;
            for (Subgraph sg : this.graphdict.values()) {
                total += sg.prepPatchWork(attrname, mode);
                if (attrname != null) {
                    this.sgPatches[n++] = sg;
                }
            }
        }

        this.elPatches = null;

        if (attrname != null && this.nodedict != null && !this.nodedict.isEmpty()) {
            m = 0;
            n = 0;
            if (mode <= 0) {
                this.elPatches = new Element[this.nodedict.size()];
            } else if (this.sgPatches == null) {
                this.elPatches = new Element[this.nodedict.size()];
            } else {
                n = this.sgPatches.length;
                this.elPatches = new Element[n + this.nodedict.size()];
                System.arraycopy(this.sgPatches, 0, this.elPatches, 0, n);
                this.sgPatches = null;
            }
            for (Node el : this.nodedict.values()) {
                if ((obj = el.getAttributeValue(attrname)) != null) {
                    if (obj instanceof Number) {
                        el.setPatchSize(((Number) obj).doubleValue());
                        total += el.getPatchSize();
                        this.elPatches[n++] = el;
                    } else {
                        m++;
                    }
                } else {
                    m++;
                }
            }
            if (m > 0) {
                if (n == m) {
                    this.elPatches = null;
                } else {
                    tmparr = new Element[n - m];
                    System.arraycopy(this.elPatches, 0, tmparr, 0, tmparr.length);
                    this.elPatches = tmparr;
                }
            }
        }

        if (mode != 0) {
            if (this.sgPatches != null) {
                Arrays.sort(this.sgPatches, 0, this.sgPatches.length, this);
            }
            if (this.elPatches != null) {
                Arrays.sort(this.elPatches, 0, this.elPatches.length, this);
            }
        }

        setPatchSize(total);

        return total;
    }

    // squarified layout
    double aspect(java.awt.geom.Rectangle2D.Double r)
    {
        return (r.getWidth() == 0 ? 1 : r.getHeight() / r.getWidth());
    }

    double score(double wd, double ht)
    {
        return ((ht <= this.PATCHEDGE2 || wd <= this.PATCHEDGE2) ? Double.MAX_VALUE : (ht > wd ? (wd == 0 ? (ht == 0
            ? 1
                : Double.MAX_VALUE) : ht / wd) : (ht == 0 ? (wd == 0 ? 1 : Double.MAX_VALUE) : wd / ht)));
    }

    public void computePatchWork(java.awt.geom.Rectangle2D.Double r, boolean square)
    {
        if (square) {
            compSqPatchWork(r, true);
        } else {
            compStdPatchWork(r, true);
        }
    }

    private void compSqPatchWork(java.awt.geom.Rectangle2D.Double r, boolean top)
    {
        double frac;
        double total;
        double previous;
        double next;
        double tot;
        double prv;
        double nxt;
        double dir;
        java.awt.geom.Rectangle2D.Double box;
        java.awt.geom.Rectangle2D.Double p;
        Element el;
        Attribute attr;
        String style;
        int i;
        int j;
        double pscore;
        double nscore;
        double sz;
        double psz;
        double tsz;
        double tfrac;

        setPatch(r);
        setAttribute(MINSIZE_ATTR, new GrappaSize(r.getWidth(), r.getHeight() + (top ? 0 : 1)));
        dir = aspect(r);

        total = getPatchSize();
        if (top) {
            box = new GrappaBox(r);
        } else {
            box =
                new GrappaBox(r.getX() + this.PATCHEDGE, r.getY() + this.PATCHEDGE, r.getWidth() - this.PATCHEDGE2,
                    r.getHeight()
                    - this.PATCHEDGE2);
        }

        if (dir > 1) {
            previous = box.getY();
        } else {
            previous = box.getX();
        }

        if (this.sgPatches != null) {
            i = 0;
            while (i < this.sgPatches.length) {
                el = this.sgPatches[i];
                sz = el.getPatchSize();
                if ((i + 1) < this.sgPatches.length) {
                    psz = 0;
                    frac = sz / total;
                    if (dir > 1) {
                        pscore = score(box.getWidth(), frac * box.getHeight());
                    } else {
                        pscore = score(frac * box.getWidth(), box.getHeight());
                    }
                    j = i + 1;

                    for (;;) {
                        tsz = this.sgPatches[j].getPatchSize();
                        tot = psz + sz + tsz;
                        tfrac = tot / total;
                        if (dir > 1) {
                            nscore = score(box.getWidth() * sz / tot, tfrac * box.getHeight());
                        } else {
                            nscore = score(tfrac * box.getWidth(), box.getHeight() * sz / tot);
                        }
                        if (nscore <= pscore) {
                            if (dir > 1) {
                                pscore = score(box.getWidth() * tsz / tot, tfrac * box.getHeight());
                            } else {
                                pscore = score(tfrac * box.getWidth(), box.getHeight() * tsz / tot);
                            }
                            psz += sz;
                            sz = tsz;
                            tsz = 0;
                            j++;
                            if (j < this.sgPatches.length) {
                                continue;
                            }
                        } else {
                            tsz = 0;
                        }
                        tot = psz + sz + tsz;
                        frac = tot / total;
                        if (dir > 1) {
                            prv = box.getX();
                            next = frac * box.getHeight();
                        } else {
                            prv = box.getY();
                            next = frac * box.getWidth();
                        }
                        for (; i < j; i++) {
                            el = this.sgPatches[i];
                            if (dir > 1) {
                                p = new GrappaBox(prv, previous, nxt = box.getWidth() * el.getPatchSize() / tot, next);
                            } else {
                                p = new GrappaBox(previous, prv, next, nxt = box.getHeight() * el.getPatchSize() / tot);
                            }
                            ((Subgraph) el).compSqPatchWork(p, false);
                            prv += nxt;
                        }
                        break;
                    }
                } else {
                    frac = sz / total;
                    if (dir > 1) {
                        p = new GrappaBox(box.getX(), previous, box.getWidth(), (next = frac * box.getHeight()));
                    } else {
                        p = new GrappaBox(previous, box.getY(), (next = frac * box.getWidth()), box.getHeight());
                    }
                    ((Subgraph) el).compSqPatchWork(p, false);
                    i++;
                }
                previous += next;
            }
        }
        if (this.elPatches != null) {
            i = 0;
            while (i < this.elPatches.length) {
                el = this.elPatches[i];
                sz = el.getPatchSize();
                if ((i + 1) < this.elPatches.length) {
                    psz = 0;
                    frac = sz / total;
                    if (dir > 1) {
                        pscore = score(box.getWidth(), frac * box.getHeight());
                    } else {
                        pscore = score(frac * box.getWidth(), box.getHeight());
                    }
                    j = i + 1;
                    for (;;) {
                        tsz = this.elPatches[j].getPatchSize();
                        tot = psz + sz + tsz;
                        tfrac = tot / total;
                        if (dir > 1) {
                            nscore = score(box.getWidth() * sz / tot, tfrac * box.getHeight());
                        } else {
                            nscore = score(tfrac * box.getWidth(), box.getHeight() * sz / tot);
                        }
                        if (nscore <= pscore) {
                            if (dir > 1) {
                                pscore = score(box.getWidth() * tsz / tot, tfrac * box.getHeight());
                            } else {
                                pscore = score(tfrac * box.getWidth(), box.getHeight() * tsz / tot);
                            }
                            psz += sz;
                            sz = tsz;
                            tsz = 0;
                            j++;
                            if (j < this.elPatches.length) {
                                continue;
                            }
                        } else {
                            tsz = 0;
                        }
                        tot = psz + sz + tsz;
                        frac = tot / total;
                        if (dir > 1) {
                            prv = box.getX();
                            next = frac * box.getHeight();
                        } else {
                            prv = box.getY();
                            next = frac * box.getWidth();
                        }
                        for (; i < j; i++) {
                            el = this.elPatches[i];
                            if (el instanceof Node) {
                                if (dir > 1) {
                                    el.setPatch(prv, previous, nxt = box.getWidth() * el.getPatchSize() / tot, next);
                                } else {
                                    el.setPatch(previous, prv, next, nxt = box.getHeight() * el.getPatchSize() / tot);
                                }
                                p = el.getPatch();
                                el.setAttribute(POS_ATTR, new GrappaPoint(p.getCenterX(), -p.getCenterY()));
                                el.setAttribute(WIDTH_ATTR, new Double(p.getWidth() / 72.0));
                                el.setAttribute(HEIGHT_ATTR, new Double(p.getHeight() / 72.0));
                                if (el.getLocalAttribute(COLOR_ATTR) == null) {
                                    el.setAttribute(COLOR_ATTR, "white");
                                }
                                attr = el.getAttribute(STYLE_ATTR);
                                if (attr == null) {
                                    el.setAttribute(STYLE_ATTR, "filled,lineColor(black)");
                                } else {
                                    style = attr.getStringValue();
                                    el.setAttribute(STYLE_ATTR, style == null || style.length() == 0
                                        ? "filled,lineColor(black)" : style + ",filled,lineColor(black)");
                                }
                            } else {
                                if (dir > 1) {
                                    p =
                                        new GrappaBox(prv, previous, nxt = box.getWidth() * el.getPatchSize() / tot,
                                            next);
                                } else {
                                    p =
                                        new GrappaBox(previous, prv, next, nxt =
                                            box.getHeight() * el.getPatchSize() / tot);
                                }
                                ((Subgraph) el).compSqPatchWork(p, false);
                            }
                            prv += nxt;
                        }
                        break;
                    }
                } else {
                    frac = sz / total;
                    if (el instanceof Node) {
                        if (dir > 1) {
                            el.setPatch(box.getX(), previous, box.getWidth(), (next = frac * box.getHeight()));
                        } else {
                            el.setPatch(previous, box.getY(), (next = frac * box.getWidth()), box.getHeight());
                        }
                        p = el.getPatch();
                        el.setAttribute(POS_ATTR, new GrappaPoint(p.getCenterX(), -p.getCenterY()));
                        el.setAttribute(WIDTH_ATTR, new Double(p.getWidth() / 72.0));
                        el.setAttribute(HEIGHT_ATTR, new Double(p.getHeight() / 72.0));
                        if (el.getLocalAttribute(COLOR_ATTR) == null) {
                            el.setAttribute(COLOR_ATTR, "white");
                        }
                        attr = el.getAttribute(STYLE_ATTR);
                        if (attr == null) {
                            el.setAttribute(STYLE_ATTR, "filled,lineColor(black)");
                        } else {
                            style = attr.getStringValue();
                            el.setAttribute(STYLE_ATTR, style == null || style.length() == 0
                                ? "filled,lineColor(black)" : style + ",filled,lineColor(black)");
                        }
                    } else {
                        if (dir > 1) {
                            p = new GrappaBox(box.getX(), previous, box.getWidth(), (next = frac * box.getHeight()));
                        } else {
                            p = new GrappaBox(previous, box.getY(), (next = frac * box.getWidth()), box.getHeight());
                        }
                        ((Subgraph) el).compSqPatchWork(p, false);
                    }
                    i++;
                }
                previous += next;
            }
        }
    }

    private void compStdPatchWork(java.awt.geom.Rectangle2D.Double r, boolean top)
    {
        double sz;
        double frac;
        double total;
        double previous;
        double next;
        double dir;
        java.awt.geom.Rectangle2D.Double box;
        java.awt.geom.Rectangle2D.Double p;
        Element el;
        Attribute attr;
        String style;

        setPatch(r);
        setAttribute(MINSIZE_ATTR, new GrappaSize(r.getWidth(), r.getHeight() + (top ? 0 : 1)));
        dir = aspect(r);

        total = getPatchSize();
        if (top) {
            box = new GrappaBox(r);
        } else {
            box =
                new GrappaBox(r.getX() + this.PATCHEDGE, r.getY() + this.PATCHEDGE, r.getWidth() - this.PATCHEDGE2,
                    r.getHeight()
                    - this.PATCHEDGE2);
        }

        if (dir > 1) {
            previous = box.getY();
        } else {
            previous = box.getX();
        }

        if (this.sgPatches != null) {
            for (Element sgPatche : this.sgPatches) {
                el = sgPatche;
                sz = el.getPatchSize();
                frac = sz / total;
                if (dir > 1) {
                    ((Subgraph) el).compStdPatchWork(new GrappaBox(box.getX(), previous, box.getWidth(), (next =
                        frac * box.getHeight())), false);
                } else {
                    ((Subgraph) el).compStdPatchWork(new GrappaBox(previous, box.getY(),
                        (next = frac * box.getWidth()), box.getHeight()), false);
                }
                previous += next;
            }
        }
        if (this.elPatches != null) {
            for (Element elPatche : this.elPatches) {
                el = elPatche;
                sz = el.getPatchSize();
                frac = sz / total;
                if (el instanceof Node) {
                    if (dir > 1) {
                        el.setPatch(box.getX(), previous, box.getWidth(), (next = frac * box.getHeight()));
                    } else {
                        el.setPatch(previous, box.getY(), (next = frac * box.getWidth()), box.getHeight());
                    }
                    p = el.getPatch();
                    el.setAttribute(POS_ATTR, new GrappaPoint(p.getCenterX(), -p.getCenterY()));
                    el.setAttribute(WIDTH_ATTR, new Double(p.getWidth() / 72.0));
                    el.setAttribute(HEIGHT_ATTR, new Double(p.getHeight() / 72.0));
                    if (el.getLocalAttribute(COLOR_ATTR) == null) {
                        el.setAttribute(COLOR_ATTR, "white");
                    }
                    attr = el.getAttribute(STYLE_ATTR);
                    if (attr == null) {
                        el.setAttribute(STYLE_ATTR, "filled,lineColor(black)");
                    } else {
                        style = attr.getStringValue();
                        el.setAttribute(STYLE_ATTR, style == null || style.length() == 0 ? "filled,lineColor(black)"
                            : style + ",filled,lineColor(black)");
                    }
                } else {
                    if (dir > 1) {
                        ((Subgraph) el).compStdPatchWork(new GrappaBox(box.getX(), previous, box.getWidth(), (next =
                            frac * box.getHeight())), false);
                    } else {
                        ((Subgraph) el)
                            .compStdPatchWork(
                                new GrappaBox(previous, box.getY(), (next = frac * box.getWidth()), box.getHeight()),
                                false);
                    }
                }
                previous += next;
            }
        }
    }

    // Comparator for patchArea
    @Override
    public int compare(Element o1, Element o2)
    {
        // biggest to smallest
        double diff = o2.getPatchSize() - o1.getPatchSize();
        return (diff < 0 ? -1 : diff > 0 ? 1 : 0);
    }

    @Override
    public boolean equals(Object obj)
    {

        // do not need
        return (false);
    }

    //
    // End PatchWork stuff
    //

    /**
     * Get an enumeration of the node elements in this subgraph.
     *
     * @return an Enumeration of Node objects
     */
    public Enumeration<Node> nodeElements()
    {
        if (this.nodedict == null) {
            return Collections.emptyEnumeration();
        }
        return new IteratorEnumeration<>(this.nodedict.values().iterator());
    }

    /**
     * Get an enumeration of the edge elements in this subgraph.
     *
     * @return an Enumeration of Edge objects
     */
    public Enumeration<Edge> edgeElements()
    {
        if (this.edgedict == null) {
            return Collections.emptyEnumeration();
        }
        return new IteratorEnumeration<>(this.edgedict.values().iterator());
    }

    /**
     * Get an enumeration of the subgraph elements in this subgraph.
     *
     * @return an Enumeration of Subgraph objects
     */
    public Enumeration<Subgraph> subgraphElements()
    {
        if (this.graphdict == null) {
            return Collections.emptyEnumeration();
        }
        return new IteratorEnumeration<>(this.graphdict.values().iterator());
    }

    /**
     * Get an enumeration of elements in this subgraph and any subgraphs under this one.
     *
     * @param types a bitwise-oring of NODE, EDGE, SUBGRAPH to determine which element types should be in the
     *            enumeration
     * @return a GraphEnumeration containing Element objects.
     * @see GrappaConstants#NODE
     * @see GrappaConstants#EDGE
     * @see GrappaConstants#SUBGRAPH
     */
    public GraphEnumeration elements(int types)
    {
        return new Enumerator(types);
    }

    /**
     * Get an enumeration of all elements in this subgraph and any subgraphs under this one. A convenience method
     * equivalent to: <code>
     * elements(NODE|EDGE|SUBGRAPH)
     * </code>
     *
     * @return a GraphEnumeration containing Element objects.
     * @see Subgraph#elements(int)
     */
    public GraphEnumeration elements()
    {
        return new Enumerator(NODE | EDGE | SUBGRAPH);
    }

    class Enumerator implements GraphEnumeration
    {
        private Subgraph root = null;

        private int types = 0;

        private Enumeration<? extends Element> enm = null;

        private GraphEnumeration subEnum = null;

        private Element elem = null;

        private int dictType = 0;

        Enumerator(int t)
        {
            this.root = Subgraph.this;
            this.types = t;

            if ((this.types & SUBGRAPH) != 0) {
                this.elem = (this.root);
            } else {
                this.elem = null;
            }
            this.enm = subgraphElements();
            if (this.enm.hasMoreElements()) {
                this.dictType = SUBGRAPH;
                while (this.enm.hasMoreElements()) {
                    this.subEnum = ((Subgraph) (this.enm.nextElement())).new Enumerator(this.types);
                    if (this.subEnum.hasMoreElements()) {
                        if (this.elem == null) {
                            this.elem = this.subEnum.nextElement();
                        }
                        break;
                    }
                }
            } else {
                this.dictType = 0;
                this.enm = null;
                this.subEnum = null;
            }
            if (this.enm == null) {
                if ((this.types & NODE) != 0 && (this.enm = nodeElements()).hasMoreElements()) {
                    this.dictType = NODE;
                    if (this.elem == null) {
                        this.elem = this.enm.nextElement();
                    }
                } else if ((this.types & EDGE) != 0 && (this.enm = edgeElements()).hasMoreElements()) {
                    this.dictType = EDGE;
                    if (this.elem == null) {
                        this.elem = this.enm.nextElement();
                    }
                } else {
                    this.enm = null;
                }
            }
        }

        @Override
        public boolean hasMoreElements()
        {
            return this.elem != null;
        }

        @Override
        public Element nextElement()
        {
            if (this.elem == null) {
                throw new NoSuchElementException("Subgraph$Enumerator");
            }
            Element el = this.elem;
            if (this.subEnum != null && this.subEnum.hasMoreElements()) {
                this.elem = this.subEnum.nextElement();
            } else if (this.enm != null && this.enm.hasMoreElements()) {
                do {
                    this.elem = this.enm.nextElement();
                    if (this.elem.isSubgraph()) {
                        this.subEnum = ((Subgraph) this.elem).new Enumerator(getEnumerationTypes());
                        if (this.subEnum.hasMoreElements()) {
                            this.elem = this.subEnum.nextElement();
                            break;
                        } else {
                            this.elem = null;
                        }
                    } else {
                        break;
                    }
                } while (this.enm.hasMoreElements());
            } else {
                this.elem = null;
            }
            if (this.elem == null) {
                if (this.dictType != 0) {
                    if (this.dictType == SUBGRAPH) {
                        if ((getEnumerationTypes() & NODE) != 0 && (this.enm = nodeElements()).hasMoreElements()) {
                            this.dictType = NODE;
                            this.elem = this.enm.nextElement();
                        } else if ((getEnumerationTypes() & EDGE) != 0 && (this.enm = edgeElements()).hasMoreElements()) {
                            this.dictType = EDGE;
                            this.elem = this.enm.nextElement();
                        } else {
                            this.dictType = 0;
                            this.enm = null;
                        }
                    } else if (this.dictType == NODE) {
                        if ((getEnumerationTypes() & EDGE) != 0 && (this.enm = edgeElements()).hasMoreElements()) {
                            this.dictType = EDGE;
                            this.elem = this.enm.nextElement();
                        } else {
                            this.dictType = 0;
                            this.enm = null;
                        }
                    } else {
                        this.dictType = 0;
                        this.enm = null;
                    }
                }
            }
            return el;
        }

        @Override
        public Element nextGraphElement()
        {
            return nextElement();
        }

        @Override
        public Subgraph getSubgraphRoot()
        {
            return this.root;
        }

        @Override
        public int getEnumerationTypes()
        {
            return this.types;
        }
    }

    /**
     * Get a vector of elements in this subgraph and, by recursion, descendant subgraphs.
     *
     * @param types a bitwise-oring of NODE, EDGE, SUBGRAPH to determine which element types should be in the count
     * @return a vector of the specified elements in this subgraph and its descendants (excluding the current subgraph
     *         itself).
     * @see GrappaConstants#NODE
     * @see GrappaConstants#EDGE
     * @see GrappaConstants#SUBGRAPH
     */
    public Vector<Element> vectorOfElements(int types)
    {
        Vector<Element> retVec = new Vector<>();
        int count = 0;
        if ((types & NODE) != 0 && this.nodedict != null) {
            count += this.nodedict.size();
            retVec.ensureCapacity(count);
            for (Node n : this.nodedict.values()) {
                retVec.addElement(n);
            }
        }
        if ((types & EDGE) != 0 && this.edgedict != null) {
            count += this.edgedict.size();
            retVec.ensureCapacity(count);
            for (Edge e : this.edgedict.values()) {
                retVec.addElement(e);
            }
        }
        if (this.graphdict != null) {
            if ((types & SUBGRAPH) != 0) {
                count += this.graphdict.size();
                retVec.ensureCapacity(count);
            }
            for (Subgraph sg : this.graphdict.values()) {
                sg.recurseVectorOfElements(types, retVec, count);
            }
        }
        return (retVec);
    }

    // used above
    private void recurseVectorOfElements(int types, Vector<Element> retVec, int count)
    {
        if ((types & SUBGRAPH) != 0) {
            retVec.addElement(this);
        }
        if ((types & NODE) != 0 && this.nodedict != null) {
            count += this.nodedict.size();
            retVec.ensureCapacity(count);
            for (Node n : this.nodedict.values()) {
                retVec.addElement(n);
            }
        }
        if ((types & EDGE) != 0 && this.edgedict != null) {
            count += this.edgedict.size();
            retVec.ensureCapacity(count);
            for (Edge e : this.edgedict.values()) {
                retVec.addElement(e);
            }
        }
        if (this.graphdict != null) {
            if ((types & SUBGRAPH) != 0) {
                count += this.graphdict.size();
                retVec.ensureCapacity(count);
            }
            for (Subgraph sg : this.graphdict.values()) {
                sg.recurseVectorOfElements(types, retVec, count);
            }
        }
    }
}
