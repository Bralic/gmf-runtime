/******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/
package org.eclipse.gmf.runtime.draw2d.ui.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Ray;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.geometry.Transform;

import org.eclipse.gmf.runtime.common.core.util.Trace;
import org.eclipse.gmf.runtime.draw2d.ui.internal.Draw2dDebugOptions;
import org.eclipse.gmf.runtime.draw2d.ui.internal.Draw2dPlugin;


/**
 * A set of utility methods around manipulating PointList objects
 * 
 * @author sshaw
 */
public class PointListUtilities {

	/**
	 * Removes redudant points from the point list.
	 * 
	 * @param points the <code>PointList</code> which will be modified to remove redudant points
	 * @param straightLineTolerance the tolerance value within which indicates the line is straight
	 * @return <code>boolean</code> <code>true</code> if segments were modified, <code>false</code> otherwise.
	 */
	static boolean flattenSegments(PointList points, int straightLineTolerance) {
		boolean changed = false;
		
		for (int i = 0; i < points.size() - 1; i++) {
			Point ptStart = points.getPoint(i);
	
			Point ptTerm = null;
			if (i + 1 < points.size())
				ptTerm = points.getPoint(i + 1);
	
			Point ptNext = null;
			if (i + 2 < points.size())
				ptNext = points.getPoint(i + 2);
	
			// We conservatively avoid changing the endpoint of the line, since it may then
			// fail to attach to the terminal view.  This implies ptNext must be non-NIL.
			if (points.size() <= 2 || ptTerm == null || ptNext == null)
				return changed;
	
			if (sameOrientation(ptStart, ptTerm, ptNext, straightLineTolerance)) {
				removePoint(points, i + 1);
				
				Ray seg = new Ray(ptStart, ptTerm);
				if (seg.y < straightLineTolerance) {
					points.setPoint(new Point(ptNext.x, ptStart.y), i + 1);
				} else if (seg.x < straightLineTolerance){
					points.setPoint(new Point(ptStart.x, ptNext.y), i + 1);
				}
				
				changed = true;
			}
		}
		
		return changed;
	}
	
	static private Point removePoint(PointList points, int index) {
		Point removedPt = points.getPoint(index);
		for (int i=index; i<points.size()-1; i++) {
			points.setPoint(points.getPoint(i+1), i);
		}
		points.setSize(points.size() - 1);
		return removedPt;
	}
	
	/**
	 * Normalizes the line segments in the polyline.  Checks for lines that are with-in a threshold for length
	 * and removes them.  Additionally, it will remove points that that adjacent segments that are both
	 * horizontal, or both vertical.
	 * 
	 * @param points <code>PointList</code> to be normalized
	 * @return <code>boolean</code> <code>true</code> if segments were changed, <code>false</code> otherwise
	 */
	static public boolean normalizeSegments(PointList points) {
		return normalizeSegments(points, 0);
	}
	
	/**
	 * Normalizes the line segments in the polyline.  Checks for lines that are with-in a threshold for length
	 * and removes them.  Additionally, it will remove points that that adjacent segments that are both
	 * horizontal, or both vertical.  Will utilize a tolerance value to determine if segments needs to flattened
	 * or not.
	 * 
	 * @param points <code>PointList</code> to normalize
	 * @param straightLineTolerance the tolerance value within which indicates the line is straight in
	 * relative coordinates.
	 * @return <code>boolean</code> <code>true</code> if segments were changed, <code>false</code> otherwise
	 */
	static public boolean normalizeSegments(PointList points, int straightLineTolerance) {
		
		boolean hasChanged = false;

		// first pass to remove points with-in the length tolerance
		for (int i = 1; i < points.size() - 1; i++) {
			Ray nextVector =
				new Ray(points.getPoint(i), points.getPoint(i - 1));
			int nextLength = (int) nextVector.length();

			if (nextLength <= straightLineTolerance) {
				points.removePoint(i);
				hasChanged = true;
			}
		}
		
		// second pass to flatten segments that are parallel to each other with-in a tolerance value
		hasChanged |= flattenSegments(points, straightLineTolerance);
		
		// recursively normalize the points if something has changed.
		if (hasChanged)
			normalizeSegments(points, straightLineTolerance);
		
		return hasChanged;
	}

	/**
	 * Method getPointsSupremum.
	 * Get points representing the extrema for this poly line.
	 * 
	 * @param points PointList to calculate the highest point from.
	 * @return Point value of the highest point in the bounding box of the
	 *      polyline.
	 */
	static public Point getPointsSupremum(PointList points) {
		Point theSupremum = points.getFirstPoint();
		for (int i = 1; i < points.size(); i++) {
			Point other = points.getPoint(i);
			theSupremum =
				new Point(
					Math.max(theSupremum.x, other.x),
					Math.max(theSupremum.y, other.y));
		}

		return theSupremum;
	}

	/**
	 * Method getPointsSupremum.
	 * Get points representing the minimum for this poly line.
	 * 
	 * @param points PointList to calculate the minimum point from.
	 * @return Point value of the lowest point in the bounding box of the
	 *      polyline.
	 */
	static public Point getPointsInfimum(PointList points) {
		Point theInfimum = points.getFirstPoint();
		for (int i = 1; i < points.size(); i++) {
			Point other = points.getPoint(i);
			theInfimum =
				new Point(
					Math.min(theInfimum.x, other.x),
					Math.min(theInfimum.y, other.y));
		}

		return theInfimum;
	}

	/**
	 * createPointsFromRect
	 * 
	 * @param rBox Rectangle to base the PointList from
	 * @return PointList that is equivalent to the Rectangle
	 */
	static public PointList createPointsFromRect(Rectangle rBox) {
		PointList points = new PointList(5);
		Point pt = new Point(rBox.getLeft().x, rBox.getTop().y);
		points.addPoint(pt);
		pt = new Point(rBox.getRight().x, rBox.getTop().y);
		points.addPoint(pt);
		pt = new Point(rBox.getRight().x, rBox.getBottom().y);
		points.addPoint(pt);
		pt = new Point(rBox.getLeft().x, rBox.getBottom().y);
		points.addPoint(pt);
		pt = new Point(rBox.getLeft().x, rBox.getTop().y);
		points.addPoint(pt);

		return points;
	}
	
	/**
	 * reAdjustBoxSize
	 * private utility method to shrink / expand the box segment size to accommodate the given point
	 * on the edge.  When completed the containedPoint will be on the border of the boxSegs list.
	 * Assumptions: 
	 * 1. boxSegs is a list of LineSegs that represent a rectangle.
	 * 2. boxSegs is ordered clockwise around the rectangle.
	 * 
	 * @param boxSegs List of LineSegs to shrink / expand
	 * @param containedPoint Point that is typically contained inside the Rectangle the
	 * the boxSegs LineSeg list represents.
	 */
	static private void reAdjustBoxSize(List boxSegs, Point containedPoint) {
		assert boxSegs.size() == 4;

		LineSeg seg =
			getNearestSegment(
				boxSegs,
				containedPoint.x,
				containedPoint.y);
		LineSeg newSeg = seg.getParallelLineSegThroughPoint(containedPoint);

		ListIterator li = boxSegs.listIterator();
		LineSeg prev = null;
		LineSeg next = null;
		LineSeg current = null;
		if (li.hasNext())
			current = (LineSeg) li.next();

		while (current != null) {
			if (li.hasNext())
				next = (LineSeg) li.next();
			else
				next = null;

			if (current.equals(seg)) {
				if (prev != null) {
					prev.setTerminus(newSeg.getOrigin());
				}

				current.setOrigin(newSeg.getOrigin());
				current.setTerminus(newSeg.getTerminus());

				if (next != null) {
					next.setOrigin(newSeg.getTerminus());
				}
			}
			current = next;
		}
	}
	
