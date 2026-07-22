package delta.cion.tokyo.modKit;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.*;

import delta.cion.tokyo.modKit.init.CherrySetup;
import delta.cion.tokyo.modKit.util.Constants;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TokyoDevKit implements GeneratorNewProjectWizard {

	@Override
	public @NotNull String getId() {
		return Constants.PLUGIN_ID;
	}

	@Override
	public @NotNull String getName() {
		return Constants.PLUGIN_NAME;
	}

	@Override
	public @NotNull Icon getIcon() {
		return Constants.PLUGIN_ICON;
	}

	@Override
	public @NotNull String getDescription() {
		return Constants.PLUGIN_DESCRIPTION;
	}

	@Override
	public @NotNull NewProjectWizardStep createStep(@NotNull WizardContext wizardContext) {
		return new NewProjectWizardChainStep<>(new RootNewProjectWizardStep(wizardContext))
			.nextStep(NewProjectWizardBaseStep::new)
			.nextStep(GitNewProjectWizardStep::new)
			.nextStep(CherrySetup::new);
	}

	public static class Builder extends GeneratorNewProjectWizardBuilderAdapter {
		public Builder() {
			super(new TokyoDevKit());
		}
	}

}
