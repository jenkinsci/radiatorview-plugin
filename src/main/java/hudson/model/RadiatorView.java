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
import org.kohsuke.stapler.DataBoundSetter;
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
	
	private static final int DEFAULT_CAPTION_SIZE = 36;

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

	 /**
	  * User configuration - show background images per status.
	  */
	 @DataBoundSetter
	 Boolean useBackgrounds = false;

	 /**
	  * User configuration - background image URL for 'not built' status.
	  */
	 @DataBoundSetter
	 String otherImage;
	 
	 /**
	  * User configuration - background image URL for successful build.
	  */
	 @DataBoundSetter
	 String successImage;

	 /**
	  * User configuration - background image URL for unstable build.
	  */
	 @DataBoundSetter
	 String unstableImage;

	 /**
	  * User configuration - background image URL for failed build.
	  */
	 @DataBoundSetter
	 String brokenImage;

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
		ProjectViewEntry contents = new ProjectViewEntry();

		placeInQueue = new HashMap<hudson.model.Queue.Item, Integer>();
		int j = 1;
		for (hudson.model.Queue.Item i : Hudson.getInstance().getQueue()
				.getItems()) {
			placeInQueue.put(i, j++);
		}

		for (TopLevelItem item : super.getItems()) {
			if(item instanceof Job && !isDisabled(item)) {
				IViewEntry entry = new JobViewEntry(this, (Job<?, ?>) item);
				contents.addBuild(entry);
			}
		}
		return contents;
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
		try {
			this.captionSize = Integer.parseInt(req.getParameter("captionSize"));
		} catch (NumberFormatException e) {
			this.captionSize = DEFAULT_CAPTION_SIZE;
		}
		this.useBackgrounds = Boolean.valueOf("on".equals(req.getParameter("useBackgrounds")));
		if (Boolean.TRUE.equals(this.useBackgrounds)) {
			this.brokenImage = req.getParameter("brokenImage");
			this.unstableImage = req.getParameter("unstableImage");
			this.successImage = req.getParameter("successImage");
			this.otherImage = req.getParameter("otherImage");
		} else {
			this.brokenImage =
			this.unstableImage =
			this.successImage =
			this.otherImage = "";
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

	public Boolean getUseBackgrounds() {
		return useBackgrounds;
	}

	public String getOtherImage() {
		return otherImage;
	}

	public String getSuccessImage() {
		return successImage;
	}

	public String getUnstableImage() {
		return unstableImage;
	}

	public String getBrokenImage() {
		return brokenImage;
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
