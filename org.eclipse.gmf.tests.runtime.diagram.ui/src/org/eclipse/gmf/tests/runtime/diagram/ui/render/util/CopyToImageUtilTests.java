/******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package org.eclipse.gmf.tests.runtime.diagram.ui.render.util;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.gmf.runtime.diagram.core.preferences.PreferencesHint;
import org.eclipse.gmf.runtime.diagram.ui.OffscreenEditPartFactory;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.image.ImageFileFormat;
import org.eclipse.gmf.runtime.diagram.ui.render.util.CopyToImageUtil;
import org.eclipse.gmf.runtime.draw2d.ui.render.RenderedImage;
import org.eclipse.gmf.runtime.draw2d.ui.render.factory.RenderedImageFactory;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.tests.runtime.diagram.ui.AbstractTestBase;
import org.eclipse.gmf.tests.runtime.diagram.ui.logic.LogicTestFixture;
import org.eclipse.swt.widgets.Shell;

public class CopyToImageUtilTests
    extends AbstractTestBase {

    public CopyToImageUtilTests(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gmf.tests.runtime.diagram.ui.AbstractTestBase#setTestFixture()
     */
    protected void setTestFixture() {
        testFixture = new LogicTestFixture();
    }

    public static Test suite() {
        return new TestSuite(CopyToImageUtilTests.class);
    }

    public void testCopyToImageUtilTest_BMP()
        throws Exception {
        copyToImageTestForFormat(".bmp", ImageFileFormat.BMP);//$NON-NLS-1$ 
    }

    public void testCopyToImageUtilTest_GIF()
        throws Exception {
        copyToImageTestForFormat(".gif", ImageFileFormat.GIF);//$NON-NLS-1$ 
    }

    public void testCopyToImageUtilTest_JPEG()
        throws Exception {
        copyToImageTestForFormat(".jpeg", ImageFileFormat.JPEG);//$NON-NLS-1$ 
    }

    public void testCopyToImageUtilTest_JPG()
        throws Exception {
        copyToImageTestForFormat(".jpg", ImageFileFormat.JPG);//$NON-NLS-1$ 
    }

    public void testCopyToImageUtilTest_PNG()
        throws Exception {
        copyToImageTestForFormat(".png", ImageFileFormat.PNG);//$NON-NLS-1$ 
    }
    
    //public void testCopyToImageUtilTest_PDF() throws Exception {
	//	copyToImageTestForFormat(".pdf", ImageFileFormat.PDF);//$NON-NLS-1$ 
	//}

    public void testCopyToImageOffscreenUtilTest_BMP()
        throws Exception {
        copyToImageOffscreenTestForFormat(".bmp", ImageFileFormat.BMP);//$NON-NLS-1$ 
    }

    public void testCopyToImageOffscreenUtilTest_GIF()
        throws Exception {
        copyToImageOffscreenTestForFormat(".gif", ImageFileFormat.GIF);//$NON-NLS-1$ 
    }

    public void testCopyToImageOffscreenUtilTest_JPEG()
        throws Exception {
        copyToImageOffscreenTestForFormat(".jpeg", ImageFileFormat.JPEG);//$NON-NLS-1$ 
    }

    public void testCopyToImageOffscreenUtilTest_JPG()
        throws Exception {
        copyToImageOffscreenTestForFormat(".jpg", ImageFileFormat.JPG);//$NON-NLS-1$ 
    }

    public void testCopyToImageOffscreenUtilTest_PNG()
        throws Exception {
        copyToImageOffscreenTestForFormat(".png", ImageFileFormat.PNG);//$NON-NLS-1$ 
    }
    
    //public void testCopyToImageOffscreenUtilTest_PDF() throws Exception {
	//	copyToImageOffscreenTestForFormat(".pdf", ImageFileFormat.PDF);//$NON-NLS-1$ 
	//}

    // public void testCopyToImageUtilTest_SVG() throws Exception {
    // copyToImageTestForFormat(".svg", ImageFileFormat.SVG);//$NON-NLS-1$
    // }

    private void copyToImageTestForFormat(String suffix, ImageFileFormat type)
        throws IOException, CoreException {
        IProgressMonitor monitor = new NullProgressMonitor();
        File file = File.createTempFile("test", suffix);//$NON-NLS-1$ 
        IPath tmpDest = new Path(file.getPath());

        // export to each file type
        new CopyToImageUtil().copyToImage(getDiagramEditPart(), tmpDest, type,
            monitor);

        RenderedImage ri = RenderedImageFactory.getInstance(tmpDest.toString());
        file.delete();
        assertTrue("RenderedImage is null", ri != null);//$NON-NLS-1$ 
        assertTrue("SWTImage is null", ri.getSWTImage() != null);//$NON-NLS-1$
        /*
         * Entries in the RenederdImageFactory#instanceMap may not get a chance to be garbage collected.
         * Hence, schedule a garbage collection.
         */
        System.gc();
    }

    private void copyToImageOffscreenTestForFormat(String suffix,
			ImageFileFormat type) throws IOException, CoreException {
		IProgressMonitor monitor = new NullProgressMonitor();
		File file = File.createTempFile("test", suffix);//$NON-NLS-1$ 
		IPath tmpDest = new Path(file.getPath());

		// export to each file type
		Diagram diagram = getDiagram();
		PreferencesHint hint = getDiagramEditPart().getDiagramPreferencesHint();
		Shell shell = new Shell();
		try {
			DiagramEditPart diagramEditPart = OffscreenEditPartFactory
					.getInstance().createDiagramEditPart(diagram, shell, hint);
			assertNotNull(
					"Offsreen editpart factory failed to generate diagram editpart. Diagram EditPart is null.", //$NON-NLS-1$
					diagramEditPart);
			new CopyToImageUtil().copyToImage(diagramEditPart, tmpDest, type,
					monitor);
		} finally {
			shell.dispose();
			RenderedImage ri = RenderedImageFactory.getInstance(tmpDest
					.toString());
			file.delete();
			assertTrue("RenderedImage is null", ri != null);//$NON-NLS-1$ 
			assertTrue("SWTImage is null", ri.getSWTImage() != null);//$NON-NLS-1$ 
	        /*
	         * Entries in the RenederdImageFactory#instanceMap may not get a chance to be garbage collected.
	         * Hence, schedule a garbage collection.
	         */
			System.gc();
		}
	}

}
