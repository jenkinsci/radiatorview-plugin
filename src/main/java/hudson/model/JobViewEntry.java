package hudson.model;

import hudson.Functions;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.tasks.test.AbstractTestResultAction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

/**
 * Represents a job to be shown in a view. Based heavily on the XFPanelEntry in
 * XFPanel plugin.
 * 
 * @author jrenaut
 */
public class JobViewEntry implements IViewEntry
{

	private static final String NOT_CLAIMED = "Not Claimed.";

	private final RadiatorView radiatorView;

	private Job<?, ?> job;

	private String backgroundColor;

	private String color;

	private Boolean broken = false;

	private Boolean building = false;

	private boolean stable = false;

	private boolean notBuilt = false;

	/**
	 * C'tor
	 * 
	 * @param job          the job to be represented
	 * @param radiatorView TODO
	 */
	public JobViewEntry(RadiatorView radiatorView, Job<?, ?> job)
	{
		this.radiatorView = radiatorView;
		this.job = job;
		this.findStatus();
	}

	/**
	 * @return the job
	 */
	public Job<?, ?> getJob()
	{
		return this.job;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getName()
	 */
	public String getName()
	{
		return job.getFullName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getQueued()
	 */
	public Boolean getQueued()
	{
		return this.job.isInQueue();
	}

	/**
	 * @return the job's queue number, if any
	 */
	public Integer getQueueNumber()
	{
		return this.radiatorView.placeInQueue.get(this.job.getQueueItem());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getBackgroundColor()
	 */
	public String getBackgroundColor()
	{
		return this.backgroundColor;
	}

	public String getStatus()
	{
		if (isNotBuilt()) { return "never built"; }
		if (getStable()) { return "successful"; }
		if (isCompletelyClaimed()) { return "claimed"; }
		if (getBroken()) { return "failing"; }
		return "unstable";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getColor()
	 */
	public String getColor()
	{
		return this.color;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getBroken()
	 */
	public Boolean getBroken()
	{
		return this.broken;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getBuilding()
	 */
	public Boolean getBuilding()
	{
		return this.building;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getUrl()
	 */
	public String getUrl()
	{
		return this.job.getUrl();
	}

	public String getLastBuildUrl()
	{
		Run lastBuild = job.getLastBuild();
		if (lastBuild == null) { return job.getUrl(); }
		return lastBuild.getUrl();
	}

	/**
	 * @return a list will all the currently building runs for this job.
	 */
	public List<Run<?, ?>> getBuildsInProgress()
	{
		List<Run<?, ?>> runs = new ArrayList<Run<?, ?>>();

		Run<?, ?> run = this.job.getLastBuild();
		if (run != null)
		{
			if (run.isBuilding())
			{
				runs.add(run);
			}

			Run<?, ?> prev = run.getPreviousBuildInProgress();
			while (prev != null)
			{
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
	public int getTestCount()
	{
		Run<?, ?> run = this.job.getLastCompletedBuild();
		if (run != null)
		{
			AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
			return tests != null ? tests.getTotalCount() : 0;
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getFailCount()
	 */
	public int getFailCount()
	{
		Run<?, ?> run = this.job.getLastCompletedBuild();
		if (run != null)
		{
			AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
			return tests != null ? tests.getFailCount() : 0;
		}
		return 0;
	}

	public int getSkipCount()
	{
		Run<?, ?> run = this.job.getLastCompletedBuild();
		if (run != null)
		{
			AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
			return tests != null ? tests.getSkipCount() : 0;
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getSuccessCount()
	 */
	public int getSuccessCount()
	{
		return this.getTestCount() - this.getFailCount() - this.getSkipCount();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getDiff()
	 */
	public String getDiff()
	{
		Run<?, ?> run = this.job.getLastSuccessfulBuild();
		if (run != null)
		{
			Run<?, ?> previous = this.getLastSuccessfulFrom(run);
			if (previous != null)
			{
				AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
				AbstractTestResultAction<?> prevTests = previous.getAction(AbstractTestResultAction.class);
				if (tests != null && prevTests != null)
				{
					int currentSuccess = tests.getTotalCount() - tests.getFailCount();
					int prevSuccess = prevTests.getTotalCount() - prevTests.getFailCount();
					int diff = currentSuccess - prevSuccess;
					if (diff != 0)
					{
						return Functions.getDiffString(diff);
					} else
					{
						return "";
					}
				}
			}
		}
		return "";
	}

	/**
	 * @param run a run
	 * @return the last successful run prior to the given run
	 */
	private Run<?, ?> getLastSuccessfulFrom(Run<?, ?> run)
	{
		Run<?, ?> previousBuild = run.getPreviousBuild();
		while (hasPreviousBuildBuildingOrWithResultWorseThanUnstable(previousBuild))
		{
			previousBuild = previousBuild.getPreviousBuild();
		}
		return previousBuild;
	}

	private boolean hasPreviousBuildBuildingOrWithResultWorseThanUnstable(Run<?, ?> previousBuild)
	{
		if (previousBuild != null)
		{
			if (previousBuild.isBuilding()) { return true; }
			final Result result = previousBuild.getResult();
			if (result == null) { return true; }
			if (result.isWorseThan(Result.UNSTABLE)) { return true; }
		}
		return false;
	}

	public Collection<String> getCulprits()
	{
		Run<?, ?> run = this.job.getLastBuild();
		Set<String> culprits = new HashSet<String>();
		while (run != null)
		{
			if (run instanceof AbstractBuild<?, ?>)
			{

				AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;

				Iterator<User> it = build.getCulprits().iterator();
				while (it.hasNext())
				{
					culprits.add(it.next().getFullName());
				}
			}
			run = run.getPreviousBuild();
			if (run != null && Result.SUCCESS.equals(run.getResult()))
			{
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
	public String getCulprit()
	{
		Collection<String> culprits = getCulprits();
		String culprit = " - ";
		if (!culprits.isEmpty())
		{
			culprit = StringUtils.join(culprits, ", ");
		}
		return culprit;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getDiffColor()
	 */
	public String getDiffColor()
	{
		String diff = this.getDiff().trim();
		if (diff.length() > 0 && !Functions.getDiffString(0).equals(diff))
		{
			if (diff.startsWith("-"))
			{
				return "#FF0000";
			} else
			{
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
	public String getSuccessPercentage()
	{
		if (this.getTestCount() > 0)
		{
			Double perc = this.getSuccessCount() / (this.getTestCount() * 1D);
			return NumberFormat.getPercentInstance().format(perc);
		}
		return "";
	}

	/**
	 * Determines some information of the current job like which colors use, wether
	 * it's building or not or broken.
	 */
	private void findStatus()
	{
		Result result = RadiatorUtil.getLastFinishedResult(job);

		if (result.ordinal == Result.NOT_BUILT.ordinal)
		{
			this.backgroundColor = getColors().getOtherBG();
			this.color = getColors().getOtherFG();
			this.notBuilt = true;
		} else if (result.ordinal == Result.SUCCESS.ordinal)
		{
			this.backgroundColor = getColors().getOkBG();
			this.color = getColors().getOkFG();
			this.stable = true;
		} else if (result.ordinal == Result.UNSTABLE.ordinal)
		{
			this.backgroundColor = getColors().getFailedBG();
			this.color = getColors().getFailedFG();
		} else
		{
			this.backgroundColor = getColors().getBrokenBG();
			this.color = getColors().getBrokenFG();
			this.broken = true;
		}

		switch (this.job.getIconColor())
		{
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

	private ViewEntryColors getColors()
	{
		return radiatorView.getColors();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getLastCompletedBuild()
	 */
	public String getLastCompletedBuild()
	{
		Run build = job.getLastCompletedBuild();
		if (build != null) { return build.getTimestampString() + " (" + build.getDurationString() + ")"; }
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getLastStableBuild()
	 */
	public String getLastStableBuild()
	{
		Run build = job.getLastStableBuild();
		if (build != null) { return build.getTimestampString() + " (in " + build.getDurationString() + ")"; }
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getStable()
	 */
	public boolean getStable()
	{
		return stable;
	}

	public boolean isNotBuilt()
	{
		return notBuilt;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.IViewEntry#getClaim()
	 */
	public String getClaim()
	{
		// check we have claim plugin
		if (Jenkins.getActiveInstance().getPlugin("claim") == null) { return null; }
		Run<?, ?> lastBuild = getLastCompletedRun();
		if (lastBuild == null) { return null; }
		// find the claim
		final String claim;
		if (lastBuild instanceof hudson.matrix.MatrixBuild)
		{
			MatrixBuild matrixBuild = (hudson.matrix.MatrixBuild) lastBuild;
			claim = buildMatrixClaimString(matrixBuild, true);
		} else
		{
			ClaimWrapper claimWrapper = ClaimWrapper.builder(lastBuild);
			if (claimWrapper != null && claimWrapper.isClaimed())
			{
				StringBuilder sb = new StringBuilder();
				if (claimWrapper.getReason() != null)
				{
					sb.append(claimWrapper.getReason()).append(" ");
				}
				sb.append("(");
				sb.append(claimWrapper.getClaimedByName());
				sb.append(").");
				claim = sb.toString();
			} else
			{
				claim = NOT_CLAIMED;
			}
		}
		return claim;
	}

	public String getUnclaimedMatrixBuilds()
	{
		if (Jenkins.getActiveInstance().getPlugin("claim") == null) { return ""; }
		Run<?, ?> lastBuild = getLastCompletedRun();
		if (!(lastBuild instanceof hudson.matrix.MatrixBuild)) { return ""; }
		MatrixBuild matrixBuild = (hudson.matrix.MatrixBuild) lastBuild;
		return buildMatrixClaimString(matrixBuild, false);
	}

	private Run<?, ?> getLastCompletedRun()
	{
		Run<?, ?> run = job.getLastBuild();
		while (run != null && run.isBuilding())
		{
			// claims can only be made against builds once they've finished,
			// so check the previous build if currently building.
			run = run.getPreviousBuild();
		}
		return run;
	}

	private String buildMatrixClaimString(MatrixBuild matrixBuild, boolean includeClaimed)
	{
		StringBuilder claimed = new StringBuilder();
		StringBuilder unclaimed = new StringBuilder();
		for (MatrixRun combination : matrixBuild.getRuns())
		{
			if (matrixBuild.getNumber() != combination.getNumber())
			{
				continue;
			}
			Result result = combination.getResult();
			if (!(Result.FAILURE.equals(result) || Result.UNSTABLE.equals(result)))
			{
				continue;
			}
			ClaimWrapper claimWrapper = ClaimWrapper.builder(combination);
			if (claimWrapper != null && claimWrapper.isClaimed())
			{
				claimed.append(combination.getParent().getCombination().toString());
				claimed.append(": ");
				if (claimWrapper.getReason() != null)
				{
					claimed.append(claimWrapper.getReason()).append(" ");
				}
				claimed.append("(");
				claimed.append(claimWrapper.getClaimedByName());
				claimed.append(").<br/>");
			} else
			{
				unclaimed.append(combination.getParent().getCombination().toString());
				unclaimed.append(": ").append(NOT_CLAIMED).append("<br/>");
			}
		}

		String claims = unclaimed.toString();
		if (includeClaimed)
		{
			claims += claimed.toString();
		}
		return claims;
	}

	public boolean isClaimed()
	{
		return !NOT_CLAIMED.equals(getClaim());
	}

	public boolean isCompletelyClaimed()
	{
		String claim = getClaim();
		if (StringUtils.isEmpty(claim)) { return false; }
		if (NOT_CLAIMED.equals(getClaim())) { return false; }
		return !claim.contains(NOT_CLAIMED);
	}

	public Result getLastFinishedResult()
	{
		return RadiatorUtil.getLastFinishedResult(job);
	}

	public boolean hasChildren()
	{
		return false;
	}
}
