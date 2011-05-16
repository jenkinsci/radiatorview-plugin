package hudson.model;

import java.util.Collection;

/**
 * Interface for an entry on the radiator, which may be either a job or a group of jobs. 
 */
public interface IViewEntry {

	/**
	 * @return the job's name
	 */
	public abstract String getName();

	/**
	 * @return if this job is queued for build
	 */
	public abstract Boolean getQueued();

	/**
	 * @return background color for this job
	 */
	public abstract String getBackgroundColor();

	/**
	 * @return foreground color for this job
	 */
	public abstract String getColor();

	/**
	 * @return true se o último build está quebrado
	 */
	public abstract Boolean getBroken();

	/**
	 * @return true if this job is currently being built
	 */
	public abstract Boolean getBuilding();

	/**
	 * @return the URL for the last build
	 */
	public abstract String getUrl();

	/**
	 * @return total tests executed
	 */
	public abstract int getTestCount();

	/**
	 * @return total failed tests
	 */
	public abstract int getFailCount();

	/**
	 * @return total successful tests
	 */
	public abstract int getSuccessCount();

	/**
	 * @return difference between this job's last build successful tests and the
	 *         previous'
	 */
	public abstract String getDiff();

	/**
	 * Elects a culprit/responsible for a broken build by choosing the last
	 * commiter of a given build
	 * 
	 * @return the culprit/responsible
	 */
	public abstract String getCulprit();
	
	
    public Collection<String> getCulprits();

	/**
	 * @return color to be used to show the test diff
	 */
	public abstract String getDiffColor();

	/**
	 * @return the percentage of successful tests versus the total
	 */
	public abstract String getSuccessPercentage();

	public abstract String getLastCompletedBuild();

	public abstract String getLastStableBuild();

	/**
	 * @return <code>true</code> if the build is stable.
	 */
	public abstract boolean getStable();

	/**
	 * If the claims plugin is installed, this will get details of the claimed
	 * build failures.
	 * 
	 * @return details of any claims for the broken build, or null if nobody has
	 *         claimed this build.
	 */
	public abstract String getClaim();

	public abstract Result getLastFinishedResult();
	
	public abstract boolean hasChildren();

	public abstract boolean isClaimed();

}