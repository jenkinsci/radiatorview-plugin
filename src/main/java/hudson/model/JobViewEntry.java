package hudson.model;

import hudson.Functions;
import hudson.plugins.claim.ClaimBuildAction;
import hudson.tasks.test.AbstractTestResultAction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;

/**
 * Represents a job to be shown in a view. Based heavily on the XFPanelEntry in
 * XFPanel plugin.
 * 
 * @author jrenaut
 */
public class JobViewEntry implements IViewEntry {

	private static final String NOT_CLAIMED = "Not Claimed";

	private final RadiatorView radiatorView;

	private Job<?, ?> job;

	private String backgroundColor;

	private String color;

	private Boolean broken = false;

	private Boolean building = false;

	/**
	 * If the build is stable.
	 */
	private boolean stable;

	/**
	 * C'tor
	 * 
	 * @param job
	 *            the job to be represented
	 * @param radiatorView
	 *            TODO
	 */
	public JobViewEntry(RadiatorView radiatorView, Job<?, ?> job) {
		this.radiatorView = radiatorView;
		this.job = job;
		this.findStatus();
	}

	/**
	 * @return the job
	 */
	public Job<?, ?> getJob() {
		return this.job;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getName()
	 */
	public String getName() {
		return job.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getQueued()
	 */
	public Boolean getQueued() {
		return this.job.isInQueue();
	}

	/**
	 * @return the job's queue number, if any
	 */
	public Integer getQueueNumber() {
		return this.radiatorView.placeInQueue.get(this.job.getQueueItem());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getBackgroundColor()
	 */
	public String getBackgroundColor() {
		return this.backgroundColor;
	}

	public String getStatus() {
		if (getStable()) {
			return "successful";
		}
		if (!StringUtils.isEmpty(getClaim())
				&& !getClaim().equals(NOT_CLAIMED + ".")) {
			return "claimed";
		}
		if (getBroken()) {
			return "failing";
		}
		return "unstable";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getColor()
	 */
	public String getColor() {
		return this.color;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getBroken()
	 */
	public Boolean getBroken() {
		return this.broken;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getBuilding()
	 */
	public Boolean getBuilding() {
		return this.building;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getUrl()
	 */
	public String getUrl() {
		return this.job.getUrl();
	}

	public String getLastBuildUrl() {
		return this.job.getUrl() + "lastBuild";
	}

	/**
	 * @return a list will all the currently building runs for this job.
	 */
	public List<Run<?, ?>> getBuildsInProgress() {
		List<Run<?, ?>> runs = new ArrayList<Run<?, ?>>();

		Run<?, ?> run = this.job.getLastBuild();
		if (run != null) {
			if (run.isBuilding()) {
				runs.add(run);
			}

			Run<?, ?> prev = run.getPreviousBuildInProgress();
			while (prev != null) {
				runs.add(prev);
				prev = prev.getPreviousBuildInProgress();
			}
		}

		return runs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getTestCount()
	 */
	public int getTestCount() {
		Run<?, ?> run = this.job.getLastSuccessfulBuild();
		if (run != null) {
			AbstractTestResultAction<?> tests = run
					.getAction(AbstractTestResultAction.class);
			return tests != null ? tests.getTotalCount() : 0;
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getFailCount()
	 */
	public int getFailCount() {
		Run<?, ?> run = this.job.getLastSuccessfulBuild();
		if (run != null) {
			AbstractTestResultAction<?> tests = run
					.getAction(AbstractTestResultAction.class);
			return tests != null ? tests.getFailCount() : 0;
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getSuccessCount()
	 */
	public int getSuccessCount() {
		return this.getTestCount() - this.getFailCount();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getDiff()
	 */
	public String getDiff() {
		Run<?, ?> run = this.job.getLastSuccessfulBuild();
		if (run != null) {
			Run<?, ?> previous = this.getLastSuccessfulFrom(run);
			if (previous != null) {
				AbstractTestResultAction<?> tests = run
						.getAction(AbstractTestResultAction.class);
				AbstractTestResultAction<?> prevTests = previous
						.getAction(AbstractTestResultAction.class);
				if (tests != null && prevTests != null) {
					int currentSuccess = tests.getTotalCount()
							- tests.getFailCount();
					int prevSuccess = prevTests.getTotalCount()
							- prevTests.getFailCount();
					int diff = currentSuccess - prevSuccess;
					if (diff != 0) {
						return Functions.getDiffString(diff);
					} else {
						return "";
					}
				}
			}
		}
		return "";
	}

	/**
	 * @param run
	 *            a run
	 * @return the last successful run prior to the given run
	 */
	private Run<?, ?> getLastSuccessfulFrom(Run<?, ?> run) {
		Run<?, ?> r = run.getPreviousBuild();
		while (r != null
				&& (r.isBuilding() || r.getResult() == null || r.getResult()
						.isWorseThan(Result.UNSTABLE))) {
			r = r.getPreviousBuild();
		}
		return r;
	}

	public Collection<String> getCulprits() {
		Run<?, ?> run = this.job.getLastBuild();
		Set<String> culprits = new HashSet<String>();
		while (run != null) {
			if (run instanceof AbstractBuild<?, ?>) {

				AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;

				Iterator<User> it = build.getCulprits().iterator();
				while (it.hasNext()) {
					culprits.add(it.next().getFullName());
				}
			}
			run = run.getPreviousBuild();
			if (run != null && Result.SUCCESS.equals(run.getResult())) {
				// don't look for culprits in successful builds.
				run = null;
			}
		}
		return culprits;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getCulprit()
	 */
	public String getCulprit() {
		Collection<String> culprits = getCulprits();
		String culprit = " - ";
		if (!culprits.isEmpty()) {
			culprit = StringUtils.join(culprits, ", ");
		}
		return culprit;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getDiffColor()
	 */
	public String getDiffColor() {
		String diff = this.getDiff().trim();
		if (diff.length() > 0 && !Functions.getDiffString(0).equals(diff)) {
			if (diff.startsWith("-")) {
				return "#FF0000";
			} else {
				return "#00FF00";
			}
		}
		return "#FFFFFF";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getSuccessPercentage()
	 */
	public String getSuccessPercentage() {
		if (this.getTestCount() > 0) {
			Double perc = this.getSuccessCount() / (this.getTestCount() * 1D);
			return NumberFormat.getPercentInstance().format(perc);
		}
		return "";
	}

	/**
	 * Determines some information of the current job like which colors use,
	 * wether it's building or not or broken.
	 */
	private void findStatus() {
		Result result = RadiatorUtil.getLastFinishedResult(job);

		this.stable = false;
		if (result.ordinal == Result.NOT_BUILT.ordinal) {
			this.backgroundColor = getColors().getOtherBG();
			this.color = getColors().getOtherFG();
			this.broken = true;
		} else if (result.ordinal == Result.SUCCESS.ordinal) {
			this.backgroundColor = getColors().getOkBG();
			this.color = getColors().getOkFG();
			this.broken = false;
			this.stable = true;
		} else if (result.ordinal == Result.UNSTABLE.ordinal) {
			this.backgroundColor = getColors().getFailedBG();
			this.color = getColors().getFailedFG();
			this.broken = false;
		} else {
			this.backgroundColor = getColors().getBrokenBG();
			this.color = getColors().getBrokenFG();
			this.broken = true;
		}

		switch (this.job.getIconColor()) {
		case BLUE_ANIME:
		case YELLOW_ANIME:
		case RED_ANIME:
		case GREY_ANIME:
		case DISABLED_ANIME:
			this.building = true;
			break;
		default:
			this.building = false;
		}
	}

	private ViewEntryColors getColors() {
		return radiatorView.getColors();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getLastCompletedBuild()
	 */
	public String getLastCompletedBuild() {
		Run build = job.getLastCompletedBuild();
		if (build != null) {
			return build.getTimestampString() + " ("
					+ build.getDurationString() + ")";
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getLastStableBuild()
	 */
	public String getLastStableBuild() {
		Run build = job.getLastStableBuild();
		if (build != null) {
			return build.getTimestampString() + " (in "
					+ build.getDurationString() + ")";
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getStable()
	 */
	public boolean getStable() {
		return stable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getClaim()
	 */
	public String getClaim() {
		String claim = null;
		if (Hudson.getInstance().getPlugin("claim") != null) {
			claim = NOT_CLAIMED;
			Run lastBuild = job.getLastBuild();
			while (lastBuild != null && lastBuild.isBuilding()) {
				// claims can only be made against builds once they've finished,
				// so check the previous build if currently building.
				lastBuild = lastBuild.getPreviousBuild();
			}
			if (lastBuild != null) {
				// TODO - check previous build if currently building.
				List<ClaimBuildAction> claimActionList = lastBuild
						.getActions(ClaimBuildAction.class);
				if (claimActionList.size() == 1) {
					ClaimBuildAction claimAction = claimActionList.get(0);
					if (claimAction.isClaimed()) {
						String by = claimAction.getClaimedByName();
						String reason = claimAction.getReason();
						claim = // "Claimed by " +
						by;
						if (reason != null) {
							claim += ": " + reason;
						}
					}
				} else if (claimActionList.size() > 1) {
					claim = "Error parsing claim details";
					Log.warn("Multiple ClaimBuildActions found for job "
							+ job.toString());
				}
			}
			claim += ".";
		}
		return claim;
	}

	public boolean isClaimed() {
		return !StringUtils.isEmpty(getClaim())
				&& !"Not Claimed.".equals(getClaim());
	}

	public Result getLastFinishedResult() {
		return RadiatorUtil.getLastFinishedResult(job);
	}

	public boolean hasChildren() {
		return false;
	}
}