	/**
	 * Method routeAroundRect.
	 * Route this polyline around a rectangle that is presumably intersecting with 
	 * it.
	 * 
	 * @param points PointList that will be modified to route around the given rectangle
	 * @param rBox the <code>Rectangle</code> around which the routing will occur.
	 * @param nSmoothFactor the <code>int</code> smooth factor to route the line with 
	 *          0 - None, 15 - some, 30 - lots
	 * @param bIncludeIntersectionPoints the <code>boolean</code> determining whether to include the
	 * points that intersect with the rectangle in the newly routed polyline.
	 * @param nBuffer the <code>int</code> buffer around the rectangle.
	 * @return <code>PointList</code> that is the newly routed version of <code>points</code>
	 * of <code>null</code> if operation was not successful or if the calculation is not possible.
	 */
	static public PointList routeAroundRect(
		PointList points,
		final Rectangle rBox,
		int nSmoothFactor,
		final boolean bIncludeIntersectionPoints,
		final int nBuffer) {
		Point infimumPoint = getPointsInfimum(points);
		Point supremumPoint = getPointsSupremum(points);

		Ray diameter = new Ray(infimumPoint, supremumPoint);
		Rectangle rPoly =
			new Rectangle(
				infimumPoint.x,
				infimumPoint.y,
				diameter.x,
				diameter.y);

		// increase connect view rect by one in case it's fully horizontal
		// or vertical
		rPoly.expand(1, 1);

		if (rPoly.intersects(rBox)) {
			// construct a polyline out of the box
			PointList rBoxPoly = createPointsFromRect(rBox);

			// check to see if the end points of the list are contained in the box.
			// If so, then we should shrink the box to force an intersection
			Point firstPoint = points.getFirstPoint();
			Point lastPoint = points.getLastPoint();
			boolean bFPContained = rBox.contains(firstPoint);
			boolean bLPContained = rBox.contains(lastPoint);

			if (bFPContained || bLPContained) {
				List boxSegs = getLineSegments(rBoxPoly);

				if (bFPContained) {
					reAdjustBoxSize(boxSegs, firstPoint);
				}
				if (bLPContained) {
					reAdjustBoxSize(boxSegs, lastPoint);
				}

				rBoxPoly.removeAllPoints();
				ListIterator li = boxSegs.listIterator();
				while (li.hasNext()) {
					LineSeg seg = (LineSeg) li.next();

					rBoxPoly.addPoint(seg.getOrigin());
					if (!li.hasNext())
						rBoxPoly.addPoint(seg.getTerminus());
				}
			}

			return routeAroundPoly(
				points,
				rBoxPoly,
				nSmoothFactor,
				true,
				bIncludeIntersectionPoints,
				nBuffer);
		}

		return null;
	}

	/**
	 * Method getLineSegments.
	 * Converts the points of this polyline into a list of <code>LineSeg</code> 
	 *      objects
	 * 
	 * @param points PointList to get LineSeg equivalents of.
	 * @return List of LineSeg objects.
	 */
	public static List getLineSegments(PointList points) {
		if (points.size() <= 1)
			return new ArrayList(0);

		ArrayList lines = new ArrayList(points.size() - 1);

		for (int i = 0; i < points.size() - 1; i++) {
			lines.add(new LineSeg(points.getPoint(i), points.getPoint(i + 1)));
		}

		return lines;
	}

	final static int INTERSECT_TOLERANCE = 1;
	final static int MIN_LINE_LENGTH = 5;
	
	/**
	 * Constant that is the default number of lines that a bezier is approximated by as a polyline
	 * point list.
	 */
	public final static int DEFAULT_BEZIERLINES = 16;
	
	final static int MAX_BEZIERLINES = 32;

	/**
	 * Routes this polyline around another polyline that is presumably intersecting with 
	 * it.
	 * 
	 * @param points the <code>PointList</code> that is to be modified based on
	 * the routing calculations made around the <code>poly</code> parameter.
	 * @param poly the <code>PolylinePointList</code> around which the routing will occur.
	 * @param nSmoothFactor the <code>int</code> smooth factor to route the line with 
	 *          0 - None, 15 - some, 30 - lots
	 * @param bShortestDistance the <code>boolean</code> determining whether to use the
	 * shortest distance possible to achieve this or else minimally modify the existing polyline.
	 * @param bIncludeIntersectionPoints the <code>boolean</code> determining whether to include the
	 * points that intersect with the rectangle in the newly routed polyline.
	 * @param nBuffer the <code>int</code> buffer around the rectangle.
	 * @return <code>PointList</code> that is the newly routed version of <code>points</code>
	 * of <code>null</code> if operation was not successful or if the calculation is not possible.
	 */
	static public PointList routeAroundPoly(
		PointList points,
		final PointList poly,
		int nSmoothFactor,
		boolean bShortestDistance,
		final boolean bIncludeIntersectionPoints,
		final int nBuffer) {

		LineSeg pCurBoxSeg = null;
		Point ptIntersect = new Point();
		PointList rPolyPoints = new PointList(points.size() * 2);

		List rBoxSegments = getLineSegments(poly);

		Point ptIntersect1 = null;
		Point ptPrevIntersect = new Point(-100, -100);
		// place initially off the diagram
		int nPointsSinceFirstIntersection = 0;

		int nCurrentLength1 = 0;
		int nCurrentLength = 0;

		List mySegments = getLineSegments(points);
		ListIterator listIter = mySegments.listIterator();
		while (listIter.hasNext()) {
			LineSeg pSegment = (LineSeg) listIter.next();

			boolean bFoundIntersects = false;

			ListIterator boxIter = rBoxSegments.listIterator();
			while (boxIter.hasNext()) {
				LineSeg pBoxSegment = (LineSeg) boxIter.next();

				// check if this segment intersects with the box - intersect with 0 tolerance
				ptIntersect = pSegment.intersect(pBoxSegment, INTERSECT_TOLERANCE);
				if (ptIntersect != null) {
					// check case where intersect is on a corner - causing intersect
					// to show up in two adjacent line segments
					if (Math.abs(ptPrevIntersect.x - ptIntersect.x)
						> (INTERSECT_TOLERANCE * 2)
						|| Math.abs(ptPrevIntersect.y - ptIntersect.y)
							> (INTERSECT_TOLERANCE * 2)) {
						if (ptIntersect1 != null) {
							Point ptIntersect2 = null;
							LineSeg pCurBoxSeg2;
							PointList newRoutePoints = new PointList();

							int nCurrentLength2 =
								nCurrentLength
									+ (int) (pSegment.distanceAlong(ptIntersect)
										* pSegment.length());

							// which intersection point should be added first?
							if (nCurrentLength1 < nCurrentLength2) {
								ptIntersect2 = new Point(ptIntersect);
								pCurBoxSeg2 = pBoxSegment;
							} else {
								ptIntersect2 = new Point(ptIntersect1);
								pCurBoxSeg2 = pCurBoxSeg;

								ptIntersect1 = new Point(ptIntersect);
								pCurBoxSeg = pBoxSegment;
							}

							getRoutedPoints(
								points,
								newRoutePoints,
								ptIntersect1,
								ptIntersect2,
								rPolyPoints.getLastPoint(),
								pSegment.getTerminus(),
								pCurBoxSeg,
								pCurBoxSeg2,
								poly,
								nSmoothFactor,
								bShortestDistance,
								bIncludeIntersectionPoints,
								nBuffer);

							while (nPointsSinceFirstIntersection > 0) {
								rPolyPoints.removePoint(rPolyPoints.size() - 1);
								nPointsSinceFirstIntersection--;
							}

							for (int i = 0; i < newRoutePoints.size(); i++)
								rPolyPoints.addPoint(
									newRoutePoints.getPoint(i));
							rPolyPoints.addPoint(
								new Point(pSegment.getTerminus()));

							ptIntersect1 = null;
							bFoundIntersects = true;
							break; // go to next segment
						} else {
							ptIntersect1 = new Point(ptIntersect);

							pCurBoxSeg = pBoxSegment;
							rPolyPoints.addPoint(
								new Point(pSegment.getOrigin()));

							nPointsSinceFirstIntersection = 0;
							nCurrentLength1 =
								nCurrentLength
									+ (int) (pSegment.distanceAlong(ptIntersect)
										* pSegment.length());
						}
					}

					ptPrevIntersect = new Point(ptIntersect);
				}
			}

			nCurrentLength += pSegment.length();

			if (!bFoundIntersects) {
				rPolyPoints.addPoint(new Point(pSegment.getOrigin()));

				if (ptIntersect1 != null)
					nPointsSinceFirstIntersection++;

				if (!listIter.hasNext())
					rPolyPoints.addPoint(new Point(pSegment.getTerminus()));
			}
		}

		if (points.size() != rPolyPoints.size()) {
			return rPolyPoints;
		}

		return null;
	}

