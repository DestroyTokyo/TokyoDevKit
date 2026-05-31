package delta.cion.cherry.modkit;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.*;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CherryModKit implements GeneratorNewProjectWizard {

	private static final String PLUGIN_ID = "cherry_mod_kit";
	private static final String PLUGIN_NAME = "CherryModKit";
	private static final String PLUGIN_DESCRIPTION = "Simple IntellijIDEA extension for develop CherryServer plugins";

	private static final Icon PLUGIN_ICON = IconLoader.getIcon("/icons/minestom-logo-small.png", CherryModKit.class);

	@Override
	public @NotNull String getId() {
		return PLUGIN_ID;
	}

	@Override
	public @NotNull String getName() {
		return PLUGIN_NAME;
	}

	@Override
	public @NotNull Icon getIcon() {
		return PLUGIN_ICON;
	}

	@Override
	public @NotNull String getDescription() {
		return PLUGIN_DESCRIPTION;
	}

	@Override
	public @NotNull NewProjectWizardStep createStep(@NotNull WizardContext wizardContext) {
		RootNewProjectWizardStep rootStep =
			new RootNewProjectWizardStep(wizardContext);
		return new NewProjectWizardChainStep<>(rootStep)
			.nextStep(NewProjectWizardBaseStep::new)
			.nextStep(GitNewProjectWizardStep::new)
			.nextStep(MinestomSetupStep::new);
	}

	public static class Builder extends GeneratorNewProjectWizardBuilderAdapter {
		public Builder() {
			super(new CherryModKit());
		}
	}

	private static

}
