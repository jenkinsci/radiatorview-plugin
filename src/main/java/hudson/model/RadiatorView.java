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
	 Boolean showStable = false;

	/**
	 * User configuration - show details in stable builds.
	 */
	 Boolean showStableDetail = false;

	/**
	 * User configuration - show build stability icon.
	 */
	 Boolean showBuildStability = false;

	/**
	 * User configuration - high visibility mode.
	 */
	 Boolean highVis = Boolean.TRUE;

	/**
	 * User configuration - group builds by common prefix.
	 */
	 Boolean groupByPrefix = Boolean.TRUE;

	 /**
	  * User configuration - text for the caption to be used on the radiator's headline.
	  */
	 String captionText;
	 
	 /**
	  * User configuration - size in points (1pt = 1/72in) for the caption to be used on the radiator's headline.
	  */
	 Integer captionSize;

	 /**
	  * User configuration - show background images per status.
	  */
	 Boolean useBackgrounds = false;

	 /**
	  * User configuration - background image URL for 'not built' status.
	  */
	 String otherImage;
	 
	 /**
	  * User configuration - background image URL for successful build.
	  */
	 String successImage;

	 /**
	  * User configuration - background image URL for unstable build.
	  */
	 String unstableImage;

	 /**
	  * User configuration - background image URL for failed build.
	  */
	 String brokenImage;

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
	 * @param showBuildStability
	 *            Shows weather icon for job view when true.
	 * @param captionText
	 *            Caption text to be used on the radiator's headline.
	 * @param captionSize
	 *            Caption size for the radiator's headline.
	 * @param useBackgrounds
	 *            if background images for status display should be used
	 * @param otherImage
	 *            URL for not built jobs
	 * @param successImage
	 *            URL for successful jobs
	 * @param unstableImage
	 *            URL for unstable jobs
	 * @param brokenImage
	 *            URL for failed jobs
	 */
	@DataBoundConstructor
	public RadiatorView(String name, Boolean showStable,
			Boolean showStableDetail, Boolean highVis, Boolean groupByPrefix,
			Boolean showBuildStability, String captionText, Integer captionSize,
			Boolean useBackgrounds,
			String otherImage,
			String successImage,
			String unstableImage,
			String brokenImage) {
		super(name);
		this.showStable = showStable;
		this.showStableDetail = showStableDetail;
		this.highVis = highVis;
		this.groupByPrefix = groupByPrefix;
		this.showBuildStability = showBuildStability;
		this.captionText = captionText;
		this.captionSize = captionSize;
		this.useBackgrounds = useBackgrounds;
		this.otherImage = otherImage;
		this.successImage = successImage;
		this.unstableImage = unstableImage;
		this.brokenImage = brokenImage;
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