	/**
	 * copyPoints
	 * This method is necessary because of an apparrent defect in the getCopy()
	 * routine of PointList class.  It is initializing the copy based on size
	 * but then making the copy based on the array allocation which may be different
	 * causing an ArrayIndexOutOfBounds exception.
	 * 
	 * @param pointsFrom
	 * @return PointList
	 */
	static public PointList copyPoints(PointList pointsFrom) {
		PointList points = new PointList(pointsFrom.size());
		copyFrom(points, pointsFrom);
		return points;
	}

	static private void copyFrom(PointList pointsTo, PointList pointsFrom) {
		pointsTo.removeAllPoints();
		for (int i = 0; i < pointsFrom.size(); i++)
			pointsTo.addPoint(pointsFrom.getPoint(i));
	}

	/**
	 * Method addRoutedPoints.
	 * Utility function used by getRoutedPoints to add the new "detour" route to the
	 * existing polyline.
	 * 
	 * @param routePoints
	 * @param pCurBoxSeg1
	 * @param pCurBoxSeg2
	 * @param routePoly
	 * @param bForward
	 * @param nBuffer
	 * @return int
	 */
	static private int addRoutedPoints(
		PointList routePoints,
		LineSeg pCurBoxSeg1,
		LineSeg pCurBoxSeg2,
		final PointList routePoly,
		final boolean bForward,
		final int nBuffer) {
		// figure out which direction to traverse
		Point pNewPoint = null;
		float fOffset = 0;
		int nDistance = 0;

		List rBoxSegments = getLineSegments(routePoly);
		ListIterator boxIter =
			bForward
				? rBoxSegments.listIterator()
				: rBoxSegments.listIterator(rBoxSegments.size());
		while (bForward ? boxIter.hasNext() : boxIter.hasPrevious()) {
			LineSeg seg =
				bForward
					? (LineSeg) boxIter.next()
					: (LineSeg) boxIter.previous();
			if (seg.equals(pCurBoxSeg1))
				break;
		}

		// traverse forward
		do {
			fOffset = nBuffer / (float) pCurBoxSeg1.length();

			Point ptEnd;
			if (bForward) {
				fOffset += 1.0;
				ptEnd = pCurBoxSeg1.getTerminus();
			} else {
				fOffset *= -1.0;
				ptEnd = pCurBoxSeg1.getOrigin();
			}

			if (routePoints.size() > 0)
				nDistance += routePoints.getLastPoint().getDistance(ptEnd);

			if (nBuffer > 0)
				pNewPoint =
					new Point(
						pCurBoxSeg1.locatePoint(
							fOffset,
							nBuffer,
							LineSeg.Sign.POSITIVE));
			else
				pNewPoint = new Point(ptEnd);

			routePoints.addPoint(pNewPoint);

			if (!pCurBoxSeg1.equals(pCurBoxSeg2)) {
				if (bForward) {
					if (!boxIter.hasNext())
						boxIter = rBoxSegments.listIterator();
					pCurBoxSeg1 = (LineSeg) boxIter.next();
				} else {
					if (!boxIter.hasPrevious())
						boxIter =
							rBoxSegments.listIterator(rBoxSegments.size());

					pCurBoxSeg1 = (LineSeg) boxIter.previous();
				}
			}
		} while (!pCurBoxSeg1.equals(pCurBoxSeg2));

		return nDistance;
	}

	/**
	 * Method getRoutedPoints.
	 * Utility function for the routing functions to created a new routed polyline.
	 * 
	 * @param points
	 * @param newRoutePoints
	 * @param ptIntersect1
	 * @param ptIntersect2
	 * @param ptPrev
	 * @param ptNext
	 * @param pCurBoxSeg1
	 * @param pCurBoxSeg2
	 * @param routePoly
	 * @param nSmoothFactor
	 * @param bShortestDistance
	 * @param bIncludeIntersectionPoints
	 * @param nBuffer
	 */
	static private void getRoutedPoints(
		PointList points,
		PointList newRoutePoints,
		final Point ptIntersect1,
		final Point ptIntersect2,
		final Point ptPrev,
		final Point ptNext,
		LineSeg pCurBoxSeg1,
		LineSeg pCurBoxSeg2,
		final PointList routePoly,
		int nSmoothFactor,
		boolean bShortestDistance,
		final boolean bIncludeIntersectionPoints,
		final int nBuffer) {
		PointList newRoutePoints1 = new PointList();
		PointList newRoutePoints2 = new PointList();

		Point ptAbove = new Point(ptIntersect1);
		if (nBuffer > 0) {
			float dDistance = pCurBoxSeg1.distanceAlong(ptIntersect1);
			ptAbove =
				pCurBoxSeg1.locatePoint(
					dDistance,
					nBuffer,
					LineSeg.Sign.POSITIVE);
		}

		newRoutePoints1.addPoint(new Point(ptAbove));
		newRoutePoints2.addPoint(new Point(ptAbove));

		// if we're not including the intersection points we still need to calculate the distance
		int nDistance1 = 0;
		int nDistance2 = 0;

		// figure out which direction to traverse
		LineSeg pCurSeg = pCurBoxSeg1;
		nDistance1
			+= addRoutedPoints(
				newRoutePoints1,
				pCurSeg,
				pCurBoxSeg2,
				routePoly,
				true,
				nBuffer);
		nDistance1 += newRoutePoints1.getLastPoint().getDistance(ptIntersect2);

		pCurSeg = pCurBoxSeg1;
		nDistance2
			+= addRoutedPoints(
				newRoutePoints2,
				pCurSeg,
				pCurBoxSeg2,
				routePoly,
				false,
				nBuffer);
		nDistance2 += newRoutePoints2.getLastPoint().getDistance(ptIntersect2);

		ptAbove = new Point(ptIntersect2);
		if (nBuffer > 0) {
			float dDistance = pCurBoxSeg2.distanceAlong(ptIntersect2);
			ptAbove =
				pCurBoxSeg2.locatePoint(
					dDistance,
					nBuffer,
					LineSeg.Sign.POSITIVE);
		}

		newRoutePoints1.addPoint(new Point(ptAbove));
		newRoutePoints2.addPoint(new Point(ptAbove));

		if ((nDistance1 < nDistance2 && bShortestDistance)
			|| (nDistance1 > nDistance2 && !bShortestDistance)) {
			copyFrom(newRoutePoints, newRoutePoints1);
		} else {
			copyFrom(newRoutePoints, newRoutePoints2);
		}

		// check for redundant points and remove them from the list if necessary.
		if (!bIncludeIntersectionPoints && newRoutePoints.size() >= 3) {
			PointList checkPoints = new PointList(newRoutePoints.size() + 2);

			checkPoints.addPoint(new Point(ptPrev));
			for (int i = 0; i < newRoutePoints.size(); i++)
				checkPoints.addPoint(new Point(newRoutePoints.getPoint(i)));
			checkPoints.addPoint(new Point(ptNext));

			int nIndex = 0;
			Point ptStart = checkPoints.getPoint(nIndex++);
			Point ptCheckSkip = checkPoints.getPoint(nIndex++);
			Point ptEnd = checkPoints.getPoint(nIndex++);

			List polySegments = getLineSegments(routePoly);

			newRoutePoints.removeAllPoints();

			while (ptEnd != null) {
				LineSeg tempSeg = new LineSeg(ptStart, ptEnd);

				// check if we should add the pptCheckSkip to the routePoints or not
				Point ptIntersect = new Point();
				boolean bAddPoint = false;

				// if the temporary line seg does intersect with the routePoly, this means
				// that the ptCheckSkip is needed for the route to work
				ListIterator segIter = polySegments.listIterator();
				while (segIter.hasNext()) {
					LineSeg pPolySegment = (LineSeg) segIter.next();
					ptIntersect = tempSeg.intersect(pPolySegment,INTERSECT_TOLERANCE);
					if (ptIntersect != null) {
						bAddPoint = true;
						break;
					}
				}

				if (bAddPoint) {
					newRoutePoints.addPoint(new Point(ptCheckSkip));
					ptStart = new Point(ptCheckSkip);
				}

				ptCheckSkip = new Point(ptEnd);

				if (nIndex < checkPoints.size())
					ptEnd = checkPoints.getPoint(nIndex++);
				else
					ptEnd = null;
			}
		}

		if (nSmoothFactor > 0) {
			PointList tempPoly =
				calcSmoothPolyline(
					newRoutePoints,
					nSmoothFactor,
					DEFAULT_BEZIERLINES);
			copyFrom(newRoutePoints, tempPoly);
		}
	}

