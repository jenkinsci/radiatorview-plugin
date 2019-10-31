/*
 * Copyright (c) 1Spatial Group Ltd.
 */
package hudson.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * Details of a project to be shown on the radiator.
 */
public class ProjectViewEntry implements IViewEntry
{
	private TreeSet<IViewEntry> jobs = new TreeSet<IViewEntry>(new EntryComparator());

	private String name;

	private TreeSet<IViewEntry> failing;

	private TreeSet<IViewEntry> completelyPassing;

	private TreeSet<IViewEntry> unstable;

	private int jobsHashCode = -1;

	public ProjectViewEntry(String name)
	{
		this.name = name;
	}

	public ProjectViewEntry()
	{

	}

	public TreeSet<IViewEntry> getClaimedBuilds()
	{
		TreeSet<IViewEntry> failing = new TreeSet<IViewEntry>(new EntryComparator());
		for (IViewEntry job : jobs)
		{
			if (job.getBroken() || job.getFailCount() > 0) if (job.isCompletelyClaimed()) failing.add(job);
		}
		return failing;
	}

	/**
	 * Returns passing jobs. Passing depends on the context: if there is at least
	 * one failing job, then passing jobs also includes unstable ones. If there's no
	 * failing job, then passing will only include stable jobs. Unstable ones will
	 * be returned through {@link #getFailingJobs()}
	 * 
	 * @return passing jobs (including unstable if there are jobs in failure).
	 * @see #getFailingJobs()
	 */
	public TreeSet<IViewEntry> getPassingJobs()
	{

		computePassingAndFailingJobs();

		if (!failing.isEmpty())
		{
			TreeSet<IViewEntry> aggregate = new TreeSet<IViewEntry>(new EntryComparator());
			aggregate.addAll(unstable);
			aggregate.addAll(completelyPassing);
			return aggregate;
		}
		return completelyPassing;
	}

	/**
	 * Returns "failing" jobs. Depending on the context: failing will be jobs in
	 * "real" failure (i.e. "red") if there are indeed some jobs of this sort. If
	 * there aren't, then failing jobs will be unstable ones.
	 * 
	 * @return "failing" jobs (unstable ones if no failing job is currently
	 *         present).
	 * @see #getPassingJobs()
	 */
	public TreeSet<IViewEntry> getFailingJobs()
	{
		computePassingAndFailingJobs();

		if (!failing.isEmpty()) { return failing; }
		return unstable;
	}

	/**
	 * Computes "completelyPassing" (meaning without including unstable jobs),
	 * unstable and failing jobs sets. If the calculation has already been made,
	 * this method does nothing.
	 */
	private void computePassingAndFailingJobs()
	{
		if (jobsHashCode == jobs.hashCode()) { return; }
		completelyPassing = new TreeSet<IViewEntry>(new EntryComparator());
		failing = new TreeSet<IViewEntry>(new EntryComparator());
		unstable = new TreeSet<IViewEntry>(new EntryComparator());
		for (IViewEntry job : jobs)
		{
			if (job.getBroken() || job.getFailCount() > 0)
			{
				failing.add(job);
			} else if (!job.getStable())
			{
				unstable.add(job);
			} else
			{
				completelyPassing.add(job);
			}
		}
	}

	public TreeSet<IViewEntry> getUnclaimedJobs()
	{
		TreeSet<IViewEntry> unclaimed = new TreeSet<IViewEntry>(new EntryComparator());
		for (IViewEntry job : jobs)
		{
			if (((!job.getStable()) || job.getFailCount() > 0) && !job.isNotBuilt())
			{
				if (!job.isCompletelyClaimed()) unclaimed.add(job);
			}
		}
		return unclaimed;
	}

	public TreeSet<IViewEntry> getUnbuiltJobs()
	{
		TreeSet<IViewEntry> unbuilt = new TreeSet<IViewEntry>(new EntryComparator());
		for (IViewEntry job : jobs)
		{
			if (job.isNotBuilt())
			{
				unbuilt.add(job);
			}
		}
		return unbuilt;
	}

