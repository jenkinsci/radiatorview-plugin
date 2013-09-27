/*
 * Copyright (c) 1Spatial Group Ltd.
 */
package hudson.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * Details of a project to be shown on the radiator.
 */
public class ProjectViewEntry implements IViewEntry {
	private TreeSet<IViewEntry> jobs = new TreeSet<IViewEntry>(
			new EntryComparator());

	private String name;

	public ProjectViewEntry(String name) {
		this.name = name;
	}

	public ProjectViewEntry() {

	}

	public TreeSet<IViewEntry> getClaimedBuilds() {
		TreeSet<IViewEntry> failing = new TreeSet<IViewEntry>(
				new EntryComparator());
		for (IViewEntry job : jobs) {
			if (job.getBroken() || job.getFailCount() > 0)
				if (job.isClaimed())
					failing.add(job);
		}
		return failing;
	}

	public TreeSet<IViewEntry> getPassingJobs() {
		TreeSet<IViewEntry> passing = new TreeSet<IViewEntry>(
				new EntryComparator());
		for (IViewEntry job : jobs) {
			if (!job.getBroken() && job.getFailCount() == 0)
				passing.add(job);
		}
		return passing;
	}

	public TreeSet<IViewEntry> getFailingJobs() {
		TreeSet<IViewEntry> failing = new TreeSet<IViewEntry>(
				new EntryComparator());
		for (IViewEntry job : jobs) {
			if (job.getBroken() || job.getFailCount() > 0)
					failing.add(job);
		}
		return failing;
	}

	public TreeSet<IViewEntry> getUnclaimedJobs() {
		TreeSet<IViewEntry> unclaimed = new TreeSet<IViewEntry>(
				new EntryComparator());
		for (IViewEntry job : jobs) {
			if ((!job.getStable()) || job.getFailCount() > 0) {
				if (!job.isClaimed())
					unclaimed.add(job);
			}
		}
		return unclaimed;
	}

	public TreeSet<IViewEntry> getJobs() {
		return jobs;
	}

	public String getName() {
		return name;
	}

	public void addBuild(IViewEntry entry) {
		Validate.notNull(entry);
		jobs.add(entry);
	}

	public String getStatus() {
		if (getStable()) {
			return "successful";
		}
		if (getUnclaimedJobs().size() == 0) {
			return "claimed";
		}
		if (getBroken()) {
			return "failing";
		}
		return "unstable";
	}

	public String getBackgroundColor() {
		if (getBroken() || getFailCount() > 0)
			if (getUnclaimedJobs().size() == 0)
				return "orange";
			else
				return "red";
		else
			return "green";
	}

	public Boolean getBroken() {
		boolean broken = false;
		for (IViewEntry job : jobs) {
			broken |= job.getBroken();
		}
		return broken;
	}
	public boolean isClaimed() {
		boolean claimed = false;
		for (IViewEntry job : jobs) {
			claimed|= job.isClaimed();
		}
		return claimed;
	}

	public Boolean getBuilding() {
		boolean building = false;
		for (IViewEntry job : jobs) {
			building |= job.getBuilding();
		}
		return building;
	}

	public String getClaim() {
		String claim = "";
		for (IViewEntry job : jobs) {
			if (job.isClaimed())
				claim += (job.getName() + ": " + job.getClaim() + ";");
		}
		return claim;
	}

	public String getColor() {
		return "white";
	}

	public Collection<String> getCulprits() {
		Set<String> culprits = new HashSet<String>();
		for (IViewEntry job : getFailingJobs()) {
			culprits.addAll(job.getCulprits());
		}
		return culprits;
	}

	public String getCulprit() {
		Collection<String> culprits = getCulprits();
		String culprit = null;
		if (!culprits.isEmpty()) {
			culprit = StringUtils.join(culprits, ", ");
		}
		return culprit;
	}

	public String getDiff() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDiffColor() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getFailCount() {
		int count = 0;
		for (IViewEntry job : jobs) {
			count += job.getFailCount();
		}
		return count;
	}

	public String getLastCompletedBuild() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getLastStableBuild() {
		// TODO Auto-generated method stub
		return null;
	}

	public Boolean getQueued() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean getStable() {
		boolean stable = true;
		for (IViewEntry job : jobs) {
			stable &= job.getStable();
		}
		return stable;
	}

	public int getSuccessCount() {
		int count = 0;
		for (IViewEntry job : jobs) {
			count += job.getSuccessCount();
		}
		return count;
	}

	public String getSuccessPercentage() {
		return "" + 100 * getSuccessCount() / getTestCount();
	}

	public int getTestCount() {
		int count = 0;
		for (IViewEntry job : jobs) {
			count += job.getTestCount();
		}
		return count;
	}

	public String getUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	public Result getLastFinishedResult() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasChildren() {
		return !jobs.isEmpty();
	}

	public String getTitle() {
		Collection<String> jobNames = new ArrayList<String>(jobs.size());
		for (IViewEntry job : jobs) {
			jobNames.add(job.getName());
		}
		return getName() + ": " + StringUtils.join(jobNames, ", ");
	}
}