	/**
	 * Method calcSmoothPolyline.
	 * Calculate the smooth polyline approximation of this polyline based on 
	 *      a smooth factor.
	 * 
	 * @param points the <code>PointList</code> that is used to calculate the
	 * smooth point list from.  
	 * @param nSmoothFactor the <code>int</code> smooth factor to smooth the line with 
	 *          0 - None, 15 - some, 30 - lots
	 * @param nBezierSteps the <code>int</code> number of line steps used to approximate the smooth curve
	 * @return PolylinePointList List of PolylinePoint representing the smooth 
	 *      polyline.
	 */
	static public final PointList calcSmoothPolyline(
		PointList points,
		int nSmoothFactor,
		int nBezierSteps) {
		PointList theBezierPoints =
			calcBezier(
				points,
				nSmoothFactor,
				0,
				points.size() - 1);
		return calcApproxPolylineFromBezier(
			theBezierPoints,
			nBezierSteps);
	}

	/**
	 * Calculates the smooth polyline equivalent of the given points list.  It will extrapolate a bezier
	 * approximation in polylines that resembles a curved line.
	 * 
	 * @param points the <code>PointList</code> that is used to calculate the smooth bezier approximation.
	 * @param nSmoothFactor the <code>int</code> smooth factor to smooth the line with 
	 *          0 - None, 15 - some, 30 - lots
	 * @param nBezierSteps the <code>int</code> number of line steps used to approximate the smooth curve
	 * @param nStartIndex the <code>int</code> index in the line to start from to create the smooth approximation
	 * @param nEndIndex the <code>int</code> index in the line to end at to create the smooth approximation
	 * @return PolylinePointList List of PolylinePoint representing the smooth 
	 *      polyline.
	 */
	static public PointList calcSmoothPolyline(
		PointList points,
		int nSmoothFactor,
		int nBezierSteps,
		int nStartIndex,
		int nEndIndex) {
		PointList theBezierPoints =
			calcBezier(
				points,
				nSmoothFactor,
				nStartIndex,
				nEndIndex);
		return calcApproxPolylineFromBezier(
			theBezierPoints,
			nBezierSteps);
	}

	/**
	 * Calculate the actual bezier points of this polyline based on a smooth factor.
	 * 
	 * @param points PointList to calculate the bezier approximation from
	 * @param nSmoothFactor the <code>int</code> smooth factor to smooth the line with 
	 *          0 - None, 15 - some, 30 - lots
	 * @param nStartIndex int Index to start the calculation at
	 * @param nEndIndex int Index to end the calculation at
	 * @return PolylinePointList List of PolylinePoint representing the bezier.
	 */
	static private PointList calcBezier(
		final PointList points,
		int nSmoothFactor,
		int nStartIndex,
		int nEndIndex) {
		Point ptPrevControl = null;
		PointList theBezierPoints = new PointList(points.size() * 2);
		// parse through the line segments and create control points based on a smoothing factor.
		List theSegments = getLineSegments(points);

		for (int i = 0; i < theSegments.size(); i++) {
			LineSeg pLineSeg = (LineSeg) theSegments.get(i);

			double dLineLength = pLineSeg.length();
			double dControlLength = dLineLength * nSmoothFactor / 100;

			boolean bAddToBezier = false;
			if (i >= nStartIndex && i <= nEndIndex)
				bAddToBezier = true;
			else if (i > nEndIndex)
				return theBezierPoints;

			if (dLineLength > MIN_LINE_LENGTH) {
				Point ptStartControl = new Point();
				Point ptTerminusControl = new Point();
				Point ptStart = new Point(pLineSeg.getOrigin());
				Point ptTerminus = new Point(pLineSeg.getTerminus());

				if (theBezierPoints.size() == 0) {
					if (bAddToBezier)
						theBezierPoints.addPoint(ptStart);
				}

				if (ptPrevControl != null) {
					LineSeg prevControlSeg =
						new LineSeg(ptPrevControl, ptStart);
					ptStartControl = new Point();
					prevControlSeg.pointOn(
						(int) Math.round(
							prevControlSeg.length() + dControlLength),
						LineSeg.KeyPoint.ORIGIN,
						ptStartControl);
				} else {
					ptStartControl = new Point();
					pLineSeg.pointOn(
						(int) Math.round(dControlLength),
						LineSeg.KeyPoint.ORIGIN,
						ptStartControl);
				}
				if (bAddToBezier)
					theBezierPoints.addPoint(ptStartControl);

				// Calculate the next terminating control point
				LineSeg pNextSeg = null;
				if (i + 1 < theSegments.size()) {
					pNextSeg = (LineSeg) theSegments.get(i + 1);
					while (pNextSeg != null
						&& pNextSeg.length() < MIN_LINE_LENGTH) {
						i++;
						if (i + 1 < theSegments.size())
							pNextSeg = (LineSeg) theSegments.get(i + 1);
						else
							pNextSeg = null;
					}
				}

				if (pNextSeg != null) {
					// compute two vectors and calculate the angle between them.
					Ray ptVector1 =
						new Ray(pLineSeg.getOrigin(), pLineSeg.getTerminus());
					Ray ptVector2 =
						new Ray(pNextSeg.getOrigin(), pNextSeg.getTerminus());
					double dNewAngle = 0.0;

					LineSeg.TrigValues val = pLineSeg.getTrigValues(ptVector2);

					dNewAngle = Math.atan2(-val.sinTheta, -val.cosTheta);

					if (dNewAngle > 0)
						dNewAngle = (Math.PI - dNewAngle) / -2;
					else
						dNewAngle = (-Math.PI - dNewAngle) / -2;

					Transform trans = new Transform();
					trans.setRotation(dNewAngle);
					Point ptVector1Prime =
						trans.getTransformed(
							new Point(ptVector1.x, ptVector1.y));

					LineSeg nextControlSeg =
						new LineSeg(
							new Point(0, 0),
							new Point(ptVector1Prime.x, ptVector1Prime.y));
					Point ptProjection = new Point();
					nextControlSeg.pointOn(
						(int) Math.round(dControlLength),
						LineSeg.KeyPoint.ORIGIN,
						ptProjection);

					ptTerminusControl =
						new Point(
							pLineSeg.getTerminus().x - ptProjection.x,
							pLineSeg.getTerminus().y - ptProjection.y);
				} else {
					pLineSeg.pointOn(
						(int) Math.round(dLineLength - dControlLength),
						LineSeg.KeyPoint.ORIGIN,
						ptTerminusControl);
				}

				ptPrevControl = new Point(ptTerminusControl);
				if (bAddToBezier) {
					theBezierPoints.addPoint(ptTerminusControl);
					theBezierPoints.addPoint(ptTerminus);
				}
			}
		}

		return theBezierPoints;
	}

