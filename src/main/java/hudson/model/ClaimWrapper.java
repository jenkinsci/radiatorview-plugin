package hudson.model;

import hudson.plugins.claim.ClaimBuildAction;

import java.util.List;

import org.jfree.util.Log;

/**
 * Wraps ClaimBuildAction to ensure that the Claim plugin remains optional
 * to the Radiator View.
 */
public class ClaimWrapper {

	private ClaimBuildAction claimBuildAction;

	private ClaimWrapper(ClaimBuildAction claimBuildAction) {
		this.claimBuildAction = claimBuildAction;
	}

	/**
	 * Returns ClaimWrapper containing the claim for the specified run
	 * if there is a single ClaimBuildAction for the run. Returns null
	 * otherwise.
	 *
	 * @param run
	 * @return null if no single ClaimBuildAction for the run param.
	 */
	static public ClaimWrapper builder(Run<?, ?> run) {
		ClaimBuildAction claimForRun = getClaimForRun(run);
		if (claimForRun == null) {
			return null;
		}
		return new ClaimWrapper(claimForRun);
	}

	static private ClaimBuildAction getClaimForRun(Run<?, ?> run) {
		ClaimBuildAction claimAction = null;
		List<ClaimBuildAction> claimActionList = run
				.getActions(ClaimBuildAction.class);
		if (claimActionList.size() == 1) {
			claimAction = claimActionList.get(0);
		} else if (claimActionList.size() > 1) {
			Log.warn("Multiple ClaimBuildActions found for job ");
		}
		return claimAction;
	}

	public boolean isClaimed() {
		return claimBuildAction.isClaimed();
	}

	public String getReason() {
		return claimBuildAction.getReason();
	}

	public String getClaimedByName() {
		return claimBuildAction.getClaimedByName();
	}
}
