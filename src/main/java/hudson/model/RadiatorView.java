package hudson.model;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A configurable Radiator-Style job view suitable for use in extreme feedback
 * systems - ideal for running on a spare PC in the office. Many thanks to
 * Julien Renaut for the xfpanel plugin that inspired some of the updates to
 * this view.
 * 
 * @author Mark Howard (mh@tildemh.com)
 */
public class RadiatorView extends ListView {
	
	private static final int DEFAULT_CAPTION_SIZE = 36;

	private static final Logger LOGGER = Logger.getLogger(RadiatorView.class.getName());

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
	 @DataBoundSetter
	 Boolean showStable = false;

	/**
	 * User configuration - show details in stable builds.
	 */
	 @DataBoundSetter
	 Boolean showStableDetail = false;

	/**
	 * User configuration - show build stability icon.
	 */
	 @DataBoundSetter
	 Boolean showBuildStability = false;

	/**
	 * User configuration - high visibility mode.
	 */
	 @DataBoundSetter
	 Boolean highVis = true;

	/**
	 * User configuration - group builds by common prefix.
	 */
	 @DataBoundSetter
	 Boolean groupByPrefix = true;

	 /**
	  * User configuration - text for the caption to be used on the radiator's headline.
	  */
	 @DataBoundSetter
	 String captionText;

	 /**
	  * User configuration - size in points (1pt = 1/72in) for the caption to be used on the radiator's headline.
	  */
	 @DataBoundSetter
	 Integer captionSize;

	 @DataBoundSetter
	 String excludeRegex;

	/**
	 * @param name
	 *            view name.
	 */
	@DataBoundConstructor
	public RadiatorView(String name) {
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
		ProjectViewEntry content = new ProjectViewEntry();

		placeInQueue = new HashMap<hudson.model.Queue.Item, Integer>();
		int j = 1;
		for (hudson.model.Queue.Item i : Jenkins.getActiveInstance().getQueue()
				.getItems()) {
			placeInQueue.put(i, j++);
		}

		LOGGER.fine("Collecting items for view " + getViewName());
		addItems(getItems(), content);
		return content;
	}

	private void addItems(Collection<TopLevelItem> items, ProjectViewEntry content) {
		for (TopLevelItem item : items) {
			LOGGER.fine(item.getName() + " (" + item.getClass() + ")");
			if (item instanceof AbstractFolder) {
				addItems(((AbstractFolder) item).getItems(), content);
			}
			if (item instanceof Job && !isDisabled(item) && !isExcluded(item)) {
				IViewEntry entry = new JobViewEntry(this, (Job<?, ?>) item);
				content.addBuild(entry);
			}
		}
	}

	private boolean isExcluded(TopLevelItem item) {
		final boolean matches = Pattern.matches(excludeRegex, item.getFullName());
		LOGGER.log(Level.FINE, "Checking {0}, fullName={1}, excluded={2}",
		           new String[]{item.getName(), item.getFullName(), String.valueOf(matches)});
		return matches;
	}

	private boolean isDisabled(TopLevelItem item) {
		return item instanceof AbstractProject && ((AbstractProject) item).isDisabled();
	}

	public ProjectViewEntry getContentsByPrefix()
	{
		ProjectViewEntry contents = new ProjectViewEntry();
		ProjectViewEntry allContents = getContents();
		Map<String, ProjectViewEntry> jobsByPrefix = new HashMap<String, ProjectViewEntry>();
		
		for (IViewEntry job: allContents.getJobs())
		{
			String projectName = job.getProjectName();
			String prefix = StringUtils.isEmpty(projectName) ? getPrefix(job.getName()) : projectName;
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


	public String getExcludeRegex() {
		return excludeRegex;
	}

	@Override
	protected void submit(StaplerRequest req) throws ServletException, IOException, 
			FormException {
		super.submit(req);
		this.showStable = Boolean.parseBoolean(req.getParameter("showStable"));
		this.showStableDetail = Boolean.parseBoolean(req.getParameter("showStableDetail"));
		this.highVis = Boolean.parseBoolean(req.getParameter("highVis"));
		this.groupByPrefix = Boolean.parseBoolean(req.getParameter("groupByPrefix"));
		this.showBuildStability = Boolean.parseBoolean(req.getParameter("showBuildStability"));
		this.captionText = req.getParameter("captionText");
		this.excludeRegex = req.getParameter("excludeRegex");
		try {
			this.captionSize = Integer.parseInt(req.getParameter("captionSize"));
		} catch (NumberFormatException e) {
			this.captionSize = DEFAULT_CAPTION_SIZE;
		}
		
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

	public Boolean getShowBuildStability() {
		return showBuildStability;
	}

	public String getCaptionText() {
		return captionText;
	}

	public Integer getCaptionSize() {
		return captionSize;
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
