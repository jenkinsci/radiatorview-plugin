package hudson.model;

/**
 * Utilities used by the radiator view. 
 */
public class RadiatorUtil {

	public static Result getLastFinishedResult(Job job) {
		Run lastBuild = job.getLastBuild();
		while (lastBuild != null
				&& (lastBuild.hasntStartedYet() || lastBuild.isBuilding() || lastBuild
						.isLogUpdated())) {
			lastBuild = lastBuild.getPreviousBuild();
		}
		if (lastBuild != null) {
			return lastBuild.getResult();
		} else {
			return Result.NOT_BUILT;
		}
	}
}