	/**
	 * Utility function that takes a set of bezier points and calculates a polyline 
	 * approximation.
	 * 
	 * @param points the <code>PointList</code> to calculate the bezier from.
	 * @param nBezierSteps the <code>int</code> number of line steps that will be used
	 *      to approximate each bezier curve
	 * @return PolylinePointList List of PolylinePoint representing the smooth 
	 *      polyline.
	 */
	static private PointList calcApproxPolylineFromBezier(
		final PointList points,
		int nBezierSteps) {
		PointList thePolyPoints =
			new PointList(points.size() * nBezierSteps + 2);

		Point ptCtl1, ptCtl2, ptCtl3, ptCtl4;
		boolean bStart = true;

		if (points.size() < 4)
			return thePolyPoints;

		ptCtl1 = new Point();
		for (int i = 0; i < points.size() - 3; i = i + 3) {
			if (bStart) {
				ptCtl1 = new Point(points.getPoint(i));
				bStart = false;
			} else {
				thePolyPoints.removePoint(thePolyPoints.size() - 1);
			}

			ptCtl2 = new Point(points.getPoint(i + 1));
			ptCtl3 = new Point(points.getPoint(i + 2));
			ptCtl4 = new Point(points.getPoint(i + 3));

			if (!BezierToLines(thePolyPoints,
				ptCtl1,
				ptCtl2,
				ptCtl3,
				ptCtl4,
				nBezierSteps))
				return null;

			ptCtl1 = new Point(ptCtl4);
		}

		// now construct the PolyLine based on the points
		thePolyPoints.setPoint(points.getPoint(0), 0);
		thePolyPoints.setPoint(
			points.getPoint(points.size() - 1),
			thePolyPoints.size() - 1);

		return thePolyPoints;
	}

	/**
	 * Method BezierToLines.
	 * Utility function that takes a set of bezier points and calculates a polyline 
	 * approximation.
	 * 
	 * @param thePolyPoints
	 * @param ptCtl1
	 * @param ptCtl2
	 * @param ptCtl3
	 * @param ptCtl4
	 * @param nSteps
	 * @return boolean
	 */
	private static boolean BezierToLines(
		PointList thePolyPoints,
		Point ptCtl1,
		Point ptCtl2,
		Point ptCtl3,
		Point ptCtl4,
		int nSteps) {
		boolean bRC = true;

		int nTotalPoints;
		/* total number of coordinate pairs in working arrays */
		int nLinePoints;
		/* total number of bezier endpoints in working arrays */
		double[] lpWorkX; /* pointer to X coordinate working array */
		double[] lpWorkY; /* pointer to Y coordinate working array */
		int i, j;

		nSteps = Math.min(MAX_BEZIERLINES, nSteps);

		/* get pointers to working arrays in workspace */
		lpWorkX = new double[3 * nSteps + 2];
		lpWorkY = new double[3 * nSteps + 2];

		/* enter original bezier X coordinates into X working array */
		lpWorkX[0] = ptCtl1.x;
		lpWorkX[1] = ptCtl2.x;
		lpWorkX[2] = ptCtl3.x;
		lpWorkX[3] = ptCtl4.x;

		/* enter original bezier Y coordinates into Y working array */
		lpWorkY[0] = ptCtl1.y;
		lpWorkY[1] = ptCtl2.y;
		lpWorkY[2] = ptCtl3.y;
		lpWorkY[3] = ptCtl4.y;

		/* initially 2 bezier endpoints and 4 coordinate pairs total */
		nLinePoints = 2;
		nTotalPoints = 4;

		/* generate enough bezier endpoints to satisfy requested number of steps */

		while (nLinePoints <= nSteps) {
			/* spread coordinate pairs in working array */

			for (i = nTotalPoints - 1; i > 0; i--) {
				lpWorkX[2 * i] = lpWorkX[i];
				lpWorkY[2 * i] = lpWorkY[i];
			}

			nTotalPoints = (2 * nTotalPoints) - 1;

			/* average consecutive coordinates and put average between coordinates */

			for (i = nTotalPoints - 2; i > 0; i -= 2) {
				lpWorkX[i] = (lpWorkX[i - 1] + lpWorkX[i + 1]) / 2;
				lpWorkY[i] = (lpWorkY[i - 1] + lpWorkY[i + 1]) / 2;
			}

			/* now average averages and store over control points */
			/* but do not overwrite bezier endpoints */

			for (i = nTotalPoints - 3; i > 0; i -= 2) {
				/* only control points */

				if ((i % 6) != 0) {
					lpWorkX[i] = (lpWorkX[i - 1] + lpWorkX[i + 1]) / 2;
					lpWorkY[i] = (lpWorkY[i - 1] + lpWorkY[i + 1]) / 2;
				}
			}

			/* lastly overwrite midpoint of control points with */
			/* average of previous calculation, yeilds midpoint on bezier */

			for (i = nTotalPoints - 4; i > 0; i -= 6) {
				lpWorkX[i] = (lpWorkX[i - 1] + lpWorkX[i + 1]) / 2;
				lpWorkY[i] = (lpWorkY[i - 1] + lpWorkY[i + 1]) / 2;
			}

			nLinePoints = (nTotalPoints / 3) + 1;
		}

		/* have enough bezier endpoints, move bezier endpoints to */
		/* polyline CGIPOINTs array */

		for (i = 0; i < nSteps; i++) {
			Point ptTemp = new Point();

			j = (3 * i);
			// consider converting to a float & running through the point convertor if needed */
			ptTemp.x = (int) Math.round(lpWorkX[j]);
			ptTemp.y = (int) Math.round(lpWorkY[j]);

			thePolyPoints.addPoint(ptTemp);
		}

		return (bRC);
	}

	/**
	 * Calculate the point on the polyline given a distance from a key point.
	 * 
	 * @param points the <code>PointList</code> to calculate the point on the polyline.
	 * @param theDistance the <code>long</code> x coordinate of the point
	 * @param fromKeyPoint the <code>int</code> constant value indicating the key point
	 *      Origin, Terminus, Midpoint
	 * @param ptResult the <code>Point</code> where the resulting point value is set.
	 * @return Point the calculated point residing on the polyline.
	 */
	static public Point pointOn(
		PointList points,
		final long theDistance,
		final LineSeg.KeyPoint fromKeyPoint,
		Point ptResult) {
		List mySegments = getLineSegments(points);
		return pointOn(mySegments, theDistance, fromKeyPoint, ptResult);
	}

	/**
	 * Method getPointsLength.
	 * Static utility function to calculate the length of a series of
	 * points if interpreted as line segments.
	 * 
	 * @param points PointList to calculate the length of.
	 * @return length of the line segments
	 */
	public static long getPointsLength(PointList points) {
		List segs = getLineSegments(points);

		return length(segs);
	}

	/**
	 * Method length.
	 * Static utility function to calculate the length of a series of
	 * line segments.
	 * @param mySegments List of line segments to calculate the length of.
	 * @return long length of the line segments
	 */
	protected static long length(List mySegments) {
		long theLength = 0;
		ListIterator lineIter = mySegments.listIterator();
		while (lineIter.hasNext()) {
			LineSeg aSegment = (LineSeg) lineIter.next();
			theLength += Math.round(aSegment.length());
		}

		return theLength;
	}

	private final static int BIGDISTANCE = 32766;

