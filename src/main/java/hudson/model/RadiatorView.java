package hudson.model;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A configurable Radiator-Style job view suitable for use in extreme feedback
 * systems - ideal for running on a spare PC in the office. Many thanks to
 * Julien Renaut for the xfpanel plugin that inspired some of the updates to
 * this view.
 * 
 * @author Mark Howard (mh@tildemh.com)
 */
public class RadiatorView extends ListView {
	/**
	 * Entries to be shown in the view.
	 */
	private transient Collection<IViewEntry> entries;

	/**
	 * Cache of location of jobs in the build queue.
	 */
	transient Map<hudson.model.Queue.Item, Integer> placeInQueue = new HashMap<hudson.model.Queue.Item, Integer>();

	/**
	 * Colours to use in the view.
	 */
	transient ViewEntryColors colors;

	/**
	 * User configuration - show stable builds when there are some unstable
	 * builds.
	 */
	 Boolean showStable = false;

	/**
	 * User configuration - show details in stable builds.
	 */
	 Boolean showStableDetail = false;

	/**
	 * User configuration - high visibility mode.
	 */
	 Boolean highVis = true;

	/**
	 * User configuration - group builds by common prefix.
	 */
	 Boolean groupByPrefix = true;

	/**
	 * @param name
	 *            view name.
	 * @param showStable
	 *            if stable builds should be shown.
	 * @param showStableDetail
	 *            if detail should be shown for stable builds.
	 * @param highVis
	 *            high visibility mode.
	 * @param groupByPrefix
	 *            If true, builds will be shown grouped together based on the
	 *            prefix of the job name.
	 */
	@DataBoundConstructor
	public RadiatorView(String name, Boolean showStable,
			Boolean showStableDetail, Boolean highVis, Boolean groupByPrefix) {
		super(name);
		this.showStable = showStable;
		this.showStableDetail = showStableDetail;
		this.highVis = highVis;
		this.groupByPrefix = groupByPrefix;
	}
	
	public RadiatorView(String name)
	{
		super(name);
	}

	/**
	 * @return the colors to use
	 */
	public ViewEntryColors getColors() {
		if (this.colors == null) {
			this.colors = ViewEntryColors.DEFAULT;
		}
		return this.colors;
	}

	public ProjectViewEntry getContents() {
		ProjectViewEntry contents = new ProjectViewEntry();

		placeInQueue = new HashMap<hudson.model.Queue.Item, Integer>();
		int j = 1;
		for (hudson.model.Queue.Item i : Hudson.getInstance().getQueue()
				.getItems()) {
			placeInQueue.put(i, j++);
		}

		for (TopLevelItem item : super.getItems()) {
			if (item instanceof AbstractProject) {
				AbstractProject project = (AbstractProject) item;
				if (!project.isDisabled()) {
					IViewEntry entry = new JobViewEntry(this, project);
					contents.addBuild(entry);
				}
			}
		}

		return contents;
	}
	
	public ProjectViewEntry getContentsByPrefix()
	{
		ProjectViewEntry contents = new ProjectViewEntry();
		ProjectViewEntry allContents = getContents();
		Map<String, ProjectViewEntry> jobsByPrefix = new HashMap<String, ProjectViewEntry>();
		
		for (IViewEntry job: allContents.getJobs())
		{
			String prefix = getPrefix(job.getName());
			ProjectViewEntry project = jobsByPrefix.get(prefix);
			if (project == null)
			{
				project = new ProjectViewEntry(prefix);
				jobsByPrefix.put(prefix, project);
				contents.addBuild(project);
			}
			project.addBuild(job);
		}
		return contents;
	}

	private String getPrefix(String name) 
	{
		if (name.contains("_"))
		{
			return StringUtils.substringBefore(name, "_");
		}		
		if (name.contains("-"))
		{
			return StringUtils.substringBefore(name, "-");
		}		
		if (name.contains(":"))
		{
			return StringUtils.substringBefore(name, ":");
		}
		else return "No Project";
	}


	/**
	 * Gets from the request the configuration parameters
	 * 
	 * @param req
	 *            {@link StaplerRequest}
	 * @throws ServletException
	 *             if any
	 * @throws FormException
	 *             if any
	 */
	@Override
	protected void submit(StaplerRequest req) throws ServletException, IOException, 
			FormException {
		super.submit(req);
		this.showStable = Boolean.parseBoolean(req.getParameter("showStable"));
		this.showStableDetail = Boolean.parseBoolean(req
				.getParameter("showStableDetail"));
		this.highVis = Boolean.parseBoolean(req.getParameter("highVis"));
		this.groupByPrefix = Boolean.parseBoolean(req.getParameter("groupByPrefix"));
	}

	public Boolean getShowStable() {
		return showStable;
	}

	public Boolean getShowStableDetail() {
		return showStableDetail;
	}

	public Boolean getHighVis() {
		return highVis;
	}
	
	public Boolean getGroupByPrefix()
	{
		return groupByPrefix;
	}

	/**
	 * Converts a list of jobs to a list of list of jobs, suitable for display
	 * as rows in a table.
	 * 
	 * @param jobs
	 *            the jobs to include.
	 * @param failingJobs
	 *            if this is a list of failing jobs, in which case fewer jobs
	 *            should be used per row.
	 * @return a list of fixed size view entry lists.
	 */
	public Collection<Collection<IViewEntry>> toRows(Collection<IViewEntry> jobs,
			Boolean failingJobs) {
		int jobsPerRow = 1;
		if (failingJobs.booleanValue()) {
			if (jobs.size() > 3) {
				jobsPerRow = 2;
			}
			if (jobs.size() > 9) {
				jobsPerRow = 3;
			}
			if (jobs.size() > 15) {
				jobsPerRow = 4;
			}
		} else {
			// don't mind having more rows as much for passing jobs.
			jobsPerRow = (int) Math.floor(Math.sqrt(jobs.size()) / 1.5);
		}
		Collection<Collection<IViewEntry>> rows = new ArrayList<Collection<IViewEntry>>();
		Collection<IViewEntry> current = null;
		int i = 0;
		for (IViewEntry job : jobs) {
			if (i == 0) {
				current = new ArrayList<IViewEntry>();
				rows.add(current);
			}
			current.add(job);
			i++;
			if (i >= jobsPerRow) {
				i = 0;
			}
		}
		return rows;
	}


	@Extension
	public static final class DescriptorImpl extends ViewDescriptor {
		public DescriptorImpl() {
			super(RadiatorView.class);
		}

		@Override
		public String getDisplayName() {
			return "Radiator";
		}

		/**
		 * Checks if the include regular expression is valid.
		 */
		public FormValidation doCheckIncludeRegex(@QueryParameter String value) {
			String v = Util.fixEmpty(value);
			if (v != null) {
				try {
					Pattern.compile(v);
				} catch (PatternSyntaxException pse) {
					return FormValidation.error(pse.getMessage());
				}
			}
			return FormValidation.ok();
		}
	}
}
