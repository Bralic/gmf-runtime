/******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package org.eclipse.gmf.tests.runtime.common.core.internal.command;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.commands.operations.UndoContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.gmf.runtime.common.core.command.AbstractCommand;
import org.eclipse.gmf.runtime.common.core.command.CommandResult;
import org.eclipse.gmf.runtime.common.core.command.ICommand;

/**
 * Tests the {@link AbstractCommand}.
 * 
 * @author ldamus
 */
public class AbstractCommandTest
    extends TestCase {

    private IOperationHistory history;

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(AbstractCommandTest.class);
    }

    public AbstractCommandTest(String name) {
        super(name);
    }

    protected void setUp()
        throws Exception {
        super.setUp();
        history = OperationHistoryFactory.getOperationHistory();
    }

    private List getFiles(String str) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] files = workspaceRoot.findFilesForLocationURI(URI.create(str));
        return Arrays.asList(files);
    }

    /**
     * Tests that commands can be executed, undone and redone through the
     * operation history.
     */
    public void test_execute_undo_redo() {
        String name = "test_execute_undo_redo"; //$NON-NLS-1$
        TestCommand c = new TestCommand(name, null);
        IUndoContext ctx = new ObjectUndoContext(this);

        try {
            c.addContext(ctx);
            history.execute(c, new NullProgressMonitor(), null);
        } catch (ExecutionException e) {
            e.printStackTrace();
            fail("Should not have thrown: " + e.getLocalizedMessage()); //$NON-NLS-1$
        }

        c.assertExecuted();

        try {
            assertTrue(history.canUndo(ctx));
            history.undo(ctx, new NullProgressMonitor(), null);
        } catch (ExecutionException e) {
            e.printStackTrace();
            fail("Should not have thrown: " + e.getLocalizedMessage()); //$NON-NLS-1$
        }

        c.assertUndone();

        try {
            assertTrue(history.canRedo(ctx));
            history.redo(ctx, new NullProgressMonitor(), null);
        } catch (ExecutionException e) {
            e.printStackTrace();
            fail("Should not have thrown: " + e.getLocalizedMessage()); //$NON-NLS-1$
        }

        c.assertRedone();
    }

    /**
     * Tests that composing two commands results in a command that combines the
     * contexts and affected files from both children.
     */
    public void test_compose() {
        String name = "test_compose"; //$NON-NLS-1$

        IUndoContext ctx1 = new UndoContext();
        IUndoContext ctx2 = new UndoContext();

        ICommand c1 = new TestCommand(name, getFiles("null:/compose1")); //$NON-NLS-1$
        c1.addContext(ctx1);

        ICommand c2 = new TestCommand(name, getFiles("null:/compose2")); //$NON-NLS-1$
        c2.addContext(ctx2);

        ICommand composition = c1.compose(c2);

        List affectedFiles = composition.getAffectedFiles();
        assertTrue(affectedFiles.containsAll(c1.getAffectedFiles()));
        assertTrue(affectedFiles.containsAll(c2.getAffectedFiles()));
        assertEquals(c1.getAffectedFiles().size()
            + c2.getAffectedFiles().size(), affectedFiles.size());

        List contexts = Arrays.asList(composition.getContexts());
        assertTrue(contexts.contains(ctx1));
        assertTrue(contexts.contains(ctx2));
    }

    /**
     * Tests that the reduction of an AbstractCommand returns itself.
     */
    public void test_reduce() {
        String name = "test_reduce"; //$NON-NLS-1$

        ICommand c = new TestCommand(name, null);
        ICommand reduction = c.reduce();

        assertSame(c, reduction);
    }

    /**
     * Tests that the affected files set in the command constructor are
     * available through the getAffectedFiles() method.
     */
    public void test_getAffectedFiles() {
        String fixtureName = "test_getAffectedFiles"; //$NON-NLS-1$

        // no affected files
        ICommand c = new TestCommand(fixtureName, null);
        assertTrue(c.getAffectedFiles().isEmpty());

        // an affected file
        List affectedFiles = getFiles("null:/AbstractCommandTest"); //$NON-NLS-1$

        c = new TestCommand(fixtureName, affectedFiles);

        assertEquals(affectedFiles.size(), c.getAffectedFiles().size());
        assertTrue(c.getAffectedFiles().containsAll(affectedFiles));
    }
    
    /**
	 * Verifies that no exceptions are thrown when a command is executed, undone
	 * or redone that has no command result.
	 */
    public void test_noCommandResult_146064() {
    	 String name = "test_noCommandResult_146064"; //$NON-NLS-1$
         TestCommand c = new TestCommandNoResults(name, null);
         IUndoContext ctx = new ObjectUndoContext(this);

         try {
             c.addContext(ctx);
             history.execute(c, new NullProgressMonitor(), null);
         } catch (ExecutionException e) {
             e.printStackTrace();
             fail("Should not have thrown: " + e.getLocalizedMessage()); //$NON-NLS-1$
         }

         c.assertExecuted();

         try {
             assertTrue(history.canUndo(ctx));
             history.undo(ctx, new NullProgressMonitor(), null);
         } catch (ExecutionException e) {
             e.printStackTrace();
             fail("Should not have thrown: " + e.getLocalizedMessage()); //$NON-NLS-1$
         }

         c.assertUndone();

         try {
             assertTrue(history.canRedo(ctx));
             history.redo(ctx, new NullProgressMonitor(), null);
         } catch (ExecutionException e) {
             e.printStackTrace();
             fail("Should not have thrown: " + e.getLocalizedMessage()); //$NON-NLS-1$
         }

         c.assertRedone();
    }

    // 
    // TEST FIXTURES
    //

    protected static class TestCommand
        extends AbstractCommand {

        private static final String EXECUTED = "executed"; //$NON-NLS-1$

        private static final String UNDONE = "undone"; //$NON-NLS-1$

        private static final String REDONE = "redone"; //$NON-NLS-1$

        protected boolean executed;

        protected boolean undone;

        protected boolean redone;

        public TestCommand(String label, List affectedFiles) {
            super(label, affectedFiles);
        }

        protected CommandResult doExecuteWithResult(
                IProgressMonitor progressMonitor, IAdaptable info)
            throws ExecutionException {
            executed = true;
            undone = false;
            redone = false;
            return CommandResult.newOKCommandResult(EXECUTED);
        }

        protected CommandResult doRedoWithResult(
                IProgressMonitor progressMonitor, IAdaptable info)
            throws ExecutionException {
            executed = false;
            undone = false;
            redone = true;
            return CommandResult.newOKCommandResult(REDONE);
        }

        protected CommandResult doUndoWithResult(
                IProgressMonitor progressMonitor, IAdaptable info)
            throws ExecutionException {
            executed = false;
            undone = true;
            redone = false;
            return CommandResult.newOKCommandResult(UNDONE);
        }

        public void assertExecuted() {
            assertTrue(executed);
            assertFalse(undone);
            assertFalse(redone);
            assertEquals(IStatus.OK, getCommandResult().getStatus()
                .getSeverity());
            assertSame(EXECUTED, getCommandResult().getReturnValue());
        }

        public void assertUndone() {
            assertTrue(undone);
            assertFalse(executed);
            assertFalse(redone);
            assertEquals(IStatus.OK, getCommandResult().getStatus()
                .getSeverity());
            assertSame(UNDONE, getCommandResult().getReturnValue());
        }

        public void assertRedone() {
            assertTrue(redone);
            assertFalse(undone);
            assertFalse(executed);
            assertEquals(IStatus.OK, getCommandResult().getStatus()
                .getSeverity());
            assertSame(REDONE, getCommandResult().getReturnValue());
        }
    }
    
    protected static class TestCommandNoResults extends TestCommand {

		public TestCommandNoResults(String label, List affectedFiles) {
			super(label, affectedFiles);
		}

		protected CommandResult doExecuteWithResult(
				IProgressMonitor progressMonitor, IAdaptable info)
				throws ExecutionException {

			super.doExecuteWithResult(progressMonitor, info);
			return null;
		}

		protected CommandResult doRedoWithResult(
				IProgressMonitor progressMonitor, IAdaptable info)
				throws ExecutionException {
			super.doRedoWithResult(progressMonitor, info);
			return null;
		}

		protected CommandResult doUndoWithResult(
				IProgressMonitor progressMonitor, IAdaptable info)
				throws ExecutionException {
			super.doUndoWithResult(progressMonitor, info);
			return null;
		}
		
        public void assertExecuted() {
            assertTrue(executed);
            assertFalse(undone);
            assertFalse(redone);
            assertNull(getCommandResult());
        }

        public void assertUndone() {
            assertTrue(undone);
            assertFalse(executed);
            assertFalse(redone);
            assertNull(getCommandResult());
        }

        public void assertRedone() {
            assertTrue(redone);
            assertFalse(undone);
            assertFalse(executed);
            assertNull(getCommandResult());
        }
	}

}