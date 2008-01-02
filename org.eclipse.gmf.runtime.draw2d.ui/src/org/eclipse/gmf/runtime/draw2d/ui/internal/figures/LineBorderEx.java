/******************************************************************************
 * Copyright (c) 2002, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/


package org.eclipse.gmf.runtime.draw2d.ui.internal.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gmf.runtime.draw2d.ui.mapmode.MapModeUtil;
import org.eclipse.swt.graphics.Color;



/**
 * This subclass of the LineBorder is required to provide MapMode support.
 * Without this the line border around figures using Hi-Metrics is displayed
 * incorrectly.
 * 
 * @author jschofie
 * @canBeSeenBy org.eclipse.gmf.runtime.draw2d.ui.*
 */
public class LineBorderEx
	extends LineBorder {

	/**
	 * Constructs a default black LineBorder with a width of one pixel.
	 */
	public LineBorderEx() {
		super();
	}

	/**
	 * Constructs a black LineBorder with a specified width
	 * @param width width of the line border in pixels
	 */
	public LineBorderEx(int width) {
		super(width);
	}

	/**
	 * Constructs a LineBorder with a width of 1 pixel using the color specified
	 * @param color
	 */
	public LineBorderEx(Color color) {
		super(color);
	}

	/**
	 * Construct a LineBorder with a given color and width
	 * @param color
	 * @param width width of the line border in pixels
	 */
	public LineBorderEx(Color color, int width) {
		super(color, width);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.LineBorder#paint(org.eclipse.draw2d.IFigure, org.eclipse.draw2d.Graphics, org.eclipse.draw2d.geometry.Insets)
	 */
	public void paint(IFigure figure, Graphics graphics, Insets insets) {
		tempRect.setBounds(getPaintRectangle(figure, insets));
		if (getWidth() % 2 == 1) {
			tempRect.width -= MapModeUtil.getMapMode(figure).DPtoLP(1);
			tempRect.height -= MapModeUtil.getMapMode(figure).DPtoLP(1);
		}
		int shrinkWidth = MapModeUtil.getMapMode(figure).DPtoLP( getWidth() / 2 );
		tempRect.shrink(shrinkWidth, shrinkWidth);

		graphics.setLineWidth(getWidth());
		if (getColor() != null)
			graphics.setForegroundColor(getColor());

		graphics.drawRectangle(tempRect);
	}
}