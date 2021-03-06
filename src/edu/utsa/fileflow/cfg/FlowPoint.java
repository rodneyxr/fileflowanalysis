package edu.utsa.fileflow.cfg;

import edu.utsa.fileflow.analysis.AnalysisDomain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A FlowPoint represents a node in the Control Flow Graph. It can have
 * any amount of entry points and should have zero, one or two exit points since
 * there is no switch statements in the FileFlow grammar.
 *
 * @author Rodney Rodriguez
 */
public class FlowPoint {

	/* Unique ID for this FlowPoint instance */
	public final int id;

	/* Used to generate a unique id for each FlowPoint instance */
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

	/* Used for graph traversal */
	private boolean visited = false;

	/* Holds information from the parser */
	private FlowPointContext fpctx;

	/* Lists of incoming and outgoing edges */
	private FlowPointEdgeList incoming;
	private FlowPointEdgeList outgoing;

	/* Analysis variables to be used by the analysis framework */
	private HashMap<String, AnalysisDomain<?>> domains = new HashMap<>();
	private boolean analyzed = false;

	public FlowPoint(String text) {
		this(new FlowPointContext(text));
	}

	public FlowPoint(FlowPointContext fpctx) {
		if (fpctx == null) {
			this.fpctx = new FlowPointContext("Flow Point");
		} else {
			this.fpctx = fpctx;
		}
		incoming = new FlowPointEdgeList();
		outgoing = new FlowPointEdgeList();
		id = ID_GENERATOR.getAndIncrement();
		this.fpctx.setFlowPoint(this);
	}

	/**
	 * Retrieves a stored domain from this FlowPoint.
	 * Each domain is stored by their classname and are independent
	 * of each other. If the analysis is not ran before calling this method,
	 * null may be returned.
	 *
	 * @param klass The extended {@link AnalysisDomain} class.
	 * @return the domain stored by the framework.
	 */
	public AnalysisDomain<?> getDomain(Class klass) {
		return domains.get(klass.getCanonicalName());
	}

	public void setDomain(AnalysisDomain<?> domain) {
		domains.put(domain.getClass().getCanonicalName(), domain);
	}

	/**
	 * Retrieves the original domain set by the framework at this FlowPoint.
	 * The original domain is the input domain of the FlowPoint. In other words,
	 * it is the domain before it was updated by the analysis. Calling this before
	 * running the analysis will return null.
	 *
	 * @param klass The extended {@link AnalysisDomain} class of the domain you want to retrieve.
	 * @return the original domain stored by the framework.
	 */
	public AnalysisDomain<?> getOriginalDomain(Class klass) {
		return domains.get(klass.getCanonicalName() + ".original");
	}

	public void setOriginalDomain(AnalysisDomain<?> domain) {
		domains.put(domain.getClass().getCanonicalName() + ".original", domain);
	}

	/**
	 * @return True if this flow point has been visited and analyzed.
	 */
	public boolean getAnalyzed() {
		return analyzed;
	}

	/**
	 * Marks the flow point as visited to be used by the fixed point algorithm
	 * during analysis.
	 *
	 * @param analyzed True if this flow point was analyzed.
	 */
	public void setAnalyzed(boolean analyzed) {
		this.analyzed = analyzed;
	}

	/*
	 * Resets this and all flow points under this flow point.
	 */
	public void reset() {
		resetAnalyzed();
		resetVisited();
	}

	/**
	 * Gets the context of this flow point.
	 *
	 * @return The context of this flow point.
	 */
	public FlowPointContext getContext() {
		return fpctx;
	}

	/**
	 * Gets the list of incoming edges.
	 *
	 * @return The list of this node's incoming edges.
	 */
	public FlowPointEdgeList getIncomingEdgeList() {
		return incoming;
	}

	/**
	 * Gets the list of outgoing edges.
	 *
	 * @return The list of this node's outgoing edges.
	 */
	public FlowPointEdgeList getOutgoingEdgeList() {
		return outgoing;
	}

	public FlowPoint addFlowPoint(FlowPoint flowpoint) {
		FlowPointEdge edge = new FlowPointEdge(this, flowpoint);
		outgoing.add(edge);
		flowpoint.incoming.add(edge);
		return flowpoint;
	}

	/**
	 * Get all flow points under this flow point. The list returned will also
	 * include itself.
	 *
	 * @return A list of all flow points.
	 */
	public ArrayList<FlowPoint> getAllFlowPoints() {
		ArrayList<FlowPoint> children = new ArrayList<FlowPoint>();
		getAllFlowPointsImpl(children);
		resetVisited();
		return children;
	}

	/**
	 * Implementation of getAllFlowPoints(). This private method is used to hide
	 * the parameter that is used by the recursion.
	 *
	 * @param children The list that holds all flow points.
	 * @return The list that holds all flow points.
	 */
	private ArrayList<FlowPoint> getAllFlowPointsImpl(ArrayList<FlowPoint> children) {
		if (visited)
			return children;

		children.add(this);
		visited = true;
		for (FlowPointEdge edge : getOutgoingEdgeList()) {
			edge.getTarget().getAllFlowPointsImpl(children);
		}

		return children;
	}

	/**
	 * Prints a text representation of this flow point and its children
	 * recursively.
	 */
	public void print() {
		printImpl();
		resetVisited();
	}

	/*
	 * Implementation of print().
	 */
	private void printImpl() {
		if (visited)
			return;

		// print parent followed by all its children flow points
		System.out.printf("%s => { ", this);
		for (FlowPointEdge edge : getOutgoingEdgeList()) {
			System.out.printf("%s, ", edge.getTarget());
		}
		System.out.println("}");
		visited = true;

		// recursive print for children
		for (FlowPointEdge edge : getOutgoingEdgeList()) {
			edge.getTarget().printImpl();
		}
	}

	/*
	 * Sets the boolean visited to false for all flow points under this instance
	 * of flow point.
	 */
	private void resetVisited() {
		visited = false;
		for (FlowPointEdge edge : getOutgoingEdgeList()) {
			FlowPoint target = edge.getTarget();
			if (target.visited)
				target.resetVisited();
		}
	}

	/*
	 * Sets the boolean analyzed to false for all flow points under this instance
	 * of flow point.
	 */
	private void resetAnalyzed() {
		analyzed = false;
		for (FlowPointEdge edge : getOutgoingEdgeList()) {
			FlowPoint target = edge.getTarget();
			if (target.analyzed)
				target.resetAnalyzed();
		}
	}

	@Override
	public String toString() {
		return fpctx.getText();
	}

}