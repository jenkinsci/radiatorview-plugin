package hudson.model;

import java.util.List;
import java.util.function.Predicate;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.views.ViewJobFilter;

public class PipelineViewJobFilter extends ViewJobFilter
{
	@DataBoundConstructor
	public PipelineViewJobFilter()
	{
	}

	@Override
	public List<TopLevelItem> filter(List<TopLevelItem> added, List<TopLevelItem> all, View filteringView)
	{
		added.removeIf(new Predicate<TopLevelItem>()
		{
			@Override
			public boolean test(TopLevelItem item)
			{
				return item instanceof WorkflowJob ? false : true;
			}
		});
		return added;
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<ViewJobFilter>
	{
		@Override
		public String getDisplayName()
		{
			return "Pipeline Jobs only";
		}
	}

}