	/**
	 * Static utility method to get the nearest segment in the polyline 
	 * from the given coordinates.
	 * 
	 * @param mySegments the <code>List</code> of <code>LineSeg</code> objects
	 * @param xCoord the <code>int</code> x coordinate of the point
	 * @param yCoord the <code>int</code> y coordinate of the point
	 * @return the <code>LineSeg</code> by reference which is closest to the given coordinates.
	 */
	public static LineSeg getNearestSegment(
		List mySegments,
		final int xCoord,
		final int yCoord) {
		long minDistance = BIGDISTANCE;
		long nextDistance = 0;

		LineSeg closeSegment = null;
		LineSeg firstSegment =
			mySegments.isEmpty() ? null : (LineSeg) mySegments.get(0);

		ListIterator lineIter = mySegments.listIterator();
		while (lineIter.hasNext()) {
			LineSeg aSegment = (LineSeg) lineIter.next();
			nextDistance = aSegment.distanceToPoint(xCoord, yCoord);

			if (nextDistance < minDistance) {
				closeSegment = aSegment;
				minDistance = nextDistance;
			}
		}

		if (closeSegment != null)
			return closeSegment;

		// prevent crash
		return firstSegment;
	}

	/**
	 * Method pointOn.
	 * Static utility function used by <code>pointOn</code>.
	 * @param mySegments
	 * @param theDistance the <code>long</code> x coordinate of the point
	 * @param fromKeyPoint the <code>int</code> constant value indicating the key point
	 *      Origin, Terminus, Midpoint
	 * @param ptResult the <code>Point</code> where the resulting point value is set.
	 * @return Point the calculated point residing on the polyline.
	 */
	protected static Point pointOn(
		List mySegments,
		final long theDistance,
		final LineSeg.KeyPoint fromKeyPoint,
		Point ptResult) {
		long thisLength = length(mySegments);
		long halfLength = thisLength / 2;

		if (theDistance >= thisLength) {
			if (fromKeyPoint == LineSeg.KeyPoint.ORIGIN) {
				((LineSeg) mySegments.get(mySegments.size() - 1)).pointOn(
					theDistance - thisLength,
					LineSeg.KeyPoint.TERMINUS,
					ptResult);
				return ptResult;
			} else if (fromKeyPoint == LineSeg.KeyPoint.MIDPOINT) {
				((LineSeg) mySegments.get(mySegments.size() - 1)).pointOn(
					theDistance - halfLength,
					LineSeg.KeyPoint.TERMINUS,
					ptResult);
				return ptResult;
			} else if (fromKeyPoint == LineSeg.KeyPoint.TERMINUS) {
				((LineSeg) mySegments.get(mySegments.size() - 1)).pointOn(
					theDistance,
					LineSeg.KeyPoint.TERMINUS,
					ptResult);
				return ptResult;
			} else {
				IllegalArgumentException iae = new IllegalArgumentException();
				Trace.throwing(Draw2dPlugin.getInstance(), Draw2dDebugOptions.EXCEPTIONS_THROWING, PointListUtilities.class, "pointOn()", //$NON-NLS-1$
				iae);
				throw iae;
			}
		} else if (theDistance < 0) {
			if (fromKeyPoint == LineSeg.KeyPoint.ORIGIN) {
				((LineSeg) mySegments.get(0)).pointOn(
					theDistance,
					fromKeyPoint,
					ptResult);
				return ptResult;
			} else if (fromKeyPoint == LineSeg.KeyPoint.MIDPOINT) {
				return pointOn(
					mySegments,
					halfLength + theDistance,
					LineSeg.KeyPoint.ORIGIN,
					ptResult);
			} else if (fromKeyPoint == LineSeg.KeyPoint.TERMINUS) {
				((LineSeg) mySegments.get(mySegments.size() - 1)).pointOn(
					theDistance,
					fromKeyPoint,
					ptResult);
				return ptResult;
			} else {
				IllegalArgumentException iae = new IllegalArgumentException();
				Trace.throwing(Draw2dPlugin.getInstance(), Draw2dDebugOptions.EXCEPTIONS_THROWING, PointListUtilities.class, "pointOn()", //$NON-NLS-1$
				iae);
				throw iae;
			}
		} else {
			LocateInfo locateInfo = new LocateInfo();
			if (!locateSegment(mySegments,
				((double) theDistance) / ((double) thisLength),
				fromKeyPoint,
				locateInfo))
				return null;

			locateInfo.theSegment.pointOn(
				locateInfo.remainingDist,
				(fromKeyPoint == LineSeg.KeyPoint.MIDPOINT
					? LineSeg.KeyPoint.ORIGIN
					: fromKeyPoint),
				ptResult);
			return ptResult;
		}
	}

	static private class LocateInfo {
		
		/**
		 * remainin distance
		 */
		public long remainingDist;

		/**
		 * the line segment.
		 */
		public LineSeg theSegment;
	}

	/**
	 * Find the line segment of this polyline that includes the given
	 * percentage of the length of the polyline from the given keypoint.
	 * Return a pointer to the segment and the portion of the length
	 * of the return segment to complete the distance of the total length.
	 * The pctDist must be between 0.0 and 1.0 if the keypoint is either
	 * the origin or the terminus; pctDist must be between 0.0 and 0.5
	 * if the keypoint is the midpoint.
	 * 
	 * @param mySegments
	 * @param pctDist
	 * @param fromKeyPoint the <code>int</code> constant value indicating the key point
	 *      Origin, Terminus, Midpoint
	 * @param locateInfo LocateInfo where the calculated info is stored for return.
	 * @return boolean true if the segment could be located, false otherwise.
	 */
	private static boolean locateSegment(
		List mySegments,
		final double pctDist,
		final LineSeg.KeyPoint fromKeyPoint,
		LocateInfo locateInfo) {
		// Formerly asserted: ASSERT (0.0 <= pctDist && pctDist <= 1.0);
		// but got one crash reported by a customer in apparently normal
		// circumstances, so enforce the condition.
		double thePctDist = pctDist;
		if (pctDist < 0.0) {
			thePctDist = 0.0;
		} else if (1.0 < pctDist) {
			thePctDist = 1.0;
		}

		final long theLength = length(mySegments);
		long remainingLength = Math.round(thePctDist * theLength);
		long nextLength = 0;

		locateInfo.theSegment = null;

		if (fromKeyPoint == LineSeg.KeyPoint.MIDPOINT
			|| fromKeyPoint == LineSeg.KeyPoint.ORIGIN) {

			if (fromKeyPoint == LineSeg.KeyPoint.MIDPOINT) {
				// formerly asserted: ASSERT (pctDist <= 0.5); but not
				// really essential to the algorithm

				// Let's be tricky. Since locating from the midpoint moves in
				// the direction of the terminus let's just change the distance
				// and locate from the origin.
				remainingLength += theLength / 2;
			}

			ListIterator lineIter = mySegments.listIterator();
			while (lineIter.hasNext()) {
				LineSeg aSegment = (LineSeg) lineIter.next();

				nextLength = Math.round(aSegment.length());
				if (nextLength >= remainingLength) {
					locateInfo.theSegment = aSegment;
					break;
				} else {
					remainingLength -= nextLength;
				}
			}
		} else if (fromKeyPoint == LineSeg.KeyPoint.TERMINUS) {
			ListIterator lineIter = mySegments.listIterator(mySegments.size());
			while (lineIter.hasPrevious()) {
				LineSeg aSegment = (LineSeg) lineIter.previous();

				nextLength = Math.round(aSegment.length());
				if (nextLength >= remainingLength) {
					locateInfo.theSegment = aSegment;
					break;
				} else {
					remainingLength -= nextLength;
				}
			}
		} else {
			IllegalArgumentException iae = new IllegalArgumentException();
			Trace.throwing(Draw2dPlugin.getInstance(), Draw2dDebugOptions.EXCEPTIONS_THROWING, PointListUtilities.class, "pointOn()", //$NON-NLS-1$
			iae);
			throw iae;
		}

		locateInfo.remainingDist = remainingLength;

		return true;
	}