	public TreeSet<IViewEntry> getJobs()
	{
		return jobs;
	}

	public String getName()
	{
		return name;
	}

	public void addBuild(IViewEntry entry)
	{
		Validate.notNull(entry);
		jobs.add(entry);
	}

	public String getStatus()
	{
		if (getStable()) { return "successful"; }
		if (getUnclaimedJobs().isEmpty()) { return "claimed"; }
		if (getBroken()) { return "failing"; }
		return "unstable";
	}

	public String getBackgroundColor()
	{
		if (getBroken() || getFailCount() > 0)
		{
			if (getUnclaimedJobs().isEmpty())
			{
				return "orange";
			} else
			{
				return "red";
			}
		} else
		{
			return "green";
		}
	}

	public Boolean getBroken()
	{
		boolean broken = false;
		for (IViewEntry job : jobs)
		{
			broken |= job.getBroken();
		}
		return broken;
	}

	public boolean isClaimed()
	{
		boolean claimed = false;
		for (IViewEntry job : jobs)
		{
			claimed |= job.isClaimed();
		}
		return claimed;
	}

	public Boolean getBuilding()
	{
		boolean building = false;
		for (IViewEntry job : jobs)
		{
			building |= job.getBuilding();
		}
		return building;
	}

	public String getClaim()
	{
		StringBuilder claim = new StringBuilder();
		for (IViewEntry job : jobs)
		{
			if (job.isClaimed())
			{
				claim.append(job.getName()).append(": ").append(job.getClaim()).append(";");
			}
		}
		return claim.toString();
	}

	public String getColor()
	{
		return "white";
	}

	public Collection<String> getCulprits()
	{
		Set<String> culprits = new HashSet<String>();
		for (IViewEntry job : getFailingJobs())
		{
			culprits.addAll(job.getCulprits());
		}
		return culprits;
	}

	public String getCulprit()
	{
		Collection<String> culprits = getCulprits();
		String culprit = null;
		if (!culprits.isEmpty())
		{
			culprit = StringUtils.join(culprits, ", ");
		}
		return culprit;
	}

	public String getDiff()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getDiffColor()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public int getFailCount()
	{
		int count = 0;
		for (IViewEntry job : jobs)
		{
			count += job.getFailCount();
		}
		return count;
	}

	@Override
	public int getSkipCount()
	{
		int count = 0;
		for (IViewEntry job : jobs)
		{
			count += job.getSkipCount();
		}
		return count;
	}

	public String getLastCompletedBuild()
	{
		throw new UnsupportedOperationException();
	}

	public String getLastStableBuild()
	{
		throw new UnsupportedOperationException();
	}

	public Boolean getQueued()
	{
		throw new UnsupportedOperationException();
	}

	public boolean getStable()
	{
		boolean stable = true;
		for (IViewEntry job : jobs)
		{
			stable &= job.getStable();
		}
		return stable;
	}

	public int getSuccessCount()
	{
		int count = 0;
		for (IViewEntry job : jobs)
		{
			count += job.getSuccessCount();
		}
		return count;
	}

	public String getSuccessPercentage()
	{
		int testCount = getTestCount();
		if (testCount > 0) return Integer.toString(100 * getSuccessCount() / testCount)+"%";
		else return "0%";
	}

	public int getTestCount()
	{
		int count = 0;
		for (IViewEntry job : jobs)
		{
			count += job.getTestCount();
		}
		return count;
	}

	public String getUrl()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Result getLastFinishedResult()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasChildren()
	{
		return !jobs.isEmpty();
	}

	public String getTitle()
	{
		Collection<String> jobNames = new ArrayList<String>(jobs.size());
		for (IViewEntry job : jobs)
		{
			jobNames.add(job.getName());
		}
		return getName() + ": " + StringUtils.join(jobNames, ", ");
	}

	public boolean isCompletelyClaimed()
	{
		throw new UnsupportedOperationException();
	}

	public String getUnclaimedMatrixBuilds()
	{
		throw new UnsupportedOperationException();
	}

	public boolean isNotBuilt()
	{
		throw new UnsupportedOperationException();
	}
}
