package org.nodeclipse.vertx.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.wst.jsdt.internal.ui.workingsets.JavaWorkingSetUpdater;
import org.nodeclipse.ui.util.LogUtil;
import org.nodeclipse.ui.wizards.NodeProjectWizardPage;
import org.nodeclipse.vertx.VertxConstants;

/**
 * NashornProjectWizard is copied  NodeProjectWizard with changed line marked with //+ (edited strings);
 * just like PhantomjsProjectWizard
 * @author Paul Verest
 */
@SuppressWarnings("restriction")
public class VertxProjectWizard extends AbstractVertxProjectWizard implements INewWizard {

	private final String WINDOW_TITLE = VertxConstants.WIZARD_WINDOW_TITLE; //+
	private NodeProjectWizardPage mainPage;

	private IProject newProject;

	public VertxProjectWizard() {
		setWindowTitle(WINDOW_TITLE);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		mainPage = new NodeProjectWizardPage("NodeNewProjectPage") { //$NON-NLS-1$
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.ui.dialogs.WizardNewProjectCreationPage#createControl
			 * (org.eclipse.swt.widgets.Composite)
			 */
			public void createControl(Composite parent) {
				super.createControl(parent);
				createWorkingSetGroup(
						(Composite) getControl(),
						getSelection(),
						new String[] { JavaWorkingSetUpdater.ID, "org.eclipse.ui.resourceWorkingSetPage" }); //$NON-NLS-1$
				Dialog.applyDialogFont(getControl());
			}
		};
		mainPage.setTitle(VertxConstants.WIZARD_PAGE_TITLE); //+
		mainPage.setDescription(VertxConstants.WIZARD_PAGE_DESCRIPTION); //+
		addPage(mainPage);
	}

	@Override
	protected IProject createNewProject() {
		if (newProject != null) {
			return null;
		}
		final IProject newProjectHandle = mainPage.getProjectHandle();
		URI location = null;
		if (!mainPage.useDefaults()) {
			location = mainPage.getLocationURI();
		}
/*
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProjectDescription description = workspace
				.newProjectDescription(newProjectHandle.getName());
		description.setLocationURI(location);
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = NodeNature.NATURE_ID;
		description.setNatureIds(newNatures);
*/
		final IProjectDescription description = createProjectDescription(newProjectHandle, location);
		final boolean exists = isExistsProjectFolder(description);

		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				CreateProjectOperation op = new CreateProjectOperation(
						description, WINDOW_TITLE);
				try {
					op.execute(monitor,
							WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
				} catch (ExecutionException e) {
					throw new InvocationTargetException(e);
				}
				
				try {
					if(!exists) {
						// copy README.md, package.json & hello-world-server.js
						generateTemplates("templates/common-templates", newProjectHandle);
						//generateTemplates("templates", newProjectHandle); //+ Node specific
						rewriteFile("README.md", newProjectHandle);
						//rewriteFile("package.json", newProjectHandle); //+ Node specific
					}
					// JSHint support
					runJSHint(newProjectHandle);
				} catch (CoreException e) {
					LogUtil.error(e);
				}
			}
		};

		try {
			getContainer().run(true, true, op);
		} catch (InvocationTargetException e) {
			LogUtil.error(e);
		} catch (InterruptedException e) {
		}

		if (newProjectHandle != null) {
			// add to workingsets
			IWorkingSet[] workingSets = mainPage.getSelectedWorkingSets();
			getWorkbench().getWorkingSetManager().addToWorkingSets(
					newProjectHandle, workingSets);
		}

		newProject = newProjectHandle;
		return newProject;
	}
}