	/**
	 * Method distanceAlong.
	 * Static utility function to calculate the distanceAlong a series of line segments
	 * a given point is on the line.
	 * @param mySegments
	 * @param aPoint Point to calculate the distance along the polyline of.
	 * @return double value of the distance along the polyline of the given point.
	 */
	protected static double distanceAlong(
		List mySegments,
		final Point aPoint) {
		LineSeg theSegment = getNearestSegment(mySegments, aPoint.x, aPoint.y);
		double linePct =
			segmentDistance(mySegments, theSegment, LineSeg.KeyPoint.ORIGIN);
		double segmentPct = theSegment.distanceAlong(aPoint);

		if (0.0 <= segmentPct && segmentPct <= 1.0) {
			long polyLength = length(mySegments);
			if (polyLength != 0) {
				linePct
					+= (segmentPct
						* (theSegment.length() / polyLength));
			}
			return linePct;
		} else {
			return segmentPct;
		}
	}

	/**
	 * Method segmentDistance.
	 * Compute the percentage of the distance along this polyline
	 * that the given segment occurs. If the keypoint specified is
	 * Origin, then the given segment will not be included in the
	 * percentage; if the keypoint is Midpoint, then half of the
	 * length will be included; if the keypoint is Terminus, the
	 * the whole length will be included. The segment must be a
	 * segment of this polyline.
	 * @param mySegments
	 * @param theSegment
	 * @param uptoKeyPoint
	 * @return double
	 */
	protected static double segmentDistance(
		List mySegments,
		LineSeg theSegment,
		final LineSeg.KeyPoint uptoKeyPoint) {
		long accumulatedLength = 0;

		ListIterator lineIter = mySegments.listIterator();
		while (lineIter.hasNext()) {
			LineSeg aSegment = (LineSeg) lineIter.next();

			if (theSegment.equals(aSegment)) {
				if (uptoKeyPoint == LineSeg.KeyPoint.ORIGIN) {
					// empty block
				} else if (uptoKeyPoint == LineSeg.KeyPoint.MIDPOINT) {
					accumulatedLength += aSegment.length() / 2;
				} else if (uptoKeyPoint == LineSeg.KeyPoint.TERMINUS) {
					accumulatedLength += aSegment.length();
				} else {
					IllegalArgumentException iae =
						new IllegalArgumentException();
					Trace.throwing(Draw2dPlugin.getInstance(), Draw2dDebugOptions.EXCEPTIONS_THROWING, PointListUtilities.class, "pointOn()", //$NON-NLS-1$
					iae);
					throw iae;
				}

				long polyLength = length(mySegments);
				if (polyLength != 0) {
					return ((double) accumulatedLength / (double) polyLength);
				} else {
					// RJ: removed the ASSERT for now
					return 0.0;
				}
			} else {
				accumulatedLength += aSegment.length();
			}
		}

		return 0.0;
	}

	/**
	 * Method routeAroundPoint.
	 * Calculate a new routed version of this polyline that will route itself around a 
	 * given point.
	 * 
	 * @param points PointList to modify
	 * @param ptCenter the <code>Point</code> around which the routing will occur.
	 * @param nHeight the <code>int</code> height to route around the point.
	 * @param nWidth the <code>int</code> width to route around the point.
	 * @param nSmoothFactor the <code>int</code> smooth factor to route the line with 
	 *          0 - None, 15 - some, 30 - lots
	 * @param nInclineOffset the <code>int</code> amount to incline the routed points.
	 * @param bTop the <code>boolean</code> route above or below the point on the line.
	 * @return <code>PointList</code> that is the newly routed version of <code>points</code>
	 * of <code>null</code> if operation was not successful or if the calculation is not possible.
	 */
	static public PointList routeAroundPoint(
		PointList points,
		final Point ptCenter,
		int nHeight,
		int nWidth,
		int nSmoothFactor,
		int nInclineOffset,
		boolean bTop) {
		List mySegments = getLineSegments(points);

		long nPolyLength = length(mySegments);
		long nCenterDistance =
			Math.round(
				distanceAlong(mySegments, ptCenter)
					* nPolyLength);

		Point ptMidStart = new Point();
		pointOn(
			mySegments,
			nCenterDistance - (nWidth / 2),
			LineSeg.KeyPoint.ORIGIN,
			ptMidStart);
		Point ptMidEnd = new Point();
		pointOn(
			mySegments,
			nCenterDistance + (nWidth / 2),
			LineSeg.KeyPoint.ORIGIN,
			ptMidEnd);
		LineSeg lineNew = new LineSeg(ptMidStart, ptMidEnd);

		Point ptStart = new Point();
		lineNew.pointOn(nInclineOffset, LineSeg.KeyPoint.ORIGIN, ptStart);
		LocateInfo locateInfo = new LocateInfo();
		if (!locateSegment(mySegments,
			(nCenterDistance - ((long) nWidth / 2)) / (double) nPolyLength,
			LineSeg.KeyPoint.ORIGIN,
			locateInfo))
			return null;
		LineSeg pStartSeg = locateInfo.theSegment;

		Point ptEnd = new Point();
		lineNew.pointOn(nInclineOffset, LineSeg.KeyPoint.TERMINUS, ptEnd);
		if (!locateSegment(mySegments,
			(nCenterDistance + ((long) nWidth / 2)) / (double) nPolyLength,
			LineSeg.KeyPoint.ORIGIN,
			locateInfo))
			return null;
		LineSeg pEndSeg = locateInfo.theSegment;

		// figure out which side to route around
		float fSlope = lineNew.slope();
		int nDir = 1;
		if ((bTop && fSlope <= 0) || (!bTop && fSlope > 0))
			nDir *= -1;

			LineSeg lineStart =
				new LineSeg(LineSeg.KeyPoint.ORIGIN, // Origin or Midpoint
	ptStart.x, ptStart.y, lineNew.perpSlope(), nHeight, nDir);

			LineSeg lineEnd =
				new LineSeg(LineSeg.KeyPoint.ORIGIN, // Origin or Midpoint
	ptEnd.x, ptEnd.y, lineNew.perpSlope(), nHeight, nDir);

		PointList rRotatedBox = new PointList();
		rRotatedBox.addPoint(new Point(ptMidStart));
		rRotatedBox.addPoint(new Point(lineStart.getTerminus()));
		rRotatedBox.addPoint(new Point(lineEnd.getTerminus()));
		rRotatedBox.addPoint(new Point(ptMidEnd));
		rRotatedBox.addPoint(new Point(ptMidStart));

		PointList rPolyPoints =
			new PointList(rRotatedBox.size() * MAX_BEZIERLINES + points.size());
		boolean bFoundStart = false;
		boolean bFoundEnd = false;
		int nPointsSinceStart = 0;

		List boxSegments = getLineSegments(rRotatedBox);
		ListIterator lineIter = mySegments.listIterator();
		while (lineIter.hasNext()) {
			LineSeg pSegment = (LineSeg) lineIter.next();

			if (pSegment.equals(pStartSeg)) {
				rPolyPoints.addPoint(new Point(pSegment.getOrigin()));
				bFoundStart = true;
			}

			if (pSegment == pEndSeg) {
				PointList newRoutePoints =
					new PointList(rRotatedBox.size() * MAX_BEZIERLINES);

				LineSeg pCurSeg1 = (LineSeg) boxSegments.get(0);
				LineSeg pCurSeg2 =
					(LineSeg) boxSegments.get(boxSegments.size() - 1);
				getRoutedPoints(
					points,
					newRoutePoints,
					ptMidStart,
					ptMidEnd,
					ptMidStart,
					ptMidEnd,
					pCurSeg1,
					pCurSeg2,
					rRotatedBox,
					nSmoothFactor,
					false,
					true,
					0);

				while (nPointsSinceStart > 0) {
					rPolyPoints.removePoint(rPolyPoints.size() - 1);
					nPointsSinceStart--;
				}

				for (int i = 0; i < newRoutePoints.size(); i++)
					rPolyPoints.addPoint(new Point(newRoutePoints.getPoint(i)));
				rPolyPoints.addPoint(new Point(pSegment.getTerminus()));
				bFoundEnd = true;
			} else {
				rPolyPoints.addPoint(new Point(pSegment.getOrigin()));

				if (bFoundStart)
					nPointsSinceStart++;

				if (!lineIter.hasNext())
					rPolyPoints.addPoint(new Point(pSegment.getTerminus()));
			}

		}

		if (bFoundEnd)
			return rPolyPoints;

		return null;
	}

