/******************************************************************************
 * Copyright (c) 2002, 2005  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package org.eclipse.gmf.runtime.draw2d.ui.figures;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gmf.runtime.draw2d.ui.mapmode.MapModeUtil;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;


/**
 * An extended label that has the following extra features:
 * 
 * 1- It is capable of showing selection and focus feedback (primary or
 * secondary) 2- It is capable of optionally underlining the label's text 3- It
 * is capable of wrapping the label's text at a given width with a given
 * alignment 4- It is capable of supporting multiple label icons (temporary
 * feature)
 * 
 * This class was originally deriving off Draw2d's <code>Label</code> class
 * but with the introduction of the auto-wrapping feature, a copy had to be made
 * overriding was not straightforward. Hopefully, this extended version can be
 * pushed to opensource
 * 
 * <p>
 * Code taken from Eclipse reference bugzilla #98820
 * 
 * @author melaasar
 */
public class WrapLabel
	extends Figure
	implements PositionConstants {

	private static String _ellipse = "..."; //$NON-NLS-1$
	private static final Dimension EMPTY_DIMENSION = new Dimension(0, 0);

	// reserve 1 bit
	private static int FLAG_SELECTED = MAX_FLAG << 1;
	private static int FLAG_HASFOCUS = MAX_FLAG << 2;
	private static int FLAG_UNDERLINED = MAX_FLAG << 3;
	private static int FLAG_STRIKEDTHROUGH = MAX_FLAG << 4;
	private static int FLAG_WRAP = MAX_FLAG << 5;
	
	// reserve 3 bits 
	private static int FLAG_TEXT_ALIGN = MAX_FLAG << 6;
	private static int FLAG_WRAP_ALIGN = MAX_FLAG << 9;
	private static int FLAG_ICON_ALIGN = MAX_FLAG << 12;
	private static int FLAG_LABEL_ALIGN = MAX_FLAG << 15;
	private static int FLAG_TEXT_PLACEMENT = MAX_FLAG << 18;
	
	
	/** the original label's text */
	private String text = "";//$NON-NLS-1$

	/** the label's text used in painting after applying required styles */
	private String subStringText;

	/** the size of text */
	private Dimension textSize;

	/** the location of text */
	private Point textLocation;

	/** the cached hint used to calculate text size */
	private Dimension cachedPrefSizeHint = new Dimension(-1, -1);

	/** the cached hint used to calculate text size */
	private Dimension cachedTextSizeHint = new Dimension(-1, -1);


	/** the icon location */
	private Point iconLocation;

	private class IconInfo {
		/** the label icons */
		private ArrayList icons = new ArrayList();
		/** total icon size */
		private Dimension totalIconSize;

		/**
		 * Gets the icon at the index location.
		 * 
		 * @param i the index to retrieve the icon of
		 * @return <code>Image</code> that corresponds to the given index.
		 */
		public Image getIcon(int i) {
			if (i >= icons.size())
				return null;
			
			return (Image) icons.get(i);
		}
		
		/**
		 * Sets the icon at the index location.
		 * 
		 * @param icon
		 * @param i
		 */
		public void setIcon(Image icon, int i) {
			if (i >= icons.size())
			{
				for (int j=icons.size(); j<i; j++)
					icons.add(null);
				icons.add(icon);
				icons.trimToSize();
			}
			else
				icons.set(i, icon);
		}
		
		/**
		 * Gets the icon size of the icon at the given index.
		 * @param i
		 * @return the <code>Dimension</code> that is the size of the icon at the given index.
		 */
		public Dimension getIconSize(int i) {
			Image img = getIcon(i);
			if (img != null && !img.isDisposed()) {
				org.eclipse.swt.graphics.Rectangle imgBounds = img.getBounds();
				return new Dimension(MapModeUtil.getMapMode(WrapLabel.this).DPtoLP(imgBounds.width), 
									MapModeUtil.getMapMode(WrapLabel.this).DPtoLP(imgBounds.height));
			}
			return EMPTY_DIMENSION;
		}
		
		/**
		 * @return the number of icons
		 */
		public int getNumberofIcons() {
			return icons.size();
		}

		/**
		 * @return the <code>Dimension</code> that is the total size of all the icons.
		 */
		public Dimension getTotalIconSize() {
			if (totalIconSize != null)
				return totalIconSize;
			
			totalIconSize = new Dimension(0, 0);
			
			for (int i = 0; i < getNumberofIcons(); i++) {
				Dimension iconSize = getIconSize(i);
				if (iconSize != null) {
					totalIconSize.width += iconSize.width;
					if (iconSize.height > totalIconSize.height)
						totalIconSize.height = iconSize.height;
				}
			}
			
			return totalIconSize;
		}

		/**
		 * 
		 */
		public void invalidate() {
			totalIconSize = null;
		}
	}
	
	private IconInfo iconInfo = null;
	
	/**
	 * Construct an empty Label.
	 * 
	 * @since 2.0
	 */
	public WrapLabel() {
		// set defaults
		setTextAlignment(CENTER);
		setIconAlignment(CENTER);
		setLabelAlignment(CENTER);
		setTextWrapAlignment(LEFT);
		setTextPlacement(EAST);
	}

	/**
	 * Construct a Label with passed String as its text.
	 * 
	 * @param s the label text
	 * @since 2.0
	 */
	public WrapLabel(String s) {
		setText(s);
//		setBorder(new LineBorderEx(ColorConstants.red,3));
	}

	/**
	 * Construct a Label with passed Image as its icon.
	 * 
	 * @param i the label image
	 * @since 2.0
	 */
	public WrapLabel(Image i) {
		setIcon(i);
	}

	/**
	 * Construct a Label with passed String as text and passed Image as its
	 * icon.
	 * 
	 * @param s the label text
	 * @param i the label image
	 * @since 2.0
	 */
	public WrapLabel(String s, Image i) {
		setText(s);
		setIcon(i);
	}

	private void alignOnHeight(Point loc, Dimension size, int alignment) {
		Insets insets = getInsets();
		switch (alignment) {
			case TOP:
				loc.y = insets.top;
				break;
			case BOTTOM:
				loc.y = bounds.height - size.height - insets.bottom;
				break;
			default:
				loc.y = (bounds.height - size.height) / 2;
		}
	}

	private void alignOnWidth(Point loc, Dimension size, int alignment) {
		Insets insets = getInsets();
		switch (alignment) {
			case LEFT:
				loc.x = insets.left;
				break;
			case RIGHT:
				loc.x = bounds.width - size.width - insets.right;
				break;
			default:
				loc.x = (bounds.width - size.width) / 2;
		}
	}

	private void calculateAlignment() {
		Dimension iconSize = getTotalIconSize();
		switch (getTextPlacement()) {
			case EAST:
			case WEST:
				alignOnHeight(textLocation, getTextSize(), getTextAlignment());
				alignOnHeight(getIconLocation(), iconSize, getIconAlignment());
				break;
			case NORTH:
			case SOUTH:
				alignOnWidth(textLocation, getSubStringTextSize(),
					getTextAlignment());
				alignOnWidth(getIconLocation(), iconSize, getIconAlignment());
				break;
		}
	}

	/**
	 * Calculates the size of the Label using the passed Dimension as the size
	 * of the Label's text.
	 * 
	 * @param txtSize the precalculated size of the label's text
	 * @return the label's size
	 * @since 2.0
	 */
	protected Dimension calculateLabelSize(Dimension txtSize) {
		int gap = getIconTextGap();
		if (!hasIcons() || getText().equals("")) //$NON-NLS-1$
			gap = 0;
		Dimension d = new Dimension(0, 0);
		Dimension iconSize = getTotalIconSize();
		if (getTextPlacement() == WEST || getTextPlacement() == EAST) {
			d.width = iconSize.width + gap + txtSize.width;
			d.height = Math.max(iconSize.height, txtSize.height);
		} else {
			d.width = Math.max(iconSize.width, txtSize.width);
			d.height = iconSize.height + gap + txtSize.height;
		}
		return d;
	}

	private void calculateLocations() {
		textLocation = new Point();
		iconLocation = new Point();

		calculatePlacement();
		calculateAlignment();
		Dimension offset = getSize().getDifference(
			getPreferredSize(getSize().width, getSize().height));
		offset.width += getTextSize().width - getSubStringTextSize().width;
		switch (getLabelAlignment()) {
			case CENTER:
				offset.scale(0.5f);
				break;
			case LEFT:
				offset.scale(0.0f);
				break;
			case RIGHT:
				offset.scale(1.0f);
				break;
			case TOP:
				offset.height = 0;
				offset.scale(0.5f);
				break;
			case BOTTOM:
				offset.height = offset.height * 2;
				offset.scale(0.5f);
				break;
			default:
				offset.scale(0.5f);
				break;
		}

		switch (getTextPlacement()) {
			case EAST:
			case WEST:
				offset.height = 0;
				break;
			case NORTH:
			case SOUTH:
				offset.width = 0;
				break;
		}

		textLocation.translate(offset);
		iconLocation.translate(offset);
	}

	private void calculatePlacement() {
		int gap = getIconTextGap();
		if (!hasIcons() || text.equals("")) //$NON-NLS-1$
			gap = 0;
		Insets insets = getInsets();
		Dimension iconSize = getTotalIconSize();
		
		switch (getTextPlacement()) {
			case EAST:
				iconLocation.x = insets.left;
				textLocation.x = iconSize.width + gap + insets.left;
				break;
			case WEST:
				textLocation.x = insets.left;
				iconLocation.x = getSubStringTextSize().width + gap
					+ insets.left;
				break;
			case NORTH:
				textLocation.y = insets.top;
				iconLocation.y = getTextSize().height + gap + insets.top;
				break;
			case SOUTH:
				textLocation.y = iconSize.height + gap + insets.top;
				iconLocation.y = insets.top;
		}
	}

	/**
	 * Calculates the size of the Label's text size. The text size calculated
	 * takes into consideration if the Label's text is currently truncated. If
	 * text size without considering current truncation is desired, use
	 * {@link #calculateTextSize(int, int)}.
	 * 
	 * @return the size of the label's text, taking into account truncation
	 * @since 2.0
	 */
	protected Dimension calculateSubStringTextSize() {
		return getTextExtents(getSubStringText(), getFont()); 
	}

	/**
	 * Calculates and returns the size of the Label's text. Note that this
	 * Dimension is calculated using the Label's full text, regardless of
	 * whether or not its text is currently truncated. If text size considering
	 * current truncation is desired, use {@link #calculateSubStringTextSize()}.
	 * 
	 * @param wHint a width hint
	 * @param hHint a height hint
	 * @return the size of the label's text, ignoring truncation
	 * @since 2.0
	 */
	protected Dimension calculateTextSize(int wHint, int hHint) {
		return getTextExtents(getWrappedText(wHint, hHint), getFont());
	}

	private void clearLocations() {
		iconLocation = textLocation = null;
	}

	/**
	 * Returns the Label's icon.
	 * 
	 * @return the label icon
	 * @since 2.0
	 */
	public Image getIcon() {
		return getIcon(0);
	}

	/**
	 * Gets the label's icon at the given index
	 * 
	 * @param index The icon index
	 * @return the <code>Image</code> that is the icon for the given index.
	 */
	public Image getIcon(int index) {
		if (iconInfo == null)
			return null;
		return iconInfo.getIcon(index);
	}

	/**
	 * Determines if there is any icons by checking if icon size is zeros.
	 * 
	 * @return true if icons are present, false otherwise 
	 */
	protected boolean hasIcons() {
		return !Dimension.SINGLETON.equals(getTotalIconSize());
	}

	/**
	 * Returns the current alignment of the Label's icon. The default is
	 * {@link PositionConstants#CENTER}.
	 * 
	 * @return the icon alignment
	 * @since 2.0
	 */
	public int getIconAlignment() {
		return getAlignment(FLAG_ICON_ALIGN);
	}

	/**
	 * Returns the bounds of the Label's icon.
	 * 
	 * @return the icon's bounds
	 * @since 2.0
	 */
	public Rectangle getIconBounds() {
		return new Rectangle(getBounds().getLocation().translate(
			getIconLocation()), getTotalIconSize());
	}

	/**
	 * Returns the location of the Label's icon relative to the Label.
	 * 
	 * @return the icon's location
	 * @since 2.0
	 */
	protected Point getIconLocation() {
		if (iconLocation == null)
			calculateLocations();
		return iconLocation;
	}

	/**
	 * Returns the gap in pixels between the Label's icon and its text.
	 * 
	 * @return the gap
	 * @since 2.0
	 */
	public int getIconTextGap() {
		return MapModeUtil.getMapMode(this).DPtoLP(3);
	}

	/**
	 * @see IFigure#getMinimumSize(int, int)
	 */
	public Dimension getMinimumSize(int w, int h) {
		if (minSize != null)
			return minSize;
		minSize = new Dimension();
		if (getLayoutManager() != null)
			minSize.setSize(getLayoutManager().getMinimumSize(this, w, h));

		Dimension d = getTextExtents(getEllipse(), getFont()).intersect(
			getTextExtents(getText(), getFont()));
		Dimension labelSize = calculateLabelSize(d);
		Insets insets = getInsets();
		labelSize.expand(insets.getWidth(), insets.getHeight());
		minSize.union(labelSize);
		return minSize;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.draw2d.IFigure#getPreferredSize(int, int)
	 */
	public Dimension getPreferredSize(int wHint, int hHint) {
		if (prefSize == null || wHint != cachedPrefSizeHint.width || hHint != cachedPrefSizeHint.height) {
			prefSize = calculateLabelSize(getTextSize(wHint, hHint));
			Insets insets = getInsets();
			prefSize.expand(insets.getWidth(), insets.getHeight());
			if (getLayoutManager() != null)
				prefSize.union(getLayoutManager().getPreferredSize(this, wHint,
					hHint));
			prefSize.union(getMinimumSize(wHint, hHint));
			cachedPrefSizeHint.width = wHint;
			cachedPrefSizeHint.height= hHint;
		}
		return prefSize;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.IFigure#getMaximumSize()
	 */
	public Dimension getMaximumSize() {
		// this assumes that getPreferredSize(wHint, hHint) is called before
		return prefSize;   
	}

	/**
	 * Calculates the amount of the Label's current text will fit in the Label,
	 * including an elipsis "..." if truncation is required.
	 * 
	 * @return the substring
	 * @since 2.0
	 */
	public String getSubStringText() {
		if (subStringText != null)
			return subStringText;

		Dimension shrink = getPreferredSize(getSize().width, getSize().height).getDifference(getSize());
		Dimension effectiveSize = getTextSize().getExpanded(-shrink.width, -shrink.height);
		
		Font f = getFont();
		int fontHeight = MapModeUtil.getMapMode(this).DPtoLP(FigureUtilities.getFontMetrics(f).getHeight());
		int maxLines = (int) (effectiveSize.height / (double) fontHeight);

		StringBuffer accumlatedText = new StringBuffer();
		StringBuffer remainingText = new StringBuffer(getText());
		int i = 0, j = 0;

		while (remainingText.length() > 0 && j++ < maxLines) {
			i = getLineWrapPosition(remainingText.toString(), f, effectiveSize.width);

			if (accumlatedText.length() > 0)
				accumlatedText.append("\n"); //$NON-NLS-1$

			if (i == 0 || (remainingText.length() > i && j == maxLines)) {
				int dotsWidth = getTextExtents(getEllipse(), f).width;
				i = getLargestSubstringConfinedTo(remainingText.toString(), f, Math.max(effectiveSize.width - dotsWidth, 0));
				accumlatedText.append(remainingText.substring(0, i));
				accumlatedText.append(getEllipse());
			} else
				accumlatedText.append(remainingText.substring(0, i));
			remainingText.delete(0, i);
		}
		return subStringText = accumlatedText.toString();
	}

	/**
	 * Creates an equivalent text to that of the label's but with "\n"(s)
	 * inserted at the wrapping positions. This method assumes unlimited
	 * bounding box and is used by <code>calculateTextSize()</code> to
	 * calculate the perfect size of the text with wrapping
	 * 
	 * @return the wrapped text
	 */
	private String getWrappedText(int wHint, int hHint) {
		if (!isTextWrapped() || wHint == -1)
			return getText();

		Dimension iconSize = getTotalIconSize();
		if (hasIcons()) {
			switch(getTextPlacement()) {
				case EAST:
				case WEST:
					wHint -= iconSize.width + getIconTextGap();
					break;
				case NORTH:
				case SOUTH:
					if (hHint != -1)
						hHint -= iconSize.height + getIconTextGap();
					break;
			}
		}
		
		Font f = getFont();
		int maxLines = Integer.MAX_VALUE;
		if (hHint != -1) {
			int fontHeight = MapModeUtil.getMapMode(this).DPtoLP(FigureUtilities.getFontMetrics(f).getHeight());
			maxLines = (int) (hHint / (double) fontHeight);
		}

		StringBuffer accumlatedText = new StringBuffer();
		StringBuffer remainingText = new StringBuffer(getText());
		int i = 0, j = 0;

		while (remainingText.length() > 0 && j++  < maxLines) {
			if ((i = getLineWrapPosition(remainingText.toString(), f, wHint)) == 0)
				break;

			if (accumlatedText.length() > 0)
				accumlatedText.append("\n"); //$NON-NLS-1$
			accumlatedText.append(remainingText.substring(0, i));
			remainingText.delete(0, i);
		}
		return accumlatedText.toString();
	}

	/**
	 * Returns the size of the Label's current text. If the text is currently
	 * truncated, the truncated text with its ellipsis is used to calculate the
	 * size.
	 * 
	 * @return the size of this label's text, taking into account truncation
	 * @since 2.0
	 */
	protected Dimension getSubStringTextSize() {
		return calculateSubStringTextSize();
	}

	/**
	 * Returns the text of the label. Note that this is the complete text of the
	 * label, regardless of whether it is currently being truncated. Call
	 * {@link #getSubStringText()}to return the label's current text contents
	 * with truncation considered.
	 * 
	 * @return the complete text of this label
	 * @since 2.0
	 */
	public String getText() {
		return text;
	}

	/**
	 * Returns the current alignment of the Label's text. The default text
	 * alignment is {@link PositionConstants#CENTER}.
	 * 
	 * @return the text alignment
	 */
	public int getTextAlignment() {
		return getAlignment(FLAG_TEXT_ALIGN);
	}

	/**
	 * Returns the current alignment of the entire Label. The default label
	 * alignment is {@link PositionConstants#LEFT}.
	 * 
	 * @return the label alignment
	 */
	private int getLabelAlignment() {
		return getAlignment(FLAG_LABEL_ALIGN);
	}
	
	/**
	 * Returns the bounds of the label's text. Note that the bounds are
	 * calculated using the label's complete text regardless of whether the
	 * label's text is currently truncated.
	 * 
	 * @return the bounds of this label's complete text
	 * @since 2.0
	 */
	public Rectangle getTextBounds() {
		return new Rectangle(getBounds().getLocation().translate(
			getTextLocation()), getTextSize());
	}

	/**
	 * Returns the location of the label's text relative to the label.
	 * 
	 * @return the text location
	 * @since 2.0
	 */
	protected Point getTextLocation() {
		if (textLocation != null)
			return textLocation;
		calculateLocations();
		return textLocation;
	}

	/**
	 * Returns the current placement of the label's text relative to its icon.
	 * The default text placement is {@link PositionConstants#EAST}.
	 * 
	 * @return the text placement
	 * @since 2.0
	 */
	public int getTextPlacement() {
		return getPlacement(FLAG_TEXT_PLACEMENT);
	}

	/**
	 * Returns the size of the label's complete text. Note that the text used to
	 * make this calculation is the label's full text, regardless of whether the
	 * label's text is currently being truncated and is displaying an ellipsis.
	 * If the size considering current truncation is desired, call
	 * {@link #getSubStringTextSize()}.
	 * 
	 * @param wHint a width hint
	 * @param hHint a height hint
	 * @return the size of this label's complete text
	 * @since 2.0
	 */
	protected Dimension getTextSize(int wHint, int hHint) {
		if (textSize == null || wHint != cachedTextSizeHint.width || hHint != cachedTextSizeHint.height) {
			textSize = calculateTextSize(wHint, hHint);
			cachedTextSizeHint.width = wHint;
			cachedTextSizeHint.height= hHint;
		}
		return textSize;
	}

	/**
	 * Gets the text size given the current size as a width hint
	 */
	private final Dimension getTextSize() {
		return getTextSize(getSize().width, getSize().height);
	}
	
	/**
	 * @see IFigure#invalidate()
	 */
	public void invalidate() {
		prefSize = null;
		minSize = null;
		clearLocations();
		textSize = null;
		subStringText = null;
		if (iconInfo != null)
			iconInfo.invalidate();
		super.invalidate();
	}

	/**
	 * Returns <code>true</code> if the label's text is currently truncated
	 * and is displaying an ellipsis, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if the label's text is truncated
	 * @since 2.0
	 */
	public boolean isTextTruncated() {
		return !getSubStringTextSize().equals(getTextSize());
	}

	/**
	 * @see org.eclipse.draw2d.Figure#paintFigure(org.eclipse.draw2d.Graphics)
	 */
	public void paintFigure(Graphics graphics) {
		if (isSelected()) {
			graphics.pushState();
			graphics.setBackgroundColor(ColorConstants.menuBackgroundSelected);
			graphics.fillRectangle(getSelectionRectangle());
			graphics.popState();
			graphics.setForegroundColor(ColorConstants.white);
		}
		if (hasFocus()) {
			graphics.pushState();
			graphics.setXORMode(true);
			graphics.setForegroundColor(ColorConstants.menuBackgroundSelected);
			graphics.setBackgroundColor(ColorConstants.white);
			graphics.drawFocus(getSelectionRectangle().resize(-1, -1));
			graphics.popState();
		}
		if (isOpaque())
			super.paintFigure(graphics);
		Rectangle figBounds = getBounds();

		graphics.translate(figBounds.x, figBounds.y);
		if (hasIcons())
			paintIcons(graphics);

		if (!isEnabled()) {
			graphics.translate(1, 1);
			graphics.setForegroundColor(ColorConstants.buttonLightest);
			paintText(graphics);
			graphics.translate(-1, -1);
			graphics.setForegroundColor(ColorConstants.buttonDarker);
		}
		paintText(graphics);
		graphics.translate(-figBounds.x, -figBounds.y);
	}

	/**
	 * Paints the text and optioanally underlines it
	 * 
	 * @param graphics The graphics context
	 */
	private void paintText(Graphics graphics) {
		String subString = getSubStringText();
		StringTokenizer tokenizer = new StringTokenizer(subString, "\n"); //$NON-NLS-1$

		Font f = getFont();
		int fontHeight = MapModeUtil.getMapMode(this).DPtoLP(FigureUtilities.getFontMetrics(f)
			.getHeight());
		int textWidth = getTextExtents(subString, f).width;
		int y = getTextLocation().y;
		
		// If the font's leading area is 0 then we need to add an offset to
		// avoid truncating at the top (e.g. Korean fonts)
		if (0 == FigureUtilities.getFontMetrics(f).getLeading()) {
			int offset = MapModeUtil.getMapMode(this).DPtoLP(2); // 2 is the leading area for default English
			y += offset;
		}				

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			int tokenWidth = getTextExtents(token, f).width;
			int x = getTextLocation().x;
			switch (getTextWrapAlignment()) {
				case CENTER:
					x += (textWidth - tokenWidth) / 2;
					break;
				case RIGHT:
					x += textWidth - tokenWidth;
					break;
			}
			
			// increase the clipping rectangle by a small amount to account for font overhang
			// from italic / irregular characters etc.
			Rectangle clipRect = new Rectangle();
			graphics.getClip(clipRect);
			
			if (tokenWidth + x <= clipRect.getTopRight().x) {
				Rectangle newClipRect = new Rectangle(clipRect);
				newClipRect.width += (tokenWidth / token.length()) / 2;
				graphics.setClip(newClipRect);
			}
				
			graphics.drawText(token, x, y);
			graphics.setClip(clipRect);
			
			y += fontHeight;

			if (isTextUnderlined())
				graphics.drawLine(x, y - 1, x + tokenWidth, y - 1);
			if (isTextStrikedThrough())
				graphics.drawLine(x, y - fontHeight / 2 + 1, x + tokenWidth, y
					- fontHeight / 2 + 1);
		}
	}

	/**
	 * Paints the icon(s)
	 * 
	 * @param graphics The graphics context
	 */
	private void paintIcons(Graphics graphics) {
		Point p = Point.SINGLETON;
		p.setLocation(getIconLocation());

		int num = getNumberofIcons();
		for (int i = 0; i < num; i++) {
			Image icon = getIcon(i); 
			if (icon != null) {
				graphics.drawImage(icon, p);
				p.x += getIconSize(i).width;
			}
		}
	}

	/**
	 * Sets the label's icon to the passed image.
	 * 
	 * @param image the new label image
	 * @since 2.0
	 */
	public void setIcon(Image image) {
		setIcon(image, 0);
	}

	/**
	 * Sets the label's icon at given index
	 * 
	 * @param image The icon image or null to remove the icon
	 * @param index The icon index
	 */
	public void setIcon(Image image, int index) {
		if (iconInfo == null)
			iconInfo = new IconInfo();
			
		if (iconInfo.getIcon(index) == image)
			return;
		
		iconInfo.setIcon(image, index);
		revalidate();
		repaint();// Call repaint, in case the image dimensions are the same.
	}


	/**
	 * Sets the icon alignment relative to the .abel's alignment to the passed
	 * value. The default is {@link PositionConstants#CENTER}. Other possible
	 * values are {@link PositionConstants#TOP},
	 * {@link PositionConstants#BOTTOM},{@link PositionConstants#LEFT}and
	 * {@link PositionConstants#RIGHT}.
	 * 
	 * @param align the icon alignment
	 * @since 2.0
	 */
	public void setIconAlignment(int align) {
		if (getIconAlignment() == align)
			return;
		setAlignmentFlags(align, FLAG_ICON_ALIGN);
		clearLocations();
		repaint();
	}

	/**
	 * getIconSize
	 * @param index of icon to retrieve size of.
	 * @return Dimension representing the icon size.
	 */
	protected Dimension getIconSize(int index) {
		if (iconInfo == null)
			return EMPTY_DIMENSION;
		return iconInfo.getIconSize(index);
	}
	
	/**
	 * getIconNumber
	 * @return int number of icons in the wrap label
	 */
	protected int getNumberofIcons() {
		if (iconInfo == null)
			return 0;
		return iconInfo.getNumberofIcons();
	}
	
	/**
	 * getTotalIconSize
	 * Calculates the total union of icon sizes
	 * @return Dimension that is the union of icon sizes
	 */
	protected Dimension getTotalIconSize() {
		if (iconInfo == null)
			return EMPTY_DIMENSION;
		return iconInfo.getTotalIconSize();
	}

	/**
	 * Sets the Label's alignment to the passed value. The default is
	 * {@link PositionConstants#CENTER}. Other possible values are
	 * {@link PositionConstants#TOP},{@link PositionConstants#BOTTOM},
	 * {@link PositionConstants#LEFT}and {@link PositionConstants#RIGHT}.
	 * 
	 * @param align label alignment
	 */
	public void setLabelAlignment(int align) {
		if (getLabelAlignment() == align)
			return;
		setAlignmentFlags(align, FLAG_LABEL_ALIGN);
		clearLocations();
		repaint();
	}

	/**
	 * Return the ellipse string.
	 * 
	 * @return the <code>String</code> that represents the fact that the
	 * text has been truncated and that more text is available but hidden. 
	 * Usually this is represented by "...".
	 */
	protected String getEllipse() {
		return _ellipse;
	}
	
	/**
	 * Sets the label's text.
	 * 
	 * @param s the new label text
	 * @since 2.0
	 */
	public void setText(String s) {
		//"text" will never be null.
		if (s == null)
			s = "";//$NON-NLS-1$
		if (text.equals(s))
			return;
		text = s;
		revalidate();
		repaint(); //If the new text does not cause a new size, we still need
		// to paint.
	}

	/**
	 * Sets the text alignment of the Label relative to the label alignment. The
	 * default is {@link PositionConstants#CENTER}. Other possible values are
	 * {@link PositionConstants#TOP},{@link PositionConstants#BOTTOM},
	 * {@link PositionConstants#LEFT}and {@link PositionConstants#RIGHT}.
	 * 
	 * @param align the text alignment
	 * @since 2.0
	 */
	public void setTextAlignment(int align) {
		if (getTextAlignment() == align)
			return;
		setAlignmentFlags(align, FLAG_TEXT_ALIGN);
		clearLocations();
		repaint();
	}

	/**
	 * Sets the text placement of the label relative to its icon. The default is
	 * {@link PositionConstants#EAST}. Other possible values are
	 * {@link PositionConstants#NORTH},{@link PositionConstants#SOUTH}and
	 * {@link PositionConstants#WEST}.
	 * 
	 * @param where the text placement
	 * @since 2.0
	 */
	public void setTextPlacement(int where) {
		if (getTextPlacement() == where)
			return;
		setPlacementFlags(where, FLAG_TEXT_PLACEMENT);
		revalidate();
		repaint();
	}

	/**
	 * Sets whether the label text should be underlined
	 * 
	 * @param b Wether the label text should be underlined
	 */
	public void setTextUnderline(boolean b) {
		if (isTextUnderlined() == b)
			return;
		setFlag(FLAG_UNDERLINED, b);
		repaint();
	}

	/**
	 * @return whether the label text is underlined
	 */
	public boolean isTextUnderlined() {
		return (flags & FLAG_UNDERLINED) != 0;
	}
	
	/**
	 * Sets whether the label text should be striked-through
	 * 
	 * @param b Wether the label text should be stricked-through
	 */
	public void setTextStrikeThrough(boolean b) {
		if (isTextStrikedThrough() == b)
			return;
		setFlag(FLAG_STRIKEDTHROUGH, b);
		repaint();
	}

	/**
	 * @return wether the label text is stricked-through
	 */
	public boolean isTextStrikedThrough() {
		return (flags & FLAG_STRIKEDTHROUGH) != 0;
	}

	/**
	 * Sets whether the label text should wrap
	 * 
	 * @param b whether the label text should wrap
	 */
	public void setTextWrap(boolean b) {
		if (isTextWrapped() == b)
			return;
		setFlag(FLAG_WRAP, b);
		revalidate();
		repaint();
	}

	/**
	 * @return wether the label text wrap is on
	 */
	public boolean isTextWrapped() {
		return (flags & FLAG_WRAP) != 0;
	}

	/**
	 * Sets the wrapping width of the label text. This is only valid if text
	 * wrapping is turned on
	 * 
	 * @param i The label text wrapping width
	 */
	public void setTextWrapWidth(int i) {
		/*
		 * if (this.wrapWidth == i) return; this.wrapWidth = i; revalidate();
		 * repaint();
		 */}

	/**
	 * Sets the wrapping width of the label text. This is only valid if text
	 * wrapping is turned on
	 * 
	 * @param i The label text wrapping width
	 */
	public void setTextWrapAlignment(int i) {
		if (getTextWrapAlignment() == i)
			return;
		
		setAlignmentFlags(i, FLAG_WRAP_ALIGN);
		repaint();
	}

	/**
	 * @return the label text wrapping width
	 */
	public int getTextWrapAlignment() {
		return getAlignment(FLAG_WRAP_ALIGN);
	}
	
	/**
	 * setPlacementFlags
	 * @param align 
	 * @param flagOffset
	 */
	private void setPlacementFlags(int align, int flagOffset) {
		flags &= ~(0x7 * flagOffset);
		switch (align) {
			case EAST:
				flags |= 0x1 * flagOffset;
				break;
			case WEST:
				flags |= 0x2 * flagOffset;
				break;
			case NORTH:
				flags |= 0x3 * flagOffset;
				break;
			case SOUTH:
				flags |= 0x4 * flagOffset;
				break;
		}
	}

	/**
	 * getPlacement
	 * 
	 * @param flagOffset
	 * @return PositionConstant representing the placement
	 */
	private int getPlacement(int flagOffset) {
		int wrapValue = flags & (0x7 * flagOffset);
		if (wrapValue == 0x1 * flagOffset)
			return EAST;
		else if (wrapValue == 0x2 * flagOffset)
			return WEST;
		else if (wrapValue == 0x3 * flagOffset)
			return NORTH;
		else if (wrapValue == 0x4 * flagOffset)
			return SOUTH;
		
		return EAST;
	}
	
	/**
	 * setAlignmentFlags
	 * @param align 
	 * @param flagOffset
	 */
	private void setAlignmentFlags(int align, int flagOffset) {
		flags &= ~(0x7 * flagOffset);
		switch (align) {
			case CENTER:
				flags |= 0x1 * flagOffset;
				break;
			case TOP:
				flags |= 0x2 * flagOffset;
				break;
			case LEFT:
				flags |= 0x3 * flagOffset;
				break;
			case RIGHT:
				flags |= 0x4 * flagOffset;
				break;
			case BOTTOM:
				flags |= 0x5 * flagOffset;
				break;
		}
	}

	/**
	 * Retrieves the alignment value from the flags member.
	 * 
	 * @param flagOffset that is the bitwise value representing the offset.
	 * @return PositionConstant representing the alignment
	 */
	private int getAlignment(int flagOffset) {
		int wrapValue = flags & (0x7 * flagOffset);
		if (wrapValue == 0x1 * flagOffset)
			return CENTER;
		else if (wrapValue == 0x2 * flagOffset)
			return TOP;
		else if (wrapValue == 0x3 * flagOffset)
			return LEFT;
		else if (wrapValue == 0x4 * flagOffset)
			return RIGHT;
		else if (wrapValue == 0x5 * flagOffset)
			return BOTTOM;
		
		return CENTER;
	}
	

	/**
	 * Sets the selection state of this label
	 * 
	 * @param b true will cause the label to appear selected
	 */
	public void setSelected(boolean b) {
		if (isSelected() == b)
			return;
		setFlag(FLAG_SELECTED, b);
		repaint();
	}

	/**
	 * @return the selection state of this label
	 */
	public boolean isSelected() {
		return (flags & FLAG_SELECTED) != 0;
	}

	/**
	 * Sets the focus state of this label
	 * 
	 * @param b true will cause a focus rectangle to be drawn around the text
	 *            of the Label
	 */
	public void setFocus(boolean b) {
		if (hasFocus() == b)
			return;
		setFlag(FLAG_HASFOCUS, b);
		repaint();
	}

	/**
	 * @return the focus state of this label
	 */
	public boolean hasFocus() {
		return (flags & FLAG_HASFOCUS) != 0;
	}

	/**
	 * Returns the bounds of the text selection
	 * 
	 * @return The bounds of the text selection
	 */
	private Rectangle getSelectionRectangle() {
		Rectangle figBounds = getTextBounds();
		figBounds
			.expand(new Insets(MapModeUtil.getMapMode(this).DPtoLP(2), 
							MapModeUtil.getMapMode(this).DPtoLP(2), 0, 0));
		translateToParent(figBounds);
		figBounds.intersect(getBounds());
		return figBounds;
	}

	/**
	 * returns the position of last character within the supplied text that will
	 * fit within the supplied width.
	 * 
	 * @param s a text string
	 * @param f font used to draw the text string
	 * @param w width in pixles.
	 */
	private int getLineWrapPosition(String s, Font f, int w) {
		// create an iterator for line breaking positions
		BreakIterator iter = BreakIterator.getLineInstance();
		iter.setText(s);
		int start = iter.first();
		int end = iter.next();

		// if the first line segment does not fit in the width,
		// determine the position within it where we need to cut
		if (getTextExtents(s.substring(start, end), f).width > w) {
			iter = BreakIterator.getCharacterInstance();
			iter.setText(s);
			start = iter.first();
		}

		// keep iterating as long as width permits
		do
			end = iter.next();
		while (end != BreakIterator.DONE
			&& getTextExtents(s.substring(start, end), f).width <= w);
		return (end == BreakIterator.DONE) ? iter.last()
			: iter.previous();
	}

	/**
	 * Returns the largest substring of <i>s </i> in Font <i>f </i> that can be
	 * confined to the number of pixels in <i>availableWidth <i>.
	 * 
	 * @param s the original string
	 * @param f the font
	 * @param w the available width
	 * @return the largest substring that fits in the given width
	 * @since 2.0
	 */
	private int getLargestSubstringConfinedTo(String s, Font f, int w) {
		int min, max;
		float avg = MapModeUtil.getMapMode(this).DPtoLP(FigureUtilities.getFontMetrics(f)
			.getAverageCharWidth());
		min = 0;
		max = s.length() + 1;

		//The size of the current guess
		int guess = 0, guessSize = 0;
		while ((max - min) > 1) {
			//Pick a new guess size
			//	New guess is the last guess plus the missing width in pixels
			//	divided by the average character size in pixels
			guess = guess + (int) ((w - guessSize) / avg);

			if (guess >= max)
				guess = max - 1;
			if (guess <= min)
				guess = min + 1;

			//Measure the current guess
			guessSize = getTextExtents(s.substring(0, guess), f).width;

			if (guessSize < w)
				//We did not use the available width
				min = guess;
			else
				//We exceeded the available width
				max = guess;
		}
		return min;
	}

	/**
	 * Gets the tex extent scaled to the mapping mode
	 */
	private Dimension getTextExtents(String s, Font f) {
		Dimension d = FigureUtilities.getTextExtents(s, f);
		return new Dimension(MapModeUtil.getMapMode(this).DPtoLP(d.width), 
							MapModeUtil.getMapMode(this).DPtoLP(d.height));
	}
	
}