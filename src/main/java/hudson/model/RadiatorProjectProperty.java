package hudson.model;

import hudson.Extension;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public final class RadiatorProjectProperty extends
		JobProperty<AbstractProject<?, ?>> {

	public final String projectName;

	@DataBoundConstructor
	public RadiatorProjectProperty(final String projectName) {

		this.projectName = projectName;
	}

	@Extension
	public static final class DescriptorImpl extends JobPropertyDescriptor {

		public DescriptorImpl() {
			super(RadiatorProjectProperty.class);
			load();
		}

		@Override
		public boolean isApplicable(Class<? extends Job> jobType) {
			return AbstractProject.class.isAssignableFrom(jobType);
		}

		@Override
		public String getDisplayName() {
			return "Radiator project name";
		}

		@Override
		public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {

			if (formData.isEmpty()) {
				return null;
			}

			return new RadiatorProjectProperty(formData.getString("projectName"));
		}
	}
}