	/**
	 * Method findNearestLineSegIndexOfPoint.
	 * Calculate the nearest line segment index distance wise to the given point.
	 * 
	 * @param points PointList to calculate the nearest line segment of.
	 * @param ptCoord the <code>Point</code> to test containment of.
	 * @return int Index of line segment that is nearest in the polyline to the given point.
	 * The index is 1 based where 1 represents the first segment.
	 */
	static public int findNearestLineSegIndexOfPoint(
		PointList points,
		final Point ptCoord) {
		List mySegments = getLineSegments(points);
		ListIterator lineIter = mySegments.listIterator();
		int nNextIndex = 0;
		int nMinIndex = 0;
		long minDistance = BIGDISTANCE;
		long nextDistance = 0;

		while (lineIter.hasNext()) {
			LineSeg aSegment = (LineSeg) lineIter.next();
			nNextIndex++;

			nextDistance = aSegment.distanceToPoint(ptCoord.x, ptCoord.y);

			if (nextDistance < minDistance) {
				minDistance = nextDistance;
				nMinIndex = nNextIndex;
			}
		}

		return nMinIndex;
	}

	/**
	 * Method findIntersections.
	 * Find all intersection points between this polyline and another polyline passed
	 * into the method.
	 * 
	 * @param points PointList to calculate interesections with.
	 * @param poly the <code>PointList</code> to calculate intersections with.
	 * @param intersections the <code>PointList</code> containing the resulting calculated 
	 * intersection points.
	 * @param distances the <code>PointList</code> containing point values representing the
	 * distance along the polyline the intersections occur.
	 * @return boolean true if intersections could be calculated, false otherwise.
	 */
	static public boolean findIntersections(
		PointList points,
		final PointList poly,
		PointList intersections,
		PointList distances) {
		List polySegments = getLineSegments(poly);
		List mySegments = getLineSegments(points);

		Point pLastIntersect = null;

		double dCurrentLength = 0;

		ListIterator segIter = mySegments.listIterator();
		while (segIter.hasNext()) {
			LineSeg pSegment = (LineSeg) segIter.next();

			double dSegLength = pSegment.length();

			ListIterator polyIter = polySegments.listIterator();
			while (polyIter.hasNext()) {
				LineSeg pPolySegment = (LineSeg) polyIter.next();

				Point ptIntersect = pSegment.intersect(pPolySegment, INTERSECT_TOLERANCE);

				// check if this segment intersects with the box
				if (ptIntersect != null) {
					boolean bAddIntersect = true;

					if (pLastIntersect != null) {
						// check case where intersect is on a corner - causing intersect
						// to show up in two adjacent line segments
						if (Math.abs(pLastIntersect.x - ptIntersect.x)
							< (INTERSECT_TOLERANCE * 2)
							&& Math.abs(pLastIntersect.y - ptIntersect.y)
								< (INTERSECT_TOLERANCE * 2))
							bAddIntersect = false;
					}

					if (bAddIntersect) {
						pLastIntersect = new Point(ptIntersect);
						intersections.addPoint(pLastIntersect);

						Point ptDistance = new Point(0, 0);
						ptDistance.x =
							(int) Math.round(
								dCurrentLength
									+ pSegment.distanceAlong(ptIntersect)
										* dSegLength);
						distances.addPoint(ptDistance);
					}
				}
			}

			dCurrentLength += dSegLength;

		}

		return intersections.size() > 0;
	}

	/**
	 * Finds a point relative to the pointList passed in based on the parameters passed in.
	 * 
	 * @param pointList the <code>PointList</code> 
	 * @param fromLine distance off the line 
	 * @param fromEnd distance from the starting point of the line (i.e. distance from source end along the line)
	 * @param isPercentage is the fromEnd given as a percentage?
	 * @return Point
	 */
	public static Point calculatePointRelativeToLine(
		PointList pointList,
		int fromLine,
		int fromEnd,
		boolean isPercentage) {
		double fractionDistance = 0;
		if (isPercentage) {
			fractionDistance = fromEnd / 100.0;
		} else {
			fractionDistance = (double) fromEnd / (double) pointList.size();
		}

		LocateInfo locateInfo =
			new LocateInfo();
		if (locateSegment(
				getLineSegments(pointList),
				fractionDistance,
				LineSeg.KeyPoint.ORIGIN,
				locateInfo)) {
			double inSegPercDist = 0;
			LineSeg seg = locateInfo.theSegment;
			if (seg.length() > 0)
				inSegPercDist =
					locateInfo.remainingDist
						/ seg.length();
			// relative position is coded as the the sign of the height
			Point location =
				seg.locatePoint(
					inSegPercDist,
					Math.abs(fromLine),
					(fromLine > 0
						? LineSeg.Sign.POSITIVE
						: LineSeg.Sign.NEGATIVE));
			return location;
		}
		return null;
	}
	
	/**
	 * Assumption: Points in the <Code>PointList</Code> and <Code>Point</Code> p lie on the same line.
	 * Returns the <Code>Point</Code> from the <Code>PointList</Code> closest to @param p
	 * 
	 * @param points - the list of points to select the result from
	 * @param p - the point to which the closest point must be found
	 * @return the <Code>Point</Code> from the <Code>PointList</Code> closest to @param p
	 */
	public static Point pickClosestPoint(PointList points, Point p) {
		Point result = points.getFirstPoint();
		for (int i=1; i<points.size(); i++) {
			Point temp = points.getPoint(i);
			if (Math.abs(temp.x - p.x) < Math.abs(result.x - p.x))
				result = temp;
			else if (Math.abs(temp.y - p.y) < Math.abs(result.y - p.y))
				result = temp;
		}
		return result;
	}
	
	/**
	 * Assumption: Points in the <Code>PointList</Code> and <Code>Point</Code> p lie on the same line.
	 * Returns the <Code>Point</Code> from the <Code>PointList</Code> closest to @param p
	 * 
	 * @param points - the list of points to select the result from
	 * @param p - the point to which the closest point must be found
	 * @return the <Code>Point</Code> from the <Code>PointList</Code> closest to @param p
	 */
	public static Point pickFarestPoint(PointList points, Point p) {
		Point result = points.getFirstPoint();
		for (int i=1; i<points.size(); i++) {
			Point temp = points.getPoint(i);
			if (Math.abs(temp.x - p.x) > Math.abs(result.x - p.x))
				result = temp;
			else if (Math.abs(temp.y - p.y) > Math.abs(result.y - p.y))
				result = temp;
		}
		return result;
	}
	
	/**
	 * Method sameOrientation.
	 * @param pt1
	 * @param pt2
	 * @param pt3
	 * @return boolean true if the line connecting all three points are 
	 *      straight, false otherwise.
	 */
	static boolean sameOrientation(Point pt1, Point pt2, Point pt3, int straightLineTolerance) {
		LineSeg line = new LineSeg(pt1, pt3);
		Point pt = line.perpIntersect(pt2.x, pt2.y);
		return Math.round(pt.getDistance(new Point(pt2.x, pt2.y))) < straightLineTolerance;
	}
}