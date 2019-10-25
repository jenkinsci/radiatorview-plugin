package hudson.model;

import java.io.IOException;
import java.text.NumberFormat;

import javax.servlet.ServletException;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.Descriptor.FormException;
import hudson.tasks.test.AbstractTestResultAction;

public class PipelineView extends ListView
{
	private static final int DEFAULT_CAPTION_SIZE = 36;

	/**
	  * User configuration - text for the caption to be used on the radiator's headline.
	  */
	@DataBoundSetter
	String captionText;

	@DataBoundSetter
	Integer captionSize;

	/**
	* @param name
	*            view name.
	*/
	@DataBoundConstructor
	public PipelineView(String name)
	{
		super(name);
	}

	public String getCaptionText()
	{
		return captionText;
	}

	public Integer getCaptionSize()
	{
		return captionSize;
	}

    @Override
	protected void submit(StaplerRequest req) throws ServletException, IOException, FormException
	{
		super.submit(req);

		this.captionText = req.getParameter("captionText");

		try
		{
			this.captionSize = Integer.parseInt(req.getParameter("captionSize"));
		}
		catch (NumberFormatException e)
		{
			this.captionSize = DEFAULT_CAPTION_SIZE;
		}
	}

	public int getTestCount(TopLevelItem item)
	{
		int testCount = 0;
		
		if (item instanceof WorkflowJob)
		{
			WorkflowRun run = ((WorkflowJob)item).getLastCompletedBuild();
			if (run != null)
			{
				for (AbstractTestResultAction<?> results : run.getActions(AbstractTestResultAction.class))
				{
					if (results != null) testCount += results.getTotalCount();
				}
			}
		}
		
		return testCount;
	}

	public int getFailCount(TopLevelItem item)
	{
		int testCount = 0;
		
		if (item instanceof WorkflowJob)
		{
			WorkflowRun run = ((WorkflowJob)item).getLastCompletedBuild();
			if (run != null)
			{
				for (AbstractTestResultAction<?> results : run.getActions(AbstractTestResultAction.class))
				{
					if (results != null) testCount += results.getFailCount();
				}
			}
		}
		
		return testCount;
	}

	public int getSkipCount(TopLevelItem item)
	{
		int testCount = 0;
		
		if (item instanceof WorkflowJob)
		{
			WorkflowRun run = ((WorkflowJob)item).getLastCompletedBuild();
			if (run != null)
			{
				for (AbstractTestResultAction<?> results : run.getActions(AbstractTestResultAction.class))
				{
					if (results != null) testCount += results.getSkipCount();
				}
			}
		}
		
		return testCount;
	}

	public int getPassCount(TopLevelItem item)
	{
		int testCount = this.getTestCount(item);

		if (testCount > 0)
		{
			testCount -= this.getFailCount(item);
			testCount -= this.getSkipCount(item);
		}
		
		return testCount;
	}
	
	public String getSuccessPercentage(TopLevelItem item)
	{
		if (this.getTestCount(item) > 0)
		{
			Double perc = this.getPassCount(item) / (this.getTestCount(item) * 1D);
			return NumberFormat.getPercentInstance().format(perc);
		}
		return "";
	}

	@Extension
	public static class DescriptorImpl extends ListView.DescriptorImpl
	{
		@Override
		public String getDisplayName()
		{
			return "Pipeline Radiator";
		}
	}
}
