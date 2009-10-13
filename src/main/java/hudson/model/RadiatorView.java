package hudson.model;

import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class RadiatorView extends ListView
{

    @DataBoundConstructor
    public RadiatorView(String name)
    {
        super(name);
    }

    /**
     * Work out the sizes for the ui
     * 
     * @param jobs
     * @return
     */
    public static int getSize(Collection<Job> jobs)
    {
        int count = 0;
        for (Job job : jobs)
        {
            Result result = getResult(job);
            if (result == Result.SUCCESS)
            {
                count++;
            }
        }
        // all others are 6* the size
        count += (jobs.size() - count) * 6;

        double perRow = Math.ceil(Math.sqrt(count));

        int size = (int) perRow;
        return size;
    }

    public static Collection<Job> sortJobs(Collection<Job> jobs)
    {
        TreeSet<Job> set = new TreeSet<Job>(new JobComparator());
        if (jobs != null)
        {
            set.addAll(jobs);
        }
        return set;
    }

    public static boolean isBuilding(Job job)
    {
        Run lastBuild = job.getLastBuild();
        return lastBuild != null && (lastBuild.isLogUpdated() || lastBuild.isBuilding());
    }

    private static final class JobComparator implements Comparator<Job>
    {
        public int compare(Job o1, Job o2)
        {
            // first compare by status
            Result r1 = getResult(o1);
            Result r2 = getResult(o2);
            if (r1 != null && r2 != null)
            {
                if (r1.isBetterThan(r2))
                {
                    return 1;
                }
                else if (r1.isWorseThan(r2))
                {
                    return -1;
                }
            }

            HealthReport h1 = o1.getBuildHealth();
            HealthReport h2 = o2.getBuildHealth();
            if (h1 != null && h2 != null)
            {
                // second compare by stability
                int health = h1.compareTo(h2);
                if (health != 0)
                {
                    return health;
                }
            }

            // finally compare by name
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static Result getResult(Job job)
    {
        Run lastBuild = job.getLastBuild();
        while (lastBuild != null
                && (lastBuild.hasntStartedYet() || lastBuild.isBuilding() || lastBuild
                        .isLogUpdated()))
        {
            lastBuild = lastBuild.getPreviousBuild();
        }
        if (lastBuild != null)
        {
            return lastBuild.getResult();
        }
        else
        {
            return Result.NOT_BUILT;
        }
    }

    @Extension
    public static final class DescriptorImpl extends ViewDescriptor
    {
        public DescriptorImpl()
        {
            super();
        }

        @Override
        public String getDisplayName()
        {
            return "Radiator";
        }

        /**
         * Checks if the include regular expression is valid.
         */
        public FormValidation doCheckIncludeRegex(@QueryParameter String value) {
            String v = Util.fixEmpty(value);
            if (v != null)
            {
                try
                {
                    Pattern.compile(v);
                }
                catch (PatternSyntaxException pse)
                {
                    return FormValidation.error(pse.getMessage());
                }
            }
            return FormValidation.ok();
        }
    }
}